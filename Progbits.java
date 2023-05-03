import java.nio.ByteBuffer;
import java.sql.SQLOutput;
import java.util.*;

public class Progbits extends Table {

    private int counter = 0;

    private Map<Integer, String> labels = new HashMap<>();

    private final static String[] registerNames = new String[] {
            "zero", "ra", "sp", "gp", "tp", "t0", "t1", "t2",
            "s0", "s1", "a0", "a1", "a2", "a3", "a4", "a5",
            "a6", "a7", "s2", "s3", "s4", "s5", "s6", "s7",
            "s8", "s9", "s10", "s11", "t3", "t4", "t5", "t6"
    };

    private List<Function> functions;

    private Symtable symtab;

    public void setSymtable(Symtable symtab) {
        this.symtab = symtab;
    }

    public Progbits(int offset, int size, int entSize, ByteBuffer buffer, int strTableOffset) {
        super(offset, size, entSize, buffer, strTableOffset);
    }

    void setFunctions(List<Function> functions) {
        this.functions = functions;
    }

    private String reverse(String s) {
        return new StringBuilder(s).reverse().toString();
    }

    private String inverse(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            sb.append(s.charAt(i) == '1' ? '0' : '1');
        }
        return sb.toString();
    }

    private String parseCommand(int command, int addr) {
        addr += 0x10000;
        String bits = String.format("%32s",
                Integer.toBinaryString(command)).replaceAll(" ", "0");
        bits = new StringBuilder(bits).reverse().toString();
        String type = reverse(bits.substring(0, 7));
        switch (type) {
            case("0110111") -> {
                String rd = reverse(bits.substring(7, 12));
                String register = registerNames[Integer.parseInt(rd, 2)];
                String imm = reverse(bits.substring(12));
                String immediate = String.format("0x%s", Integer.toHexString(Integer.parseInt(imm, 2)));
                return String.format("   %05x:\t%08x\t%7s\t%s, %s\n",
                        addr,
                        command,
                        "lui", register, immediate);
            }
            case("0010111") -> {
                String rd = reverse(bits.substring(7, 12));
                String register = registerNames[Integer.parseInt(rd, 2)];
                String imm = reverse(bits.substring(12));
                String immediate = String.format("0x%s", Integer.toHexString(Integer.parseInt(imm, 2)));
                return String.format("   %05x:\t%08x\t%7s\t%s, %s\n",
                        addr,
                        command,
                        "auipc", register, immediate);
            }
            case("0010011") -> {
                String rd = reverse(bits.substring(7, 12));
                String register = registerNames[Integer.parseInt(rd, 2)];
                String key = reverse(bits.substring(12, 15));
                String imm = reverse(bits.substring(20));
                String immediate = String.format("0x%s", Integer.toHexString(Integer.parseInt(imm, 2)));
                String rs1 = reverse(bits.substring(15, 20));
                String register1 = registerNames[Integer.parseInt(rs1, 2)];
                String shamt = "";
                String name = "bruh";
                switch(key) {
                    case("000") -> name = "addi";
                    case("010") -> name = "slti";
                    case("011") -> name = "sltiu";
                    case("100") -> name = "xori";
                    case("110") -> name = "ori";
                    case("111") -> name = "andi";
                    case("001") -> {
                        name = "slli";
                        shamt = reverse(bits.substring(20, 25));
                    }
                    case("101") -> {
                        String s = reverse(bits.substring(27));
                        if (s.equals("00000")) {
                            name = "srli";
                        }
                        else {
                            name = "srai";
                        }
                        shamt = reverse(bits.substring(20, 25));
                    }
                }
                int i = Integer.parseInt(imm, 2);
                immediate = Integer.toString(i - 2 * (i & (1 << 11)));
                return String.format("   %05x:\t%08x\t%7s\t%s, %s, %s\n",
                        addr,
                        command,
                        name, register, register1,
                        shamt.length() == 0 ? immediate : shamt);
            }
            case ("0110011") -> {
                String rd = reverse(bits.substring(7, 12));
                String register = registerNames[Integer.parseInt(rd, 2)];
                String key = reverse(bits.substring(12, 15));
                String rs1 = reverse(bits.substring(15, 20));
                String register1 = registerNames[Integer.parseInt(rs1, 2)];
                String rs2 = reverse(bits.substring(20, 25));
                String register2 = registerNames[Integer.parseInt(rs2, 2)];
                String two = reverse(bits.substring(25, 27));
                String tail = reverse(bits.substring(27));
                String name = "bruh";
                switch (key) {
                    case ("000") -> {
                        if (two.equals("01")) {
                            name = "mul";
                        }
                        else {
                            if (tail.equals("00000")) {
                                name = "add";
                            } else {
                                name = "sub";
                            }
                        }
                    }
                    case ("001") -> name = two.equals("01") ? "mulh" : "sll";
                    case ("010") -> name = two.equals("01") ? "mulhsu" : "slt";
                    case ("011") -> name = two.equals("01") ? "mulhu" : "sltu";
                    case ("100") -> name = two.equals("01") ? "div" : "xor";
                    case ("101") -> {
                        if (two.equals("01")) {
                            name = "divu";
                        }
                        else {
                            if (tail.equals("00000")) {
                                name = "srl";
                            } else {
                                name = "sra";
                            }
                        }
                    }
                    case ("110") -> name = two.equals("01") ? "rem" : "or";
                    case ("111") -> name = two.equals("01") ? "remu" : "and";
                }
                return String.format("   %05x:\t%08x\t%7s\t%s, %s, %s\n",
                        addr,
                        command,
                        name, register, register1, register2);
            }
            case("0001111") -> {
                String succ = reverse(bits.substring(20, 24));
                String pred = reverse(bits.substring(24, 28));
                return String.format("   %05x:\t%08x\t%7s\t%s, %s\n", addr, command, "fence", pred, succ);
            }
            case("1110011") -> {
                String tail = reverse(bits.substring(7));
                String name = "bruh";
                switch(tail) {
                    case("0000000000000000000000000") -> name = "ecall";
                    case("0000000000010000000000000") -> name = "ebreak";
                    case("0000000000100000000000000") -> name = "uret";
                    case("0001000000100000000000000") -> name = "sret"; // :))))))))))))))))))))))))
                    case("0011000000100000000000000") -> name = "mret";
                    case("0001000001010000000000000") -> name = "wfi";
                }
                return String.format("   %05x:\t%08x\t%7s\n", addr, command, name);
            }
            case("0000011") -> {
                String rd = reverse(bits.substring(7, 12));
                String register = registerNames[Integer.parseInt(rd, 2)];
                String key = reverse(bits.substring(12, 15));
                String offset = Integer.toHexString(
                        Integer.parseInt(reverse(bits.substring(20)), 2)
                );
                String rs1 = reverse(bits.substring(15, 20));
                String register1 = registerNames[Integer.parseInt(rs1, 2)];
                String name = "bruh";
                switch (key) {
                    case("000") -> name = "lb";
                    case("001") -> name = "lh";
                    case("010") -> name = "lw";
                    case("100") -> name = "lbu";
                    case("101") -> name = "lhu";
                }
                return String.format("   %05x:\t%08x\t%7s\t%s, %s(%s)\n",
                        addr, command, name, register, offset, register1);
            }
            case("0100011") -> {
                String offset = Integer.toHexString(
                        Integer.parseInt(reverse(bits.substring(7, 12)), 2)
                );
                String key = reverse(bits.substring(12, 15));
                String rs1 = reverse(bits.substring(15, 20));
                String rs2 = reverse(bits.substring(20, 25));
                String register1 = registerNames[Integer.parseInt(rs1, 2)];
                String register2 = registerNames[Integer.parseInt(rs2, 2)];
                String name = "bruh";
                switch (key) {
                    case("001") -> name = "sh";
                    case("010") -> name = "sw";
                }
                return String.format("   %05x:\t%08x\t%7s\t%s, %s(%s)\n",
                        addr, command, name, register2, offset, register1);
            }
            case("1101111") -> {
                String rd = reverse(bits.substring(7, 12));
                String register = registerNames[Integer.parseInt(rd)];
                int address = ((command >> 31) << 20) +
                        (((command >>> 21) & ((1 << 10) - 1)) << 1) + (((command >>> 20) & 1) << 11)
                        + (((command >>> 12) & ((1 << 8) - 1)) << 12) + addr;
                String destination = "bruh";
                for (Symbol s : symtab.symbols) {
                    if (s.st_value == address) {
                        destination = s.name;
                    }
                }
                if (destination.equals("bruh")) {
                    if (labels.containsKey(address)) {
                        destination = labels.get(address);
                    }
                    else {
                        destination = String.format("L%d", counter);
                        labels.put(address, String.format("L%d", counter));
                        functions.add(new Function(String.format("L%d", counter), Integer.toHexString(address), 0));
                        counter++;
                    }
                }
                return String.format("   %05x:\t%08x\t%7s\t%s, %s <%s>\n",
                        addr, command, "jal", register, Integer.toHexString(address), destination
                );
            }
            case("1100111") -> {
                String rd = reverse(bits.substring(7, 12));
                String register = registerNames[Integer.parseInt(rd)];
                String rd1 = reverse(bits.substring(15, 20));
                String register1 = registerNames[Integer.parseInt(rd1)];
                String offset = reverse(bits.substring(20));
                int i = Integer.parseInt(offset, 2);
                String off = Integer.toHexString(i - 2 * (i & (i << 12)));
                return String.format("   %05x:\t%08x\t%7s\t%s, %s(%s)\n",
                        addr, command, "jalr", register, off, register1
                );
            }
            case("1100011") -> {
                String key = reverse(bits.substring(12, 15));
                String rs1 = reverse(bits.substring(15, 20));
                String rs2 = reverse(bits.substring(20, 25));
                String register1 = registerNames[Integer.parseInt(rs1, 2)];
                String register2 = registerNames[Integer.parseInt(rs2, 2)];
                short off = (short) (((command >> 31) << 12)
                        + (((command >> 25) & ((1 << 6) - 1)) << 5)
                        + (((command >> 8) & ((1 << 4) - 1)) << 1)
                        + (((command >> 7) & 1) << 11));
                int address = off + addr;
                String destination = "bruh";
                for (Symbol s : symtab.symbols) {
                    if (s.st_value == address) {
                        destination = s.name;
                    }
                }
                if (destination.equals("bruh")) {
                    if (labels.containsKey(address)) {
                        destination = labels.get(address);
                    }
                    else {
                        destination = String.format("L%d", counter);
                        labels.put(address, String.format("L%d", counter));
                        functions.add(new Function(String.format("L%d", counter), Integer.toHexString(address), 0));
                        counter++;
                    }
                }
                String name = "bruh";
                switch (key) {
                    case ("000") -> name = "beq";
                    case ("001") -> name = "bne";
                    case ("100") -> name = "blt";
                    case ("101") -> name = "bge";
                    case ("110") -> name = "bltu";
                    case ("111") -> name = "bgeu";
                }
                return String.format("   %05x:\t%08x\t%7s\t%s, %s, %s <%s>\n",
                        addr, command, name, register1, register2, Integer.toHexString(address), destination);
            }
            default -> { return "Unknown instruction\n"; }
        }
    }

    private List<String> parse() {
        List<String> commands = new ArrayList<>();
        int num = size / 4;
        int index = offset;
        for (int i = 0; i < num; i++) {
            int command = buffer.getInt(index);
            commands.add(parseCommand(command, index));
            index += 4;
        }
        return commands;
    }

    private List<String> divideToFunctions(List<String> commands) {
        List<String> result = new ArrayList<>();
        int currentFunction = 0;
        this.functions.sort((o1, o2) -> {
            if (Integer.parseInt(o1.value, 16) == Integer.parseInt(o2.value, 16)) {
                return 0;
            }
            return Integer.parseInt(o1.value, 16) < Integer.parseInt(o2.value, 16) ? -1 : 1;
        });
        int counter = 0;
        int num = functions.size() == 1 ? commands.size()
                : (Integer.parseInt(functions.get(1).value, 16)
                - Integer.parseInt(functions.get(0).value, 16)) / 4;
        System.out.println(num);
        result.add(functions.get(0).toString());
        for (String command : commands) {
            result.add(command);
            counter++;
            if (counter == num) {
                result.add(functions.get(currentFunction + 1).toString());
                currentFunction += 1;
                num = functions.size() - 1 == currentFunction ? commands.size()
                        : (Integer.parseInt(functions.get(currentFunction + 1).value, 16)
                        - Integer.parseInt(functions.get(currentFunction).value, 16)) / 4;
                counter = 0;
            }
        }
        return result;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(".text\n");
        for (String command : divideToFunctions(parse())) {
            sb.append(command);
        }
        return sb.toString();
    }
}
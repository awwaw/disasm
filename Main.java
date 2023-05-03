import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


public class Main {

    private static ByteBuffer buffer;

    private static byte[] readFile(Path path) throws NoSuchFileException, IOException {
        return Files.readAllBytes(path);
    }

    private static String toHex(byte b) {
        int hex = b & 0xFF;
        StringBuilder sb = new StringBuilder();
        String hexString = Integer.toHexString(hex);
        if (hexString.length() < 2) {
            sb.append('0');
        }
        sb.append(hexString);
        return sb.toString();
    }

    private static String[] toHexArray(byte[] array) {
        String[] res = new String[array.length];
        for (int i = 0; i < array.length; i++) {
            res[i] = toHex(array[i]);
        }
        return res;
    }

    private static String getSectionHeadersOffset() {
        return Integer.toHexString(buffer.getInt(32));
    }

    private static String getE_shnum() {
        return Integer.toHexString(buffer.getShort(48));
    }

    private static int getIndexOfSectionNames() {
        int tableIndex = buffer.getShort(50);
        int e_shoff = Integer.parseInt(getSectionHeadersOffset(), 16);
        int absoluteIndex = e_shoff + (tableIndex) * buffer.getShort(46) + 16;
        int offset = buffer.getInt(absoluteIndex);
        return offset;
    }

    private static String getTableName(int nameOffset) {
        int index = getIndexOfSectionNames() + nameOffset;
        StringBuilder name = new StringBuilder();
        byte chr = buffer.get(index);
        while (chr != 0) {
            name.append((char) chr);
            chr = buffer.get(++index);
        }
        return name.toString();
    }

    private static Table getProgbits() {
        return getTable(".text");
    }

    private static Table getSymtable() {
        return getTable(".symtab");
    }

    private static Table getTable(String tableName) {
        int e_shnum = Integer.parseInt(getE_shnum(), 16);
        int e_shoff = Integer.parseInt(getSectionHeadersOffset(), 16);
        int offset = e_shoff;
        final int sectionSize = buffer.getShort(46); // section size in bytes;
        int strTableOffset = -1;
        int symtableOffset = 0;
        int symtableSize = 0;
        int symtableEntSize = 0;
        for (int i = 0; i < e_shnum; i++) {
            int name = buffer.getInt(offset);
            offset += 4;
            int type = buffer.getInt(offset);
            offset += 4;
            int flags = buffer.getInt(offset);
            offset += 4;
            int addr = buffer.getInt(offset);
            offset += 4;
            int sectionOffset = buffer.getInt(offset);
            offset += 4;
            int size = buffer.getInt(offset);
            offset += 4;
            int link = buffer.getInt(offset);
            offset += 4;
            int a = buffer.getInt(offset);
            offset += 4;
            int b = buffer.getInt(offset);
            offset += 4;
            int entSize = buffer.getInt(offset);
            offset += 4;
            if (getTableName(name).equals(tableName)) {
                symtableOffset = sectionOffset;
                symtableSize = size;
                symtableEntSize = entSize;
            }
            if (getTableName(name).equals(".strtab")) {
                strTableOffset = sectionOffset;
            }
        }
        if (tableName.equals(".text")) {
            return new Progbits(symtableOffset, symtableSize, symtableEntSize, buffer, strTableOffset);
        }
        return new Symtable(symtableOffset, symtableSize, symtableEntSize, buffer, strTableOffset);
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            throw new IllegalArgumentException(
                    String.format("Not enough arguments. Expected 2, found %d", args.length)
            );
        }

        String inputFilename = args[0];
        String outputFilename = args[1];
        Path path = Paths.get(inputFilename);
        byte[] data = readFile(path);
        buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        Symtable symtab = (Symtable) getSymtable();

        if (!Integer.toHexString(buffer.getInt()).equals("464c457f")) {
            throw new IllegalArgumentException("Incorrect file format: this is not an .elf file!");
        }

        List<Function> functionList = new ArrayList<>();
        for (Symbol s : symtab.symbols) {
            if (s.type.equals("FUNC")) {
                functionList.add(new Function(s.name, s.value, s.size == null ? 0 : Integer.parseInt(s.size, 16)));
            }
        }
        Progbits text = (Progbits) getProgbits();
        text.setSymtable(symtab);
        text.setFunctions(functionList);

        try {
            try (PrintWriter writer = new PrintWriter(
                    new BufferedWriter(new OutputStreamWriter(
                            new FileOutputStream(outputFilename),
                            StandardCharsets.UTF_8
                    )))) {
                writer.println(text);
                writer.println(symtab);
            }
        } catch(IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }
}
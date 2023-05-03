import java.nio.ByteBuffer;

public class Symtable extends Table {
    private final int symNumber;
    public Symbol[] symbols;

    Symtable(int offset, int size, int entSize, ByteBuffer buffer, int strTableOffset) {
        super(offset, size, entSize, buffer, strTableOffset);
        symNumber = this.size / this.entSize;
        symbols = new Symbol[this.symNumber];
        fill();
    }


    private void fill() {
        int offset = this.offset;
        for (int i = 0; i < symNumber; i++) {
            int st_name = buffer.getInt(offset);
            offset += 4;
            int st_value = buffer.getInt(offset);
            offset += 4;
            int st_size = buffer.getInt(offset);
            offset += 4;
            byte st_info = buffer.get(offset);
            offset += 1;
            byte st_other = buffer.get(offset);
            offset += 1;
            short st_shndx = buffer.getShort(offset);
            offset += 2;
            Symbol current = new Symbol(st_name, st_value, st_size, st_info, st_other, st_shndx);
            int bind = ((st_info) >> 4);
            current.setBind(
                    switch (bind) {
                        case(0) -> "LOCAL";
                        case(1) -> "GLOBAL";
                        case(2) -> "WEAK";
                        case(13) -> "LOPROC";
                        case(15) -> "HIPROC";
                        default -> throw new IllegalStateException("Unexpected value: " + bind);
                    }
            );

            int type = ((st_info) & 0xf);
            current.setType(
                    switch(type) {
                        case(0) -> "NOTYPE";
                        case(1) -> "OBJECT";
                        case(2) -> "FUNC";
                        case(3) -> "SECTION";
                        case(4) -> "FILE";
                        case(13) -> "LOPROC";
                        case(15) -> "HIPROC";
                        default -> throw new IllegalStateException("Unexpected value: " + type);
                    }
            );

            int visibility = ((st_other) & 0x3);
            current.setVis(
                    switch(visibility) {
                        case(0) -> "DEFAULT";
                        case(1) -> "INTERNAL";
                        case(2) -> "HIDDEN";
                        case(3) -> "PROTECTED";
                        case(4) -> "EXPORTED";
                        case(5) -> "SINGLETON";
                        case(6) -> "ELIMINATE";
                        default -> throw new IllegalStateException("Unexpected value: " + visibility);
                    }
            );

            current.setValue(Integer.toHexString(st_value));

            StringBuilder name = new StringBuilder();
            int index = strTableOffset + st_name;
            byte chr = buffer.get(index);
            while (chr != 0) {
                name.append((char) chr);
                chr = buffer.get(++index);
            }
            current.setName(name.toString());

            String ndx = Integer.toHexString(st_shndx);
            current.setNdx(
                    switch(st_shndx) {
                        case(0) -> "UND";
                        case(-256) -> "LORESERVE";
                        case(-225) -> "HIPROC";
                        case(-14) -> "COMMON";
                        case(-1) -> "HIRESERVE";
                        case(-15) -> "ABS";
                        default -> ndx;// throw new IllegalStateException("Unexpected value: " + ndx);
                    }
            );
            symbols[i] = current;
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Symbol table '.symtab' contains %d entries:\n", this.symNumber));
        sb.append("Symbol Value          	  Size Type 	Bind 	 Vis   	      Index Name\n");
        for (int i = 0; i < this.symNumber; i++) {
            Symbol current = this.symbols[i];
            sb.append(String.format("[%4d] 0x%-15X %5d %-8s %-8s %-8s %9s %s\n",
                            i,
                            current.st_value,
                            current.st_size,
                            current.type,
                            current.bind,
                            current.vis,
                            current.ndx,
                            current.name
                    )
            );
        }
        return sb.toString();
    }
}
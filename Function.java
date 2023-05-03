public class Function {
    public final String name;
    public final String value;
    public final int size;

    Function(String name, String value, int size) {
        this.name = name;
        this.value = value;
        this.size = size;
    }

    public String toString() {
        return String.format("%08x   <%s>:\n", Integer.parseInt(value, 16), name);
    }
}
import java.nio.ByteBuffer;

public class Table {
    protected final int offset;
    protected final int size;
    protected final int strTableOffset;
    protected final int entSize;

    protected final ByteBuffer buffer;

    public Table(int offset, int size, int entSize, ByteBuffer buffer, int strTableOffset) {
        this.offset = offset;
        this.size = size;
        this.strTableOffset = strTableOffset;
        this.buffer = buffer;
        this.entSize = entSize;
    }
}
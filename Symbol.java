import java.util.Comparator;

public class Symbol {
    public final int st_name;
    public final int st_value;
    public final int st_size;
    public final byte st_info;
    public final byte st_other;
    public final short st_shndx;

    public String value, size, type, bind, vis, ndx, name;


    public Symbol(int st_name, int st_value, int st_size, byte st_info, byte st_other, short st_shndx) {
        this.st_name = st_name;
        this.st_value = st_value;
        this.st_size = st_size;
        this.st_info = st_info;
        this.st_other = st_other;
        this.st_shndx = st_shndx;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setBind(String bind) {
        this.bind = bind;
    }

    public void setVis(String vis) {
        this.vis = vis;
    }

    public void setNdx(String ndx) {
        this.ndx = ndx;
    }

    public void setName(String name) {
        this.name = name;
    }
}
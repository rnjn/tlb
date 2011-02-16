package tlb;

import java.io.File;

public class DummyTlbFileResource implements TlbFileResource {
    private final String name;
    private final String dir;

    public DummyTlbFileResource(String name, String dir) {
        this.name = name;
        this.dir = dir;
    }

    public String getName() {
        return name();
    }

    public File getFile() {
        return new File(name());
    }

    private String name() {
        return dir + "/" + name + ".class";
    }

    public void setBaseDir(File file) {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DummyTlbFileResource that = (DummyTlbFileResource) o;

        if (dir != null ? !dir.equals(that.dir) : that.dir != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (dir != null ? dir.hashCode() : 0);
        return result;
    }
}

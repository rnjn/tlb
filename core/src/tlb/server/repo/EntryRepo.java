package tlb.server.repo;

import tlb.domain.Entry;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.List;

/**
 * @understands storage and retrieval of records 
 */
public interface EntryRepo<T extends Entry> {
    Collection<T> list();

    Collection<T> list(String version) throws IOException, ClassNotFoundException;

    void update(T entry);

    String diskDump() throws IOException;

    void load(final String fileContents) throws IOException;

    void add(T entry);

    void setFactory(EntryRepoFactory factory);

    void setNamespace(String namespace);

    void setIdentifier(String type);

    List<T> parse(String string);
}

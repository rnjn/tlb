package tlb.server.repo;

import tlb.domain.SuiteLevelEntry;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @understands persistence and retrieval of suite based data
 */
public abstract class SuiteEntryRepo<T extends SuiteLevelEntry> implements EntryRepo<T> {
    protected Map<String, T> suiteData;
    protected String namespace;
    transient protected EntryRepoFactory factory;
    protected String identifier;

    public SuiteEntryRepo() {
        super();
        suiteData = new ConcurrentHashMap<String, T>();
    }

    public Collection<T> list() {
        return suiteData.values();
    }

    public void update(T record) {
        suiteData.put(record.getName(), record);
    }

    public final void add(T entry) {
        throw new UnsupportedOperationException("add not allowed on repository");
    }

    public final void setFactory(EntryRepoFactory factory) {
        this.factory = factory;
    }

    public final void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public void setIdentifier(String type) {
        this.identifier = type;
    }

    public final String diskDump() throws IOException {
        StringBuilder dumpBuffer = new StringBuilder();
        for (T entry : suiteData.values()) {
            dumpBuffer.append(entry.dump());
        }
        return dumpBuffer.toString();
    }

    public void load(final String fileContents) throws IOException {
        for (T entry : parse(fileContents)) {
            suiteData.put(entry.getName(), entry);
        }
    }
}

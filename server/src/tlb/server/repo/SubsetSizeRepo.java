package tlb.server.repo;

import tlb.domain.SubsetSizeEntry;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @understands storage and retrival of size of subset of total suites run by job
 */
public class SubsetSizeRepo implements EntryRepo<SubsetSizeEntry> {
    private List<SubsetSizeEntry> entries;

    public SubsetSizeRepo() {
        entries = new ArrayList<SubsetSizeEntry>();
    }

    public Collection<SubsetSizeEntry> list() {
        return entries;
    }

    public Collection<SubsetSizeEntry> list(String version) throws IOException, ClassNotFoundException {
        throw new UnsupportedOperationException("versioning not allowed");
    }

    public void update(SubsetSizeEntry entry) {
        throw new UnsupportedOperationException("update not allowed on repository");
    }

    public String diskDump() throws IOException {
        StringBuilder dumpBuffer = new StringBuilder();
        for (SubsetSizeEntry entry : entries) {
            dumpBuffer.append(entry.dump());
        }
        return dumpBuffer.toString();
    }

    public void load(final String fileContents) throws IOException {
        entries = parse(fileContents);
    }

    public void add(SubsetSizeEntry entry) {
        entries.add(entry);
    }

    public void setFactory(EntryRepoFactory factory) {
        //doesn't need
    }

    public void setNamespace(String namespace) {
        //doesn't need
    }

    public void setIdentifier(String type) {
        //doesn't need
    }

    public List<SubsetSizeEntry> parse(String string) {
        return SubsetSizeEntry.parse(string);
    }
}

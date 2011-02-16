package tlb.server.repo;

import tlb.domain.SuiteResultEntry;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * @understands storage and retrival of suite results for suites
 */
public class SuiteResultRepo extends SuiteEntryRepo<SuiteResultEntry> {

    public Collection<SuiteResultEntry> list(String version) throws IOException, ClassNotFoundException {
        throw new UnsupportedOperationException("versioning not allowed");
    }

    public List<SuiteResultEntry> parse(String string) {
        return SuiteResultEntry.parse(string);
    }
}

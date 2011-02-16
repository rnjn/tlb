package tlb.server.repo;

import tlb.domain.SuiteTimeEntry;
import tlb.domain.TimeProvider;

import java.io.IOException;
import java.util.List;

/**
 * @understands storage and retrival of time that each suite took to run
 */
public class SuiteTimeRepo extends VersioningEntryRepo<SuiteTimeEntry> {

    public SuiteTimeRepo(TimeProvider timeProvider) {
        super(timeProvider);
    }

    @Override
    public SuiteTimeRepo getSubRepo(String versionIdentifier) throws IOException {
        return factory.createSuiteTimeRepo(namespace, versionIdentifier);
    }

    public List<SuiteTimeEntry> parse(String string) {
        return SuiteTimeEntry.parse(string);
    }
}

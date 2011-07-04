package tlb.server.repo;

import tlb.domain.SuiteLevelEntry;
import tlb.domain.TimeProvider;

import java.io.IOException;
import java.util.*;

/**
 * @understands versions of entry list
 */
public abstract class VersioningEntryRepo<T extends SuiteLevelEntry> extends SuiteEntryRepo<T> {
    private boolean loadedData = false;
    private Map<String, VersioningEntryRepo<T>> versions;
    private final TimeProvider timeProvider;
    private Date createdAt;

    public VersioningEntryRepo(TimeProvider timeProvider) {
        this.timeProvider = timeProvider;
        createdAt = timeProvider.now();
        versions = Collections.synchronizedMap(new WeakHashMap<String, VersioningEntryRepo<T>>());
    }

    public void purgeOldVersions(int versionLifeInDays) throws IOException {
        GregorianCalendar cal = timeProvider.cal();
        cal.add(GregorianCalendar.DAY_OF_WEEK, -versionLifeInDays);//this should be parametrized
        final Date yesterday = cal.getTime();
        HashMap<String, VersioningEntryRepo> versionsToBePurged = new HashMap<String, VersioningEntryRepo>();
        for (String versionKey : versions.keySet()) {
            final VersioningEntryRepo<T> version = versions.get(versionKey);
            if (version.createdAt.before(yesterday)) {
                versionsToBePurged.put(versionKey, version);
            }
        }
        for (Map.Entry<String, VersioningEntryRepo> purgableVersion : versionsToBePurged.entrySet()) {
            versions.remove(purgableVersion.getKey());
            factory.purge(purgableVersion.getValue().identifier);
        }
    }

    public abstract VersioningEntryRepo<T> getSubRepo(String versionIdentifier) throws IOException;

    public Collection<T> list(String versionIdentifier) throws IOException {
        VersioningEntryRepo<T> version;
        synchronized (versionIdentifier.intern()) {
            version = versions.get(versionIdentifier);
            if (version == null) {
                version = getSubRepo(versionIdentifier);
                if (!version.loadedData) {
                    for (T entry : list()) {
                        version.update(entry);
                    }
                }
                versions.put(versionIdentifier, version);
            }
        }
        return version.list();
    }

    @Override
    public final void load(final String fileContents) throws IOException {
        super.load(fileContents);
        loadedData = true;
    }
}

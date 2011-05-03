package tlb.server.repo;

import org.apache.log4j.Logger;
import tlb.TlbConstants;
import tlb.domain.TimeProvider;
import tlb.utils.FileUtil;
import tlb.utils.SystemEnvironment;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


import static tlb.TlbConstants.Server.DEFAULT_TLB_DATA_DIR;
import static tlb.TlbConstants.Server.EntryRepoFactory.SUBSET_SIZE;
import static tlb.TlbConstants.Server.EntryRepoFactory.SUITE_RESULT;
import static tlb.TlbConstants.Server.EntryRepoFactory.SUITE_TIME;
import static tlb.TlbConstants.Server.TLB_DATA_DIR;

/**
 * @understands creation of EntryRepo
 */
public class EntryRepoFactory implements Runnable {
    public static final String DELIMITER = "_";
    public static final String LATEST_VERSION = "LATEST";
    private static final Logger logger = Logger.getLogger(EntryRepoFactory.class.getName());

    private final Map<String, EntryRepo> repos;
    private final String tlbStoreDir;
    private final TimeProvider timeProvider;

    static interface Creator<T> {
        T create();
    }

    public EntryRepoFactory(SystemEnvironment env) {
        this(new File(env.val(TLB_DATA_DIR, DEFAULT_TLB_DATA_DIR)), new TimeProvider(), Double.parseDouble(env.val(TlbConstants.TLB_SMOOTHING_FACTOR, "1")));
    }

    EntryRepoFactory(File tlbStoreDir, TimeProvider timeProvider, double smoothingFactor) {
        this.tlbStoreDir = tlbStoreDir.getAbsolutePath();
        this.timeProvider = timeProvider;
        repos = new ConcurrentHashMap<String, EntryRepo>();
    }

    public void purge(String identifier) throws IOException {
        synchronized (repoId(identifier)) {
            repos.remove(identifier);
            File file = dumpFile(identifier);
            if (file.exists()) FileUtils.forceDelete(file);
        }
    }

    private String repoId(String identifier) {
        return identifier.intern();
    }

    public void purgeVersionsOlderThan(int versionLifeInDays) {
        for (String identifier : repos.keySet()) {
            EntryRepo entryRepo = repos.get(identifier);
            if (entryRepo instanceof VersioningEntryRepo) {
                final VersioningEntryRepo repo = (VersioningEntryRepo) entryRepo;
                try {
                    repo.purgeOldVersions(versionLifeInDays);
                } catch (Exception e) {
                    logger.warn(String.format("failed to delete older versions for repo identified by '%s'", identifier), e);
                }
            }
        }
    }

    public SuiteResultRepo createSuiteResultRepo(final String namespace, final String version) throws ClassNotFoundException, IOException {
        return (SuiteResultRepo) findOrCreate(namespace, version, SUITE_RESULT, new Creator<SuiteResultRepo>() {
            public SuiteResultRepo create() {
                return new SuiteResultRepo();
            }
        });
    }

    public SuiteTimeRepo createSuiteTimeRepo(final String namespace, final String version) throws IOException {
        return (SuiteTimeRepo) findOrCreate(namespace, version, SUITE_TIME, new Creator<SuiteTimeRepo>() {
            public SuiteTimeRepo create() {
                return new SuiteTimeRepo(timeProvider);
            }
        });
    }

    public SubsetSizeRepo createSubsetRepo(final String namespace, final String version) throws IOException, ClassNotFoundException {
        return (SubsetSizeRepo) findOrCreate(namespace, version, SUBSET_SIZE, new Creator<SubsetSizeRepo>() {
            public SubsetSizeRepo create() {
                return new SubsetSizeRepo();
            }
        });
    }

    EntryRepo findOrCreate(String namespace, String version, String type, Creator<? extends EntryRepo> creator) throws IOException {
        String identifier = name(namespace, version, type);
        synchronized (repoId(identifier)) {
            EntryRepo repo = repos.get(identifier);
            if (repo == null) {
                repo = creator.create();
                repo.setFactory(this);
                repo.setNamespace(namespace);
                repo.setIdentifier(identifier);
                repos.put(identifier, repo);

                File diskDump = dumpFile(identifier);
                if (diskDump.exists()) {
                    final FileReader reader = new FileReader(diskDump);
                    repo.load(FileUtil.readIntoString(new BufferedReader(reader)));
                }
            }
            return repo;
        }
    }

    private File dumpFile(String identifier) {
        new File(tlbStoreDir).mkdirs();
        return new File(tlbStoreDir, identifier);
    }

    public static String name(String namespace, String version, String type) {
        return escape(namespace) + DELIMITER + escape(version) + DELIMITER + escape(type);
    }

    private static String escape(String str) {
        return str.replace(DELIMITER, DELIMITER + DELIMITER);
    }

    @Deprecated
        //for tests only
    Map<String, EntryRepo> getRepos() {
        return repos;
    }

    public void run() {
        for (String identifier : repos.keySet()) {
            FileWriter writer = null;
            try {
                //don't care about a couple entries not being persisted(at teardown), as client is capable of balancing on averages(treat like new suites)
                synchronized (repoId(identifier)) {
                    EntryRepo entryRepo = repos.get(identifier);
                    if (entryRepo != null) {
                        writer = new FileWriter(dumpFile(identifier));
                        String dump = entryRepo.diskDump();
                        writer.write(dump);
                    }
                }
            } catch (IOException e) {
                logger.warn(String.format("disk dump of %s failed, tlb server may not be able to perform data dependent on next reboot.", identifier), e);
            } finally {
                try {
                    if (writer != null) {
                        writer.close();
                    }
                } catch (IOException e) {
                    logger.warn(String.format("closing of disk dump file of %s failed, tlb server may not be able to perform data dependent on next reboot.", identifier), e);
                }
            }
        }
    }

    public void registerExitHook() {
        Runtime.getRuntime().addShutdownHook(exitHook());
    }

    public Thread exitHook() {
        return new Thread(this);
    }
}

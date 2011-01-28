package tlb.splitter;

import org.apache.log4j.Logger;
import tlb.TlbSuiteFile;
import tlb.domain.SuiteTimeEntry;
import tlb.service.TalkToService;
import tlb.splitter.timebased.Bucket;
import tlb.splitter.timebased.TestFile;
import tlb.utils.FileUtil;
import tlb.utils.SystemEnvironment;

import java.util.*;


/**
 * @understands criteria for splitting tests based on time taken
 */
public class TimeBasedTestSplitterCriteria extends JobFamilyAwareSplitterCriteria implements TalksToService {
    private final FileUtil fileUtil;
    private static final Logger logger = Logger.getLogger(TimeBasedTestSplitterCriteria.class.getName());
    private static final String NO_HISTORICAL_DATA = "no historical test time data, aborting attempt to balance based on time";

    public TimeBasedTestSplitterCriteria(TalkToService talkToService, SystemEnvironment env) {
        this(env);
        talksToService(talkToService);
    }

    public TimeBasedTestSplitterCriteria(SystemEnvironment env) {
        super(env);
        fileUtil = new FileUtil(env);
    }

    protected List<TlbSuiteFile> subset(List<TlbSuiteFile> fileResources) {
        List<TestFile> testFiles = testFiles(fileResources);
        Bucket thisBucket = buckets(testFiles);

        return resourcesFrom(thisBucket);
    }

    private Bucket buckets(List<TestFile> testFiles) {
        Bucket thisBucket = null;
        List<Bucket> buckets = new ArrayList<Bucket>();
        int thisPartition = talkToService.partitionNumber();
        for(int i = 1; i <= totalPartitions; i++) {
            Bucket bucket = new Bucket(i);
            if (i == thisPartition) thisBucket = bucket;
            buckets.add(bucket);
        }

        assignToBuckets(testFiles, buckets);

        return thisBucket;
    }

    private void assignToBuckets(List<TestFile> testFiles, List<Bucket> buckets) {
        for (TestFile testFile : testFiles) {
            buckets.get(0).add(testFile);
            Collections.sort(buckets);
        }
    }

    private List<TestFile> testFiles(List<TlbSuiteFile> fileResources) {
        List<SuiteTimeEntry> suiteTimeEntries = talkToService.getLastRunTestTimes();
        logger.info(String.format("historical test time data has entries for %s suites", suiteTimeEntries.size()));
        if (suiteTimeEntries.isEmpty()) {
            logger.warn(NO_HISTORICAL_DATA);
            throw new IllegalStateException(NO_HISTORICAL_DATA);
        }
        Map<String, TlbSuiteFile> fileNameToResource = new HashMap<String, TlbSuiteFile>();
        Set<String> currentFileNames = new HashSet<String>();
        logger.info("The TLB Resources are: \n" + fileResources + "\n");
        for (TlbSuiteFile fileResource : fileResources) {
            String name = fileResource.getName();
            currentFileNames.add(name);
            fileNameToResource.put(name, fileResource);
        }

        List<TestFile> testFiles = new ArrayList<TestFile>();
        double totalTime = 0;

        logger.info("The suite entries are: \n" + suiteTimeEntries + "\n");
        for (SuiteTimeEntry suiteTimeEntry : suiteTimeEntries) {
            String fileName = suiteTimeEntry.getName();
            double time = suiteTimeEntry.getTime();
            totalTime += time;
            if (currentFileNames.remove(fileName)) testFiles.add(new TestFile(fileNameToResource.get(fileName), time));
        }
        logger.info(String.format("%s entries of historical test time data found relavent", testFiles.size()));

        double avgTime = totalTime / suiteTimeEntries.size();

        logger.info(String.format("encountered %s new files which don't have historical time data, used average time [ %s ] to balance", currentFileNames.size(), avgTime));

        for (String newFile : currentFileNames) {
            testFiles.add(new TestFile(fileNameToResource.get(newFile), avgTime));
        }

        Collections.sort(testFiles);
        
        return testFiles;
    }

    private List<TlbSuiteFile> resourcesFrom(Bucket bucket) {
        ArrayList<TlbSuiteFile> resources = new ArrayList<TlbSuiteFile>();
        for (TlbSuiteFile file : bucket.files()) {
            resources.add(file);
        }
        return resources;
    }

}

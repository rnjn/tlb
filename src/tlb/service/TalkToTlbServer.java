package tlb.service;

import tlb.TlbConstants;
import tlb.domain.SuiteResultEntry;
import tlb.domain.SuiteTimeEntry;
import tlb.server.repo.EntryRepoFactory;
import tlb.service.http.DefaultHttpAction;
import tlb.service.http.HttpAction;
import tlb.utils.SystemEnvironment;

import java.util.List;

import static tlb.TlbConstants.TlbServer.JOB_NAMESPACE;
import static tlb.TlbConstants.TlbServer.URL;

/**
 * @understands exchanging balancing/ordering related data with the TLB server
 */
public class TalkToTlbServer extends SmoothingTalkToService {
    private final HttpAction httpAction;

    //reflectively invoked by factory
    public TalkToTlbServer(SystemEnvironment systemEnvironment) {
        this(systemEnvironment, new DefaultHttpAction());
    }

    public TalkToTlbServer(SystemEnvironment systemEnvironment, HttpAction httpAction) {
        super(systemEnvironment);
        this.httpAction = httpAction;
    }

    public void processedTestClassTime(String className, long time) {
        httpAction.put(getUrl(namespace(), suiteTimeRepoName()), String.format("%s: %s", className, time));
    }

    private String suiteTimeRepoName() {
        return EntryRepoFactory.SUITE_TIME;
    }

    public void testClassFailure(String className, boolean hasFailed) {
        httpAction.put(suiteResultUrl(), new SuiteResultEntry(className, hasFailed).toString());
    }

    public List<SuiteTimeEntry> fetchLastRunTestTimes() {
        return SuiteTimeEntry.parse(httpAction.get(getUrl(namespace(), suiteTimeRepoName(), environment.val(TlbConstants.TlbServer.JOB_VERSION))));
    }

    public List<SuiteResultEntry> getLastRunFailedTests() {
        return SuiteResultEntry.parse(httpAction.get(suiteResultUrl()));
    }

    public void publishSubsetSize(int size) {
        httpAction.post(getUrl(jobName(), EntryRepoFactory.SUBSET_SIZE), String.valueOf(size));
    }

    public void clearOtherCachingFiles() {
        //NOOP
        //TODO: if chattiness becomes a problem, this will need to be implemented sensibly
    }

    public int partitionNumber() {
        return Integer.parseInt(environment.val(TlbConstants.TlbServer.PARTITION_NUMBER));
    }

    public int totalPartitions() {
        return Integer.parseInt(environment.val(TlbConstants.TlbServer.TOTAL_PARTITIONS));
    }

    private String getUrl(String... parts) {
        final StringBuilder builder = new StringBuilder();
        builder.append(environment.val(URL));
        for (String part : parts) {
            builder.append("/").append(part);
        }
        return builder.toString();
    }

    private String suiteResultUrl() {
        return getUrl(namespace(), EntryRepoFactory.SUITE_RESULT);
    }

    private String jobName() {
        return String.format("%s-%s", namespace(), partitionNumber());
    }

    private String namespace() {
        return environment.val(JOB_NAMESPACE);
    }
}

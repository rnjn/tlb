package tlb.splitter;

import java.util.List;

import tlb.TlbConstants;
import tlb.TlbSuiteFile;
import tlb.utils.SystemEnvironment;

/**
 * @understands the criteria for splitting a given test suite 
 */
public abstract class TestSplitter {
    protected final SystemEnvironment env;
    public static final SystemEnvironment.EnvVar TLB_SPLITTER = new SystemEnvironment.DefaultedEnvVar(TlbConstants.TLB_SPLITTER, DefaultingTestSplitter.class.getCanonicalName());

    protected TestSplitter(SystemEnvironment env) {
        this.env = env;
    }

    public abstract List<TlbSuiteFile> filterSuites(List<TlbSuiteFile> fileResources);
}

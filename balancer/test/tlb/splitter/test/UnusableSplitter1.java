package tlb.splitter.test;

import tlb.TlbSuiteFile;
import tlb.splitter.TestSplitter;
import tlb.utils.SystemEnvironment;

import java.util.List;

public class UnusableSplitter1 extends TestSplitter {
    public UnusableSplitter1(SystemEnvironment env) {
        super(env);
    }

    @Override
    public List<TlbSuiteFile> filterSuites(List<TlbSuiteFile> fileResources) {
        throw new RuntimeException("Unusable criteira #1 won't work!");
    }
}

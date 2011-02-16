package tlb.splitter.test;

import tlb.TlbSuiteFile;
import tlb.splitter.TestSplitter;
import tlb.utils.SystemEnvironment;

import java.util.Arrays;
import java.util.List;

public class LastSelectingSplitter extends TestSplitter {
    public LastSelectingSplitter(SystemEnvironment env) {
        super(env);
    }

    @Override
    public List<TlbSuiteFile> filterSuites(List<TlbSuiteFile> fileResources) {
        return Arrays.asList(fileResources.get(fileResources.size() - 1));
    }
}
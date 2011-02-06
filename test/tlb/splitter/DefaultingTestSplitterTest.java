package tlb.splitter;

import org.apache.tools.ant.Project;
import org.junit.Before;
import org.junit.Test;
import tlb.TlbConstants;
import tlb.TlbFileResource;
import tlb.TlbSuiteFile;
import tlb.ant.JunitFileResource;
import tlb.utils.SuiteFileConvertor;
import tlb.utils.SystemEnvironment;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.fail;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;

public class DefaultingTestSplitterTest {
    private Project project;

    @Before
    public void setUp() throws Exception {
        project = new Project();
    }

    @Test
    public void shouldAttemptCriterionSpecifiedInOrder() throws Exception{
        TestSplitter criteria = defaultingCriteriaWith("tlb.splitter.test.UnusableSplitter1:tlb.splitter.test.UnusableSplitter2:tlb.splitter.test.LastSelectingSplitter");

        TlbFileResource foo = fileResource("foo");
        TlbFileResource bar = fileResource("bar");
        final SuiteFileConvertor convertor = new SuiteFileConvertor();
        final List<TlbSuiteFile> suiteFiles = convertor.toTlbSuiteFiles(Arrays.asList(foo, bar));
        List<TlbFileResource> filteredResources = convertor.toTlbFileResources(criteria.filterSuites(suiteFiles));
        assertThat(filteredResources.size(), is(1));
        assertThat(filteredResources, hasItem(bar));
    }

    @Test
    public void shouldAcceptSpacesBetweenCriterionNamesSpecified() throws Exception{
        TestSplitter criteria = defaultingCriteriaWith("tlb.splitter.test.UnusableSplitter1   :   tlb.splitter.test.UnusableSplitter2 :   tlb.splitter.test.LastSelectingSplitter");

        TlbFileResource foo = fileResource("foo");
        TlbFileResource bar = fileResource("bar");
        final SuiteFileConvertor convertor = new SuiteFileConvertor();
        final List<TlbSuiteFile> suiteFiles = convertor.toTlbSuiteFiles(Arrays.asList(foo, bar));
        List<TlbFileResource> filteredResources = convertor.toTlbFileResources(criteria.filterSuites(suiteFiles));
        assertThat(filteredResources.size(), is(1));
        assertThat(filteredResources, hasItem(bar));
    }

    private TestSplitter defaultingCriteriaWith(String criterion) {
        Map<String, String> envMap = new HashMap<String, String>();
        envMap.put(TlbConstants.TLB_PREFERRED_SPLITTERS, criterion);
        SystemEnvironment env = new SystemEnvironment(envMap);
        return new DefaultingTestSplitter(env);
    }

    @Test
    public void shouldBombIfNoCriteriaCanBeUsedSuccessfully() throws Exception{
        TestSplitter criteria = defaultingCriteriaWith("tlb.splitter.test.UnusableSplitter1:tlb.splitter.test.UnusableSplitter2");

        TlbFileResource foo = fileResource("foo");
        TlbFileResource bar = fileResource("bar");
        try {
            final SuiteFileConvertor convertor = new SuiteFileConvertor();
            final List<TlbSuiteFile> suiteFiles = convertor.toTlbSuiteFiles(Arrays.asList(foo, bar));
            convertor.toTlbFileResources(criteria.filterSuites(suiteFiles));
            fail("should have raised exception as no usable criteria specified");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("None of [tlb.splitter.test.UnusableSplitter1, tlb.splitter.test.UnusableSplitter2] could successfully split the test suites."));
        }
    }

    private JunitFileResource fileResource(String fileName) {
        return new JunitFileResource(project, fileName);
    }
}

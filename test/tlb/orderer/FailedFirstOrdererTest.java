package tlb.orderer;

import org.apache.tools.ant.Project;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.verification.Times;
import tlb.TestUtil;
import tlb.TlbFileResource;
import tlb.TlbSuiteFile;
import tlb.ant.JunitFileResource;
import tlb.domain.SuiteResultEntry;
import tlb.service.GoServer;
import tlb.service.TalksToServer;
import tlb.utils.SuiteFileConvertor;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.core.AnyOf.anyOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static tlb.TestUtil.initEnvironment;

public class FailedFirstOrdererTest {
    private FailedFirstOrderer orderer;
    private GoServer toCruise;
    private Project project;
    private String baseDir;

    @Before
    public void setUp() throws Exception {
        orderer = new FailedFirstOrderer(initEnvironment("job-1"));
        toCruise = mock(GoServer.class);
        project = new Project();
        baseDir = TestUtil.createTempFolder().getAbsolutePath();
        project.setBasedir(baseDir);
        orderer.talksToServer(toCruise);
    }

    @Test
    public void shouldImplementTalksToCruise() throws Exception{
        assertTrue("Failed first orderer must be talk to cruise aware", TalksToServer.class.isAssignableFrom(FailedFirstOrderer.class));
    }

    @Test
    public void shouldNotReorderTestsWhenNoneFailed() throws Exception{
        JunitFileResource bazClass = junitFileResource(baseDir, "foo/bar/Baz.class");
        JunitFileResource quuxClass = junitFileResource(baseDir, "foo/baz/Quux.class");
        JunitFileResource bangClass = junitFileResource(baseDir, "foo/baz/Bang.class");
        List<SuiteResultEntry> failedTests = Arrays.asList(new SuiteResultEntry("baz/bang/Foo.class", true), new SuiteResultEntry("foo/bar/Bang.class", true));
        when(toCruise.getLastRunFailedTests()).thenReturn(failedTests);
        List<TlbFileResource> fileList = new ArrayList<TlbFileResource>(Arrays.asList(bazClass, quuxClass, bangClass));
        final SuiteFileConvertor convertor = new SuiteFileConvertor();
        final List<TlbSuiteFile> tlbSuiteFiles = convertor.toTlbSuiteFiles(fileList);
        Collections.sort(tlbSuiteFiles, orderer);
        final List<TlbFileResource> resources = new ArrayList<TlbFileResource>(Arrays.asList(bazClass, quuxClass, bangClass));
        assertThat(convertor.toTlbFileResources(tlbSuiteFiles), is(resources));
        verify(toCruise, new Times(1)).getLastRunFailedTests();
    }

    @Test
    public void shouldReorderTestsToBringFailedTestsFirst() throws Exception{
        JunitFileResource bazClass = junitFileResource(baseDir, "foo/bar/Baz.class");
        JunitFileResource quuxClass = junitFileResource(baseDir, "foo/baz/Quux.class");
        JunitFileResource failedFooClass = junitFileResource(baseDir, "baz/bang/Foo.class");
        JunitFileResource failedBangClass = junitFileResource(baseDir, "foo/bar/Bang.class");
        List<SuiteResultEntry> failedTests = Arrays.asList(new SuiteResultEntry(convertToPlatformSpecificPath("baz/bang/Foo.class"), true), new SuiteResultEntry(convertToPlatformSpecificPath("foo/bar/Bang.class"), true));
        when(toCruise.getLastRunFailedTests()).thenReturn(failedTests);
        List<TlbFileResource> fileList = new ArrayList<TlbFileResource>(Arrays.asList(bazClass, failedFooClass, quuxClass, failedBangClass));
        final SuiteFileConvertor convertor = new SuiteFileConvertor();
        final List<TlbSuiteFile> tlbSuiteFiles = convertor.toTlbSuiteFiles(fileList);
        Collections.sort(tlbSuiteFiles, orderer);
        fileList = convertor.toTlbFileResources(tlbSuiteFiles);

        assertThat(fileList.get(0), anyOf(is((TlbFileResource) failedBangClass), is((TlbFileResource) failedFooClass)));
        assertThat(fileList.get(1), anyOf(is((TlbFileResource) failedBangClass), is((TlbFileResource) failedFooClass)));

        assertThat(fileList.get(2), anyOf(is((TlbFileResource) bazClass), is((TlbFileResource) quuxClass)));
        assertThat(fileList.get(3), anyOf(is((TlbFileResource) bazClass), is((TlbFileResource) quuxClass)));
        verify(toCruise, new Times(1)).getLastRunFailedTests();
    }

    @Test
    public void shouldNotReorderPassedSuitesInspiteOfHavingResults() throws Exception{
        JunitFileResource bazClass = junitFileResource(baseDir, "foo/bar/Baz.class");
        JunitFileResource quuxClass = junitFileResource(baseDir, "foo/baz/Quux.class");
        JunitFileResource reportedButPassedFooClass = junitFileResource(baseDir, "baz/bang/Foo.class");
        JunitFileResource reportedButPassedBangClass = junitFileResource(baseDir, "foo/bar/Bang.class");
        List<SuiteResultEntry> failedTests = Arrays.asList(new SuiteResultEntry("baz.bang.Foo", false), new SuiteResultEntry("foo.bar.Bang", false));
        when(toCruise.getLastRunFailedTests()).thenReturn(failedTests);
        final SuiteFileConvertor convertor = new SuiteFileConvertor();
        List<TlbSuiteFile> fileList = convertor.toTlbSuiteFiles(new ArrayList<TlbFileResource>(Arrays.asList(bazClass, reportedButPassedFooClass, quuxClass, reportedButPassedBangClass)));
        Collections.sort(fileList, orderer);

        final List<TlbFileResource> expected = new ArrayList<TlbFileResource>(Arrays.asList(bazClass, reportedButPassedFooClass, quuxClass, reportedButPassedBangClass));
        assertThat(convertor.toTlbFileResources(fileList), is(expected));
        verify(toCruise, new Times(1)).getLastRunFailedTests();
    }
    
    private JunitFileResource junitFileResource(String baseDir, String classRelPath) {
        JunitFileResource bazClass = new JunitFileResource(project, convertToPlatformSpecificPath(classRelPath));
        bazClass.setBaseDir(new File(baseDir));
        return bazClass;
    }

    private String convertToPlatformSpecificPath(String classRelPath) {
        return new File(classRelPath).getPath();
    }
}

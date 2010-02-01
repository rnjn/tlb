package com.thoughtworks.cruise.tlb.ant;

import static com.thoughtworks.cruise.tlb.TlbConstants.CRUISE_SERVER_URL;
import static com.thoughtworks.cruise.tlb.TlbConstants.TLB_CRITERIA;
import com.thoughtworks.cruise.tlb.splitter.CountBasedTestSplitterCriteria;
import com.thoughtworks.cruise.tlb.splitter.TestSplitterCriteria;
import com.thoughtworks.cruise.tlb.utils.FileUtil;
import com.thoughtworks.cruise.tlb.utils.SystemEnvironment;
import com.thoughtworks.cruise.tlb.ant.LoadBalancedFileSet;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.resources.FileResource;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.*;

public class LoadBalancedFileSetTest {
    private LoadBalancedFileSet fileSet;
    private File projectDir;

    @Before
    public void setUp() throws Exception {
        fileSet = new LoadBalancedFileSet(new SystemEnvironment(new HashMap<String, String>()));
        projectDir = FileUtil.createTempFolder();
        initFileSet(fileSet);
    }

    private void initFileSet(FileSet fileSet) {
        fileSet.setDir(projectDir);
        fileSet.setProject(new Project());
    }

    @Test
    public void shouldReturnAllFilesWhenThereIsNothingToSplit() {
        File newFile = FileUtil.createFileInFolder(projectDir, "abc");

        Iterator files = fileSet.iterator();

        assertThat(files.hasNext(), is(true));
        assertThat(((FileResource) files.next()).getFile(), is(newFile));
        assertThat(files.hasNext(), is(false));
    }

    @Test
    public void shouldReturnAListOfFilesWhichMatchAGivenMatcher() {
        File excluded = FileUtil.createFileInFolder(projectDir, "excluded");
        File included = FileUtil.createFileInFolder(projectDir, "included");

        List<FileResource> resources = Arrays.asList(new FileResource(excluded), new FileResource(included));

        TestSplitterCriteria criteria = mock(TestSplitterCriteria.class);
        when(criteria.filter(any(List.class))).thenReturn(Arrays.asList(new FileResource(included)));

        fileSet = new LoadBalancedFileSet(criteria);
        initFileSet(fileSet);
        Iterator files = fileSet.iterator();

        assertThat(files.hasNext(), is(true));
        assertThat(((FileResource) files.next()).getFile(), is(included));
        assertThat(files.hasNext(), is(false));
    }

    @Test
    public void shouldUseSystemPropertyToInstantiateCriteria() {
        fileSet = new LoadBalancedFileSet(initEnvironment("count"));
        assertThat(fileSet.getSplitterCriteria(), instanceOf(CountBasedTestSplitterCriteria.class));
    }

    private SystemEnvironment initEnvironment(String strategyName) {
        Map<String, String> map = new HashMap<String, String>();
        map.put(TLB_CRITERIA, strategyName);
        map.put(CRUISE_SERVER_URL, "https://localhost:8154/cruise");
        return new SystemEnvironment(map);
    }
}

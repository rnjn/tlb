package tlb.ant;

import org.apache.tools.ant.Project;
import org.junit.Test;
import tlb.TestUtil;

import java.io.File;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class JunitFileResourceTest {
    @Test
    public void shouldReturnTheClassNameAsName() {
        Project project = new Project();
        String baseDir = TestUtil.createTempFolder().getAbsolutePath();
        project.setBasedir(baseDir);
        JunitFileResource junitFileResource = new JunitFileResource(project, "foo/bar/Baz.class");
        junitFileResource.setBaseDir(new File(baseDir));
        assertThat(junitFileResource.getName(), is(new File("foo/bar/Baz.class").getPath()));
    }
}

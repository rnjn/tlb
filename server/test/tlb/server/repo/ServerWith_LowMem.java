package tlb.server.repo;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import tlb.TestUtil;
import tlb.TlbConstants;
import tlb.domain.SuiteTimeEntry;
import tlb.utils.SystemEnvironment;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import static tlb.server.repo.EntryRepoFactory.LATEST_VERSION;

public class ServerWith_LowMem {
    private EntryRepoFactory factory;
    private File baseDir;
    private TestUtil.LogFixture logFixture;

    private SystemEnvironment env() {
        final HashMap<String, String> env = new HashMap<String, String>();
        env.put(TlbConstants.Server.TLB_DATA_DIR.key, baseDir.getAbsolutePath());
        return new SystemEnvironment(env);
    }

    @Before
    public void setUp() throws Exception {
        baseDir = new File(TestUtil.createTempFolder(), "test_case_tlb_store");
        factory = new EntryRepoFactory(env());
        logFixture = new TestUtil.LogFixture();
    }

    @After
    public void tearDown() {
        factory = null;
        System.gc();
    }

    @Test
    public void shouldNotRunOutOfMemoryWhenTooManyRepositoriesAndVersionsAreLoaded() throws ClassNotFoundException, IOException, InterruptedException {
        for (int i = 0; i < 10000; i++) {
            String name = "foo-" + i;
            SuiteTimeRepo suiteTimeRepo = factory.createSuiteTimeRepo(name, LATEST_VERSION);
            for (int j = 0; j < 10000; j++)
                suiteTimeRepo.update(new SuiteTimeEntry(new String("foo bar baz bang quux"), j * 100));
            for (int j = 0; j < 100; j++)
                suiteTimeRepo.getSubRepo("abcde" + j);

            System.out.println("Created all versions of " + name + " repository successfully.");
        }
    }
}

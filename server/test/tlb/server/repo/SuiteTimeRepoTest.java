package tlb.server.repo;

import org.junit.Before;
import org.junit.Test;
import tlb.TestUtil;
import tlb.TlbConstants;
import tlb.domain.SuiteLevelEntry;
import tlb.domain.SuiteTimeEntry;
import tlb.utils.SystemEnvironment;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;
import static tlb.TestUtil.sortedList;
import static tlb.domain.SuiteTimeEntry.parseSingleEntry;
import static tlb.server.repo.EntryRepoFactory.LATEST_VERSION;

public class SuiteTimeRepoTest {
    private SuiteTimeRepo repo;
    protected File tmpDir;
    private EntryRepoFactory factory;

    @Before
    public void setUp() throws Exception {
        tmpDir = TestUtil.createTempFolder();
        factory = new EntryRepoFactory(env());
        repo = factory.createSuiteTimeRepo("name", LATEST_VERSION);
    }

    private SystemEnvironment env() {
        final HashMap<String, String> env = new HashMap<String, String>();
        env.put(TlbConstants.Server.TLB_DATA_DIR, tmpDir.getAbsolutePath());
        return new SystemEnvironment(env);
    }

    @Test
    public void shouldKeepVersionFrozenAcrossDumpAndReload() throws InterruptedException, ClassNotFoundException, IOException {
        repo.update(parseSingleEntry("foo.bar.Foo: 12"));
        repo.update(parseSingleEntry("foo.bar.Bar: 134"));
        sortedList(repo.list("foo"));
        repo.update(parseSingleEntry("foo.bar.Baz: 15"));
        repo.update(parseSingleEntry("foo.bar.Bar: 18"));
        final Thread exitHook = factory.exitHook();
        exitHook.start();
        exitHook.join();
        final SuiteTimeRepo newTestCaseRepo = new EntryRepoFactory(env()).createSuiteTimeRepo("name", LATEST_VERSION);
        final List<SuiteLevelEntry> frozenCollection = sortedList(newTestCaseRepo.list("foo"));
        assertThat(frozenCollection.size(), is(2));
        assertThat(frozenCollection, hasItem((SuiteLevelEntry) new SuiteTimeEntry("foo.bar.Foo", 12)));
        assertThat(frozenCollection, hasItem((SuiteLevelEntry) new SuiteTimeEntry("foo.bar.Bar", 134)));
    }
}

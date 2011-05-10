package tlb.server;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.internal.verification.Only;
import org.mockito.internal.verification.Times;
import org.restlet.Component;
import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.util.ServerList;
import tlb.TestUtil;
import tlb.TlbConstants;
import tlb.domain.SubsetSizeEntry;
import tlb.server.repo.EntryRepoFactory;
import tlb.server.repo.SubsetSizeRepo;
import tlb.utils.SystemEnvironment;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentMap;

import static junit.framework.Assert.fail;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static tlb.TlbConstants.Server.EntryRepoFactory.SUBSET_SIZE;
import static tlb.TlbConstants.Server.TLB_VERSION_LIFE_IN_DAYS;
import static tlb.server.repo.EntryRepoFactory.LATEST_VERSION;

public class TlbServerInitializerTest {
    private TlbServerInitializer initializer;
    private HashMap<String, String> systemEnv;
    private Context context = new Context();

    @Before
    public void setUp() {
        systemEnv = new HashMap<String, String>();
        SystemEnvironment env = new SystemEnvironment(systemEnv);
        initializer = new TlbServerInitializer(env);
    }

    @Test
    public void shouldCreateTlbApplication() {
        final Restlet app = initializer.application();
        assertThat(app, is(TlbApplication.class));
    }

    @Test
    public void shouldCreateApplicationContextWithRepoFactory() {
        ConcurrentMap<String,Object> map = initializer.application().getContext().getAttributes();
        assertThat(map.get(TlbConstants.Server.REPO_FACTORY), is(EntryRepoFactory.class));
    }

    @Test
    public void shouldInitializeTlbToRunOnConfiguredPort() {
        systemEnv.put(TlbConstants.Server.TLB_SERVER_PORT.key, "1234");
        assertThat(new TlbServerInitializer(new SystemEnvironment(systemEnv)).appPort(), is(1234));
    }

    @Test
    public void shouldInitializeTlbWithDefaultPortIfNotGiven() {
        Component component = initializer.init();
        ServerList servers = component.getServers();
        assertThat(servers.size(), is(1));
        assertThat(servers.get(0).getPort(), is(7019));
    }


    @Test
    public void shouldRegisterEntryRepoFactoryExitHook() {
        final EntryRepoFactory repoFactory = mock(EntryRepoFactory.class);
        class TestMain extends TlbServerInitializer {
            TestMain(SystemEnvironment env) {
                super(env);
            }

            @Override
            EntryRepoFactory repoFactory() {
                return repoFactory;
            }
        }
        TestMain main = new TestMain(new SystemEnvironment());
        Context ctx = main.application().getContext();
        assertThat((EntryRepoFactory) ctx.getAttributes().get(TlbConstants.Server.REPO_FACTORY), sameInstance(repoFactory));
        verify(repoFactory).registerExitHook();
    }

    @Test
    public void shouldInitializeEntryRepoFactoryWithPresentWorkingDirectoryAsDiskStorageRoot() throws IOException, ClassNotFoundException {
        EntryRepoFactory factory = initializer.repoFactory();
        File dir = TestUtil.mkdirInPwd("tlb_store");
        File file = new File(dir, EntryRepoFactory.name("foo", LATEST_VERSION, SUBSET_SIZE));
        List<SubsetSizeEntry> entries = writeEntriedTo(file);
        SubsetSizeRepo repo = factory.createSubsetRepo("foo", LATEST_VERSION);
        assertThat((List<SubsetSizeEntry>) repo.list(), is(entries));
    }

    private List<SubsetSizeEntry> writeEntriedTo(File file) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        List<SubsetSizeEntry> entries = Arrays.asList(new SubsetSizeEntry(1), new SubsetSizeEntry(2), new SubsetSizeEntry(3));
        for (SubsetSizeEntry entry : entries) {
            writer.append(entry.dump());
        }
        writer.close();
        return entries;
    }

    @Test
    public void shouldHonorDiskStorageRootOverride() throws IOException, ClassNotFoundException {
        String tmpDir = TestUtil.createTempFolder().getAbsolutePath();
        systemEnv.put(TlbConstants.Server.TLB_DATA_DIR.key, tmpDir);
        initializer = new TlbServerInitializer(new SystemEnvironment(systemEnv));
        EntryRepoFactory factory = initializer.repoFactory();
        File file = new File(tmpDir, EntryRepoFactory.name("quux", LATEST_VERSION, SUBSET_SIZE));
        writeEntriedTo(file);
        SubsetSizeRepo repo = factory.createSubsetRepo("quux", LATEST_VERSION);
        assertThat((List<SubsetSizeEntry>) repo.list(), is(Arrays.asList(new SubsetSizeEntry(1), new SubsetSizeEntry(2), new SubsetSizeEntry(3))));
    }
    
    @Test
    public void shouldEscapeTheEscapeCharInName() {
        assertThat(EntryRepoFactory.name("foo", "bar", "baz"), is("foo_bar_baz"));
        assertThat(EntryRepoFactory.name("fo_o", "b_ar_", "baz|"), is("fo__o_b__ar___baz|"));
    }

    @Test
    public void shouldSetTimerToPurgeOldVersions() {
        final TimerTask[] tasks = new TimerTask[1];
        final EntryRepoFactory repoFactory = mock(EntryRepoFactory.class);
        final Timer timer = new Timer() {
            @Override
            public void schedule(TimerTask task, long delay, long period) {
                tasks[0] = task;
                assertThat(delay, is(0l));
                assertThat(period, is(1*24*60*60*1000l));
            }
        };

        new TlbServerInitializer(new SystemEnvironment(systemEnv), timer) {
            @Override
            EntryRepoFactory repoFactory() {
                return repoFactory;
            }
        }.init();

        assertThat(tasks[0], is(nullValue()));
        verify(repoFactory).registerExitHook();
        verifyNoMoreInteractions(repoFactory);

        final EntryRepoFactory anotherRepoFactory = mock(EntryRepoFactory.class);

        systemEnv.put(TLB_VERSION_LIFE_IN_DAYS.key, "3");

        new TlbServerInitializer(new SystemEnvironment(systemEnv), timer) {
            @Override
            EntryRepoFactory repoFactory() {
                return anotherRepoFactory;
            }
        }.init();

        tasks[0].run();
        verify(anotherRepoFactory).purgeVersionsOlderThan(3);
        verify(anotherRepoFactory).registerExitHook();
        verifyNoMoreInteractions(repoFactory);
    }

    @Test
    public void shouldNotSetTimerIfNoVersionLifeIsMentioned() {
        systemEnv.put(TLB_VERSION_LIFE_IN_DAYS.key, "-1");

        final EntryRepoFactory repoFactory = mock(EntryRepoFactory.class);
        final Timer timer = new Timer() {
            @Override
            public void schedule(TimerTask task, long delay, long period) {
                fail("Should not have scheduled anything!");
            }
        };

        new TlbServerInitializer(new SystemEnvironment(systemEnv), timer) {
            @Override
            EntryRepoFactory repoFactory() {
                return repoFactory;
            }
        }.init();

        verify(repoFactory).registerExitHook();
        verifyNoMoreInteractions(repoFactory);
    }
}

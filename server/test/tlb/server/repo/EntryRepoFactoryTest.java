package tlb.server.repo;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import tlb.TestUtil;
import tlb.TlbConstants;
import tlb.domain.SubsetSizeEntry;
import tlb.domain.SuiteResultEntry;
import tlb.domain.SuiteTimeEntry;
import tlb.domain.TimeProvider;
import tlb.utils.SystemEnvironment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.*;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.hasItem;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.mockito.Mockito.*;
import static tlb.TestUtil.deref;
import static tlb.TlbConstants.Server.EntryRepoFactory.SUBSET_SIZE;
import static tlb.server.repo.EntryRepoFactory.LATEST_VERSION;


public class EntryRepoFactoryTest {
    private EntryRepoFactory factory;
    private File baseDir;
    private TestUtil.LogFixture logFixture;

    private SystemEnvironment env() {
        final HashMap<String, String> env = new HashMap<String, String>();
        env.put(TlbConstants.Server.TLB_DATA_DIR, baseDir.getAbsolutePath());
        return new SystemEnvironment(env);
    }

    @Before
    public void setUp() throws Exception {
        baseDir = new File(TestUtil.createTempFolder(), "test_case_tlb_store");
        factory = new EntryRepoFactory(env());
        logFixture = new TestUtil.LogFixture();
    }

    @Test
    public void shouldPassFactoryAndNamespaceToEachRepo() throws ClassNotFoundException, IOException {
        final EntryRepo createdEntryRepo = mock(EntryRepo.class);
        final EntryRepo repo = factory.findOrCreate("namespace", "old_version", "suite_time", new EntryRepoFactory.Creator<EntryRepo>() {
            public EntryRepo create() {
                return createdEntryRepo;
            }
        });
        assertThat(repo, sameInstance(createdEntryRepo));
        verify(createdEntryRepo).setFactory(factory);
        verify(createdEntryRepo).setNamespace("namespace");
        verify(createdEntryRepo).setIdentifier("namespace_old__version_suite__time");
    }

    @Test
    public void shouldNotOverrideSubsetRepoWithSuiteTimeRepo() throws ClassNotFoundException, IOException {
        SubsetSizeRepo subsetRepo = factory.createSubsetRepo("dev", LATEST_VERSION);
        SuiteTimeRepo suiteTimeRepo = factory.createSuiteTimeRepo("dev", LATEST_VERSION);
        SuiteResultRepo suiteResultRepo = factory.createSuiteResultRepo("dev", LATEST_VERSION);
        assertThat(factory.createSubsetRepo("dev", LATEST_VERSION), sameInstance(subsetRepo));
        assertThat(subsetRepo, is(SubsetSizeRepo.class));
        assertThat(factory.createSuiteTimeRepo("dev", LATEST_VERSION), sameInstance(suiteTimeRepo));
        assertThat(suiteTimeRepo, is(SuiteTimeRepo.class));
        assertThat(factory.createSuiteResultRepo("dev", LATEST_VERSION), sameInstance(suiteResultRepo));
        assertThat(suiteResultRepo, is(SuiteResultRepo.class));
    }

    @Test
    public void shouldReturnOneRepositoryForOneFamilyName() throws ClassNotFoundException, IOException {
        assertThat(factory.createSubsetRepo("dev", LATEST_VERSION), sameInstance(factory.createSubsetRepo("dev", LATEST_VERSION)));
    }

    @Test
    public void shouldCallDiskDumpForEachRepoAtExit() throws InterruptedException, IOException {
        EntryRepo repoFoo = mock(EntryRepo.class);
        EntryRepo repoBar = mock(EntryRepo.class);
        EntryRepo repoBaz = mock(EntryRepo.class);
        when(repoFoo.diskDump()).thenReturn("foo-data");
        when(repoBar.diskDump()).thenReturn("bar-data");
        when(repoBaz.diskDump()).thenReturn("baz-data");
        factory.getRepos().put("foo", repoFoo);
        factory.getRepos().put("bar", repoBar);
        factory.getRepos().put("baz", repoBaz);
        Thread exitHook = factory.exitHook();
        exitHook.start();
        exitHook.join();
        verify(repoFoo).diskDump();
        verify(repoBar).diskDump();
        verify(repoBaz).diskDump();
    }

    @Test
    public void shouldBeAbleToLoadFromDumpedFile() throws ClassNotFoundException, IOException, InterruptedException {
        SubsetSizeRepo subsetSizeRepo = factory.createSubsetRepo("foo", LATEST_VERSION);
        subsetSizeRepo.add(new SubsetSizeEntry(50));
        subsetSizeRepo.add(new SubsetSizeEntry(100));
        subsetSizeRepo.add(new SubsetSizeEntry(200));

        SuiteTimeRepo subsetTimeRepo = factory.createSuiteTimeRepo("bar", LATEST_VERSION);
        subsetTimeRepo.update(new SuiteTimeEntry("foo.bar.Baz", 10));
        subsetTimeRepo.update(new SuiteTimeEntry("bar.baz.Quux", 20));

        SuiteResultRepo subsetResultRepo = factory.createSuiteResultRepo("baz", LATEST_VERSION);
        subsetResultRepo.update(new SuiteResultEntry("foo.bar.Baz", true));
        subsetResultRepo.update(new SuiteResultEntry("bar.baz.Quux", false));

        Thread exitHook = factory.exitHook();
        exitHook.start();
        exitHook.join();
        EntryRepoFactory otherFactoryInstance = new EntryRepoFactory(env());
        assertThat(otherFactoryInstance.createSubsetRepo("foo", LATEST_VERSION).list(), is((Collection<SubsetSizeEntry>) Arrays.asList(new SubsetSizeEntry(50), new SubsetSizeEntry(100), new SubsetSizeEntry(200))));
        assertThat(otherFactoryInstance.createSuiteTimeRepo("bar", LATEST_VERSION).list().size(), is(2));
        assertThat(otherFactoryInstance.createSuiteTimeRepo("bar", LATEST_VERSION).list(), hasItems(new SuiteTimeEntry("foo.bar.Baz", 10), new SuiteTimeEntry("bar.baz.Quux", 20)));
        assertThat(otherFactoryInstance.createSuiteResultRepo("baz", LATEST_VERSION).list().size(), is(2));
        assertThat(otherFactoryInstance.createSuiteResultRepo("baz", LATEST_VERSION).list(), hasItems(new SuiteResultEntry("foo.bar.Baz", true), new SuiteResultEntry("bar.baz.Quux", false)));
    }

    @Test
    public void shouldLogExceptionsButContinueDumpingRepositories() throws InterruptedException, IOException {
        EntryRepo repoFoo = mock(EntryRepo.class);
        EntryRepo repoBar = mock(EntryRepo.class);
        factory.getRepos().put("foo_subset__size", repoFoo);
        factory.getRepos().put("bar_subset__size", repoBar);
        when(repoFoo.diskDump()).thenThrow(new IOException("test exception"));
        when(repoBar.diskDump()).thenReturn("bar-data");
        logFixture.startListening();
        factory.run();
        logFixture.stopListening();
        logFixture.assertHeard("disk dump of foo_subset__size failed");
        verify(repoFoo).diskDump();
        verify(repoBar).diskDump();
    }

    @Test
    public void shouldWireUpAtExitHook() {
        factory.registerExitHook();
        try {
            Runtime.getRuntime().addShutdownHook(factory.exitHook());
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Hook previously registered"));
        }
    }

    @Test
    public void shouldFeedTheDiskDumpContentsToSubsetRepo() {
        TestUtil.createTempFolder();
    }

    @Test
    public void shouldUseWorkingDirAsDiskStorageRootWhenNotGiven() throws IOException, ClassNotFoundException {
        final File workingDirStorage = new File(TlbConstants.Server.DEFAULT_TLB_DATA_DIR);
        workingDirStorage.mkdirs();
        File file = new File(workingDirStorage, EntryRepoFactory.name("foo", LATEST_VERSION, SUBSET_SIZE));
        FileUtils.writeStringToFile(file, "1\n2\n3\n");
        EntryRepoFactory factory = new EntryRepoFactory(new SystemEnvironment(new HashMap<String, String>()));
        SubsetSizeRepo repo = factory.createSubsetRepo("foo", LATEST_VERSION);
        assertThat(repo.list(), is((Collection<SubsetSizeEntry>) Arrays.asList(new SubsetSizeEntry(1), new SubsetSizeEntry(2), new SubsetSizeEntry(3))));
    }

    @Test
    public void shouldLoadDiskDumpFromStorageRoot() throws IOException, ClassNotFoundException {
        baseDir.mkdirs();
        File file = new File(baseDir, EntryRepoFactory.name("foo", LATEST_VERSION, SUBSET_SIZE));
        FileUtils.writeStringToFile(file, "1\n2\n3\n");
        SubsetSizeRepo repo = factory.createSubsetRepo("foo", LATEST_VERSION);
        assertThat(repo.list(), is((Collection<SubsetSizeEntry>) Arrays.asList(new SubsetSizeEntry(1), new SubsetSizeEntry(2), new SubsetSizeEntry(3))));
    }

    @Test
    public void shouldNotLoadDiskDumpWhenUsingARepoThatIsAlreadyCreated() throws ClassNotFoundException, IOException {
        SubsetSizeRepo fooRepo = factory.createSubsetRepo("foo", LATEST_VERSION);
        File file = new File(baseDir, EntryRepoFactory.name("foo", LATEST_VERSION, SUBSET_SIZE));
        ObjectOutputStream outStream = new ObjectOutputStream(new FileOutputStream(file));
        outStream.writeObject(new ArrayList<SubsetSizeEntry>(Arrays.asList(new SubsetSizeEntry(1), new SubsetSizeEntry(2), new SubsetSizeEntry(3))));
        outStream.close();
        assertThat(fooRepo.list().size(), is(0));
        assertThat(factory.createSubsetRepo("foo", LATEST_VERSION).list().size(), is(0));
    }

    @Test
    public void shouldPurgeDiskDumpAndRepositoryWhenAsked() throws IOException, ClassNotFoundException, InterruptedException {
        SuiteTimeRepo fooRepo = factory.createSuiteTimeRepo("foo", LATEST_VERSION);
        fooRepo.update(new SuiteTimeEntry("foo.bar.Baz", 15));
        fooRepo.update(new SuiteTimeEntry("foo.bar.Qux", 80));
        final Thread exitHook = factory.exitHook();
        exitHook.start();
        exitHook.join();
        factory.purge(fooRepo.identifier);
        fooRepo = factory.createSuiteTimeRepo("foo", LATEST_VERSION);
        assertThat(fooRepo.list().size(), is(0));
        fooRepo = new EntryRepoFactory(env()).createSuiteTimeRepo("foo", LATEST_VERSION);
        assertThat(fooRepo.list().size(), is(0));
    }

    @Test
    public void shouldPurgeDiskDumpAndRepositoryOlderThanGivenTime() throws IOException, ClassNotFoundException, InterruptedException {
        final GregorianCalendar[] cal = new GregorianCalendar[1];
        final TimeProvider timeProvider = new TimeProvider() {
            @Override
            public GregorianCalendar now() {
                GregorianCalendar gregorianCalendar = cal[0];
                return gregorianCalendar == null ? null : (GregorianCalendar) gregorianCalendar.clone();
            }
        };
        final EntryRepoFactory factory = new EntryRepoFactory(baseDir, timeProvider, 1);

        SuiteTimeRepo repo = factory.createSuiteTimeRepo("foo", LATEST_VERSION);
        repo.update(new SuiteTimeEntry("foo.bar.Baz", 15));
        repo.update(new SuiteTimeEntry("foo.bar.Quux", 80));

        cal[0] = new GregorianCalendar(2010, 6, 7, 0, 37, 12);
        Collection<SuiteTimeEntry> oldList = repo.list("old");
        assertThat(oldList.size(), is(2));
        assertThat(oldList, hasItem(new SuiteTimeEntry("foo.bar.Baz", 15)));
        assertThat(oldList, hasItem(new SuiteTimeEntry("foo.bar.Quux", 80)));

        repo.update(new SuiteTimeEntry("foo.bar.Bang", 130));
        repo.update(new SuiteTimeEntry("foo.bar.Baz", 20));

        oldList = repo.list("old");
        assertThat(oldList.size(), is(2));
        assertThat(oldList, hasItem(new SuiteTimeEntry("foo.bar.Baz", 15)));
        assertThat(oldList, hasItem(new SuiteTimeEntry("foo.bar.Quux", 80)));


        cal[0] = new GregorianCalendar(2010, 6, 9, 0, 37, 12);
        Collection<SuiteTimeEntry> notSoOld = repo.list("not_so_old");
        assertThat(notSoOld.size(), is(3));
        assertThat(notSoOld, hasItem(new SuiteTimeEntry("foo.bar.Baz", 20)));
        assertThat(notSoOld, hasItem(new SuiteTimeEntry("foo.bar.Quux", 80)));
        assertThat(notSoOld, hasItem(new SuiteTimeEntry("foo.bar.Bang", 130)));

        repo.update(new SuiteTimeEntry("foo.bar.Foo", 12));

        final Thread exitHook = factory.exitHook();
        exitHook.start();
        exitHook.join();

        cal[0] = new GregorianCalendar(2010, 6, 10, 0, 37, 12);
        factory.purgeVersionsOlderThan(2);

        oldList = repo.list("old");
        assertThat(oldList.size(), is(4));
        assertThat(oldList, hasItem(new SuiteTimeEntry("foo.bar.Baz", 20)));
        assertThat(oldList, hasItem(new SuiteTimeEntry("foo.bar.Quux", 80)));
        assertThat(oldList, hasItem(new SuiteTimeEntry("foo.bar.Bang", 130)));
        assertThat(oldList, hasItem(new SuiteTimeEntry("foo.bar.Foo", 12)));

        notSoOld = repo.list("not_so_old");
        assertThat(notSoOld.size(), is(3));
        assertThat(notSoOld, hasItem(new SuiteTimeEntry("foo.bar.Baz", 20)));
        assertThat(notSoOld, hasItem(new SuiteTimeEntry("foo.bar.Quux", 80)));
        assertThat(notSoOld, hasItem(new SuiteTimeEntry("foo.bar.Bang", 130)));
    }

    @Test
    public void shouldHaveATimerThatPurgesOldVersions() throws ClassNotFoundException, IOException {
        final VersioningEntryRepo repo1 = mock(VersioningEntryRepo.class);
        final VersioningEntryRepo repo2 = mock(VersioningEntryRepo.class);
        final VersioningEntryRepo repo3 = mock(VersioningEntryRepo.class);
        doThrow(new IOException("test exception")).when(repo2).purgeOldVersions(12);
        findOrCreateRepo(repo1, "foo");
        findOrCreateRepo(repo2, "bar");
        findOrCreateRepo(repo3, "baz");
        logFixture.startListening();
        factory.purgeVersionsOlderThan(12);
        logFixture.stopListening();
        verify(repo1).purgeOldVersions(12);
        verify(repo2).purgeOldVersions(12);
        verify(repo3).purgeOldVersions(12);
        logFixture.assertHeard("failed to delete older versions for repo identified by 'bar_LATEST_foo__bar'");
        logFixture.assertHeardException(new IOException("test exception"));
    }

    private EntryRepo findOrCreateRepo(final VersioningEntryRepo repo, String name) throws IOException, ClassNotFoundException {
        return factory.findOrCreate(name, LATEST_VERSION, "foo_bar", new EntryRepoFactory.Creator<EntryRepo>() {
            public EntryRepo create() {
                return repo;
            }
        });
    }

    @Test
    public void shouldCheckRepoExistenceBeforeTryingPurge() throws IOException, IllegalAccessException {
        factory.createSuiteTimeRepo("foo", LATEST_VERSION);
        Map<String, EntryRepo> repos = (Map<String, EntryRepo>) deref("repos", factory);
        List<String> keys = new ArrayList<String>(repos.keySet());
        assertThat(keys.size(), is(1));
        String fooKey = keys.get(0);
        repos.clear();
        try {
            factory.purge(fooKey);
        } catch (IOException e) {
            e.printStackTrace();
            fail("Should not fail when trying to purge already purged entry");
        }
    }
}

package tlb.server.repo;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import tlb.TestUtil;
import tlb.TlbConstants;
import tlb.domain.SuiteLevelEntry;
import tlb.domain.TimeProvider;
import tlb.utils.FileUtil;
import tlb.utils.SystemEnvironment;

import java.io.*;
import java.util.*;

import static junit.framework.Assert.fail;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static tlb.server.repo.TestCaseRepo.TestCaseEntry.parseSingleEntry;

public class SuiteEntryRepoTest {

    private TestCaseRepo testCaseRepo;

    @Before
    public void setUp() {
        testCaseRepo = new TestCaseRepo(new TimeProvider());
    }

    @Test
    public void shouldStoreAttributesFactorySets() throws ClassNotFoundException, IOException {
        final EntryRepoFactory factory = new EntryRepoFactory(new SystemEnvironment(Collections.singletonMap(TlbConstants.Server.TLB_DATA_DIR.key, TestUtil.createTempFolder().getAbsolutePath())));
        final SuiteEntryRepo entryRepo = (SuiteEntryRepo) factory.findOrCreate("name_space", "version", "type", new EntryRepoFactory.Creator<SuiteEntryRepo>() {
            public SuiteEntryRepo create() {
                return new SuiteEntryRepo<TestCaseRepo.TestCaseEntry>() {
                    public Collection<TestCaseRepo.TestCaseEntry> list(String version) throws IOException, ClassNotFoundException {
                        return null;
                    }

                    public List<TestCaseRepo.TestCaseEntry> parse(String string) {
                        return TestCaseRepo.TestCaseEntry.parse(string);
                    }
                };
            }
        });
        assertThat(entryRepo.factory, sameInstance(factory));
        assertThat(entryRepo.namespace, is("name_space"));
        assertThat(entryRepo.identifier, is("name__space_version_type"));
    }

    @Test
    public void shouldNotAllowAdditionOfEntries() {
        try {
            testCaseRepo.add(parseSingleEntry("shouldBar#Bar"));
            fail("add should not have been allowed for suite repo");
        } catch (UnsupportedOperationException e) {
            assertThat(e.getMessage(), is("add not allowed on repository"));
        }
    }

    @Test
    public void shouldRecordSuiteRecordWhenUpdated() {
        testCaseRepo.update(parseSingleEntry("shouldBar#Bar"));
        testCaseRepo.update(parseSingleEntry("shouldFoo#Foo"));
        List<SuiteLevelEntry> entryList = TestUtil.sortedList(testCaseRepo.list());
        assertThat(entryList.size(), is(2));
        assertThat((TestCaseRepo.TestCaseEntry) entryList.get(0), is(new TestCaseRepo.TestCaseEntry("shouldBar", "Bar")));
        assertThat((TestCaseRepo.TestCaseEntry) entryList.get(1), is(new TestCaseRepo.TestCaseEntry("shouldFoo", "Foo")));
    }

    @Test
    public void shouldOverwriteExistingEntryIfAddedAgain() {
        testCaseRepo.update(parseSingleEntry("shouldBar#Bar"));
        testCaseRepo.update(parseSingleEntry("shouldFoo#Foo"));
        testCaseRepo.update(parseSingleEntry("shouldBar#Foo"));
        List<SuiteLevelEntry> entryList = TestUtil.sortedList(testCaseRepo.list());
        assertThat(entryList.size(), is(2));
        assertThat((TestCaseRepo.TestCaseEntry) entryList.get(0), is(new TestCaseRepo.TestCaseEntry("shouldBar", "Foo")));
        assertThat((TestCaseRepo.TestCaseEntry) entryList.get(1), is(new TestCaseRepo.TestCaseEntry("shouldFoo", "Foo")));
    }

    @Test
    public void shouldDumpDataOnGivenOutputStream() throws IOException, ClassNotFoundException {
        testCaseRepo.update(parseSingleEntry("shouldBar#Bar"));
        testCaseRepo.update(parseSingleEntry("shouldFoo#Foo"));
        String dump = testCaseRepo.diskDump();
        assertThat(dump, is("shouldBar#Bar\nshouldFoo#Foo\n"));
    }

    @Test
    public void shouldLoadFromGivenReader() throws IOException, ClassNotFoundException {
        File tempFile = File.createTempFile("temp-file", "something");
        tempFile.deleteOnExit();

        FileUtils.writeStringToFile(tempFile, "shouldBar#Bar\nshouldFoo#Foo\n");

        final FileReader reader = new FileReader(tempFile);
        testCaseRepo.load(FileUtil.readIntoString(new BufferedReader(reader)));
        assertThat(TestUtil.sortedList(testCaseRepo.list()), is(listOf(new TestCaseRepo.TestCaseEntry("shouldBar", "Bar"), new TestCaseRepo.TestCaseEntry("shouldFoo", "Foo"))));
    }

    @Test
    public void shouldVersionListItself() {
        testCaseRepo.update(parseSingleEntry("shouldBar#Bar"));
        testCaseRepo.update(parseSingleEntry("shouldFoo#Foo"));
        List<SuiteLevelEntry> entryList = TestUtil.sortedList(testCaseRepo.list());
        assertThat(entryList.size(), is(2));
        assertThat((TestCaseRepo.TestCaseEntry) entryList.get(0), is(new TestCaseRepo.TestCaseEntry("shouldBar", "Bar")));
        assertThat((TestCaseRepo.TestCaseEntry) entryList.get(1), is(new TestCaseRepo.TestCaseEntry("shouldFoo", "Foo")));

    }

    private List<SuiteLevelEntry> listOf(SuiteLevelEntry... entries) {
        ArrayList<SuiteLevelEntry> list = new ArrayList<SuiteLevelEntry>();
        for (SuiteLevelEntry entry : entries) {
            list.add(entry);
        }
        return list;
    }
}

package tlb.server.repo;

import tlb.domain.SubsetSizeEntry;
import org.junit.Before;
import org.junit.Test;
import tlb.utils.FileUtil;

import java.io.*;
import java.util.List;

import static junit.framework.Assert.fail;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class SubsetSizeRepoTest {
    private SubsetSizeRepo subsetSizeRepo;

    @Before
    public void setUp() throws Exception {
        subsetSizeRepo = new SubsetSizeRepo();
    }
    
    @Test
    public void shouldNotAllowUpdate() {
        try {
            subsetSizeRepo.update(new SubsetSizeEntry(10));
            fail("update should not have been allowed");
        } catch (UnsupportedOperationException e) {
            assertThat(e.getMessage(), is("update not allowed on repository"));
        }
    }

    @Test
    public void shouldListAddedEntries() {
        addToRepo();

        List<SubsetSizeEntry> entries = (List<SubsetSizeEntry>) subsetSizeRepo.list();

        assertListContents(entries);
    }

    private void addToRepo() {
        subsetSizeRepo.add(new SubsetSizeEntry(10));
        subsetSizeRepo.add(new SubsetSizeEntry(12));
        subsetSizeRepo.add(new SubsetSizeEntry(7));
    }

    private void assertListContents(List<SubsetSizeEntry> entries) {
        assertThat(entries.size(), is(3));
        assertThat(entries.get(0), is(new SubsetSizeEntry(10)));
        assertThat(entries.get(1), is(new SubsetSizeEntry(12)));
        assertThat(entries.get(2), is(new SubsetSizeEntry(7)));
    }

    @Test
    public void shouldDumpDataAsString() throws IOException, ClassNotFoundException {
        addToRepo();
        String dump = subsetSizeRepo.diskDump();
        subsetSizeRepo.load(dump);
        assertListContents((List<SubsetSizeEntry>) subsetSizeRepo.list());
    }

    @Test
    public void shouldLoadFromGivenReader() throws IOException, ClassNotFoundException {
        final StringReader reader = new StringReader("10\n12\n7\n");
        subsetSizeRepo.load(FileUtil.readIntoString(new BufferedReader(reader)));
        assertListContents((List<SubsetSizeEntry>) subsetSizeRepo.list());
    }
    
    @Test
    public void shouldNotAllowVersioning() {
        try {
            subsetSizeRepo.list("crap");
            fail("should not have allowed versioning");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("versioning not allowed"));
        }
    }

    @Test
    public void shouldKnowHowToParseAnEntry() {
        List<SubsetSizeEntry> subsetSizeEntry = subsetSizeRepo.parse("10\n19");
        assertThat(subsetSizeEntry.get(0), is(new SubsetSizeEntry(10)));
        assertThat(subsetSizeEntry.get(1), is(new SubsetSizeEntry(19)));
        assertThat(subsetSizeEntry.size(), is(2));
    }
}

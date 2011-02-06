package tlb.service;

import org.apache.tools.ant.util.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.verification.Times;
import tlb.TestUtil;
import tlb.TlbConstants;
import tlb.domain.SuiteResultEntry;
import tlb.domain.SuiteTimeEntry;
import tlb.storage.TlbEntryRepository;
import tlb.utils.SystemEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static tlb.TestUtil.deref;
import static tlb.TestUtil.updateEnv;

public class SmoothingServerTest {
    private SystemEnvironment env;
    private SmoothingServer server;
    private ArrayList<SuiteTimeEntry> fetchedEntries;
    private TestUtil.LogFixture logFixture;
    private SmoothingServer delegate;

    @Before
    public void setUp() throws IllegalAccessException {
        fetchedEntries = new ArrayList<SuiteTimeEntry>();
        HashMap<String, String> variables = new HashMap<String, String>();
        variables.put(TlbConstants.TLB_SMOOTHING_FACTOR, "0.05");
        env = new SystemEnvironment(variables);

        delegate = mock(SmoothingServer.class);
        server = new DelegatingSmoothingServer(delegate, env);
        FileUtils.delete(testTimeCacheRepo().getFile());

        logFixture = new TestUtil.LogFixture();
    }

    private TlbEntryRepository testTimeCacheRepo() throws IllegalAccessException {
        return (TlbEntryRepository) deref("oldTestTimesRepo", server);
    }

    @Test
    public void shouldSmoothenUsingSetSmoothingFactor() throws NoSuchFieldException, IllegalAccessException {
        when(delegate.fetchLastRunTestTimes()).thenReturn(Arrays.asList(new SuiteTimeEntry("foo/bar/Baz.class", 12l)));
        server.testClassTime("foo/bar/Baz.class", 102l);
        verify(delegate).processedTestClassTime("foo/bar/Baz.class", 17l);
    }

    @Test
    public void shouldPassTimeAsIsWhenForANewTestSuite() {
        when(delegate.fetchLastRunTestTimes()).thenReturn(Arrays.asList(new SuiteTimeEntry("foo/bar/Baz.class", 12l)));
        server.testClassTime("foo/baz/Quux.class", 77l);
        verify(delegate).processedTestClassTime("foo/baz/Quux.class", 77l);
    }

    @Test
    public void shouldNotFailWhenHasNoHistory() {//should just skip smoothing
        final RuntimeException exception = new RuntimeException("failed for some reason");
        when(delegate.fetchLastRunTestTimes()).thenThrow(exception);
        logFixture.startListening();
        server.testClassTime("foo/baz/Bam.class", 35l);
        logFixture.stopListening();
        verify(delegate).processedTestClassTime("foo/baz/Bam.class", 35l);
        logFixture.assertHeard("could not load test times for smoothing.: 'failed for some reason'");
        logFixture.assertHeardException(exception);
    }

    @Test
    public void shouldCacheTestRunTimes() {
        when(delegate.fetchLastRunTestTimes()).thenReturn(Arrays.asList(new SuiteTimeEntry("foo/bar/Baz.class", 12l), new SuiteTimeEntry("quux/bang/Boom.class", 15l)));
        server.testClassTime("foo/baz/Quux.class", 77l);
        verify(delegate, new Times(1)).fetchLastRunTestTimes();
        verify(delegate).processedTestClassTime("foo/baz/Quux.class", 77l);
        server.testClassTime("quux/bang/Boom.class", 18l);
        verify(delegate).processedTestClassTime("quux/bang/Boom.class", 15l);
        server.testClassTime("foo/bar/Baz.class", 90l);
        verify(delegate).processedTestClassTime("foo/bar/Baz.class", 16l);
    }
    
    @Test
    public void shouldClearCachedOldTestTimesOnClearCall() throws IllegalAccessException {
        when(delegate.fetchLastRunTestTimes()).thenReturn(Arrays.asList(new SuiteTimeEntry("foo/bar/Baz.class", 12l), new SuiteTimeEntry("quux/bang/Boom.class", 15l)));
        server.testClassTime("foo/baz/Quux.class", 77l);
        verify(delegate).processedTestClassTime("foo/baz/Quux.class", 77l);
        assertThat(testTimeCacheRepo().getFile().exists(), is(true));
        server.clearCachingFiles();
        assertThat(testTimeCacheRepo().getFile().exists(), is(false));

        server.testClassTime("quux/bang/Boom.class", 18l);
        verify(delegate).processedTestClassTime("quux/bang/Boom.class", 15l);
        assertThat(testTimeCacheRepo().getFile().exists(), is(true));
        verify(delegate, new Times(2)).fetchLastRunTestTimes();
    }

    @Test
    public void shouldPassOnClearFilesCall() {
        server.clearCachingFiles();
        verify(delegate).clearOtherCachingFiles();
    }
    
    @Test
    public void shouldAssumeNoOpSmoothingFactorWhenNotGiven() throws NoSuchFieldException, IllegalAccessException {
        when(delegate.fetchLastRunTestTimes()).thenReturn(Arrays.asList(new SuiteTimeEntry("foo/bar/Baz.class", 12l)));
        updateEnv(env, TlbConstants.TLB_SMOOTHING_FACTOR, null);
        server.testClassTime("foo/bar/Baz.class", 102l);
        verify(delegate).processedTestClassTime("foo/bar/Baz.class", 102l);
    }

    private static class DelegatingSmoothingServer extends SmoothingServer {

        private final SmoothingServer delegate;

        public DelegatingSmoothingServer(SmoothingServer delegate, final SystemEnvironment env) {
            super(env);
            this.delegate = delegate;
        }

        @Override
        public List<SuiteTimeEntry> fetchLastRunTestTimes() {
            return delegate.fetchLastRunTestTimes();
        }

        @Override
        public void processedTestClassTime(String className, long time) {
            delegate.processedTestClassTime(className, time);
        }

        public void testClassFailure(String className, boolean hasFailed) {
            delegate.testClassFailure(className, hasFailed);
        }

        public List<SuiteResultEntry> getLastRunFailedTests() {
            return delegate.getLastRunFailedTests();
        }

        public void publishSubsetSize(int size) {
            delegate.publishSubsetSize(size);
        }

        public void clearOtherCachingFiles() {
            delegate.clearOtherCachingFiles();
        }

        public int partitionNumber() {
            return delegate.partitionNumber();
        }

        public int totalPartitions() {
            return delegate.totalPartitions();
        }
    }
}

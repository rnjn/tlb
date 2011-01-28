package tlb.service;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.Component;
import tlb.TestUtil;
import tlb.TlbConstants;
import tlb.domain.SuiteResultEntry;
import tlb.domain.SuiteTimeEntry;
import tlb.server.ServerInitializer;
import tlb.server.TlbServerInitializer;
import tlb.service.http.DefaultHttpAction;
import tlb.utils.SystemEnvironment;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;
import static tlb.TestUtil.updateEnv;

/**
 * This in addition to being a talk-to-tlb-server test is also a tlb-server integration test,
 * hence uses real instance of tlb server
 */
public class TalkToTlbServerTest {
    private static Component component;
    private TalkToTlbServer talkToTlb;
    private static String freePort;
    private HashMap<String,String> clientEnv;
    private DefaultHttpAction httpAction;
    private SystemEnvironment env;

    @BeforeClass
    public static void startTlbServer() throws Exception {
        HashMap<String, String> serverEnv = new HashMap<String, String>();
        serverEnv.put(TlbConstants.SMOOTHING_FACTOR, "0.1");
        freePort = TestUtil.findFreePort();
        serverEnv.put(TlbConstants.Server.TLB_PORT, freePort);
        serverEnv.put(TlbConstants.Server.TLB_STORE_DIR, TestUtil.createTempFolder().getAbsolutePath());
        ServerInitializer main = new TlbServerInitializer(new SystemEnvironment(serverEnv));
        component = main.init();
        component.start();
    }

    @AfterClass
    public static void shutDownTlbServer() throws Exception {
        component.stop();
    }

    @Before
    public void setUp() {
        clientEnv = new HashMap<String, String>();
        clientEnv.put(TlbConstants.TlbServer.JOB_NAMESPACE, "job");
        clientEnv.put(TlbConstants.TlbServer.PARTITION_NUMBER, "4");
        clientEnv.put(TlbConstants.TlbServer.TOTAL_PARTITIONS, "15");
        clientEnv.put(TlbConstants.TlbServer.JOB_VERSION, String.valueOf(UUID.randomUUID()));
        String url = "http://localhost:" + freePort;
        clientEnv.put(TlbConstants.TlbServer.URL, url);
        httpAction = new DefaultHttpAction();
        env = new SystemEnvironment(clientEnv);
        talkToTlb = new TalkToTlbServer(env, httpAction);
        talkToTlb.clearCachingFiles();
    }

    @Test
    public void shouldBeAbleToPostSubsetSize() throws NoSuchFieldException, IllegalAccessException {
        talkToTlb.publishSubsetSize(10);
        talkToTlb.publishSubsetSize(20);
        talkToTlb.publishSubsetSize(17);
        assertThat(httpAction.get(String.format("http://localhost:%s/job-4/subset_size", freePort)), is("10\n20\n17\n"));
        updateEnv(env, TlbConstants.TlbServer.PARTITION_NUMBER, "5");
        talkToTlb.publishSubsetSize(12);
        talkToTlb.publishSubsetSize(13);
        assertThat(httpAction.get(String.format("http://localhost:%s/job-5/subset_size", freePort)), is("12\n13\n"));
    }

    @Test
    public void shouldBeAbleToPostSuiteTime() throws NoSuchFieldException, IllegalAccessException {
        talkToTlb.testClassTime("com.foo.Foo", 100);
        talkToTlb.testClassTime("com.bar.Bar", 120);
        updateEnv(env, TlbConstants.TlbServer.PARTITION_NUMBER, "2");
        talkToTlb.testClassTime("com.baz.Baz", 15);
        updateEnv(env, TlbConstants.TlbServer.PARTITION_NUMBER, "15");
        talkToTlb.testClassTime("com.quux.Quux", 137);
        final String response = httpAction.get(String.format("http://localhost:%s/job/suite_time", freePort));
        final List<SuiteTimeEntry> entryList = SuiteTimeEntry.parse(response);
        assertThat(entryList.size(), is(4));
        assertThat(entryList, hasItem(new SuiteTimeEntry("com.foo.Foo", 100)));
        assertThat(entryList, hasItem(new SuiteTimeEntry("com.bar.Bar", 120)));
        assertThat(entryList, hasItem(new SuiteTimeEntry("com.baz.Baz", 15)));
        assertThat(entryList, hasItem(new SuiteTimeEntry("com.quux.Quux", 137)));
    }

    @Test
    public void shouldBeAbleToPostSmoothenedSuiteTimeToRepo() throws NoSuchFieldException, IllegalAccessException {
        updateEnv(env, TlbConstants.TlbServer.JOB_NAMESPACE, "foo-job");
        updateEnv(env, TlbConstants.SMOOTHING_FACTOR, "0.5");
        String suiteTimeUrl = String.format("http://localhost:%s/foo-job/suite_time", freePort);
        httpAction.put(suiteTimeUrl, "com.foo.Foo: 100");
        httpAction.put(suiteTimeUrl, "com.bar.Bar: 120");
        httpAction.put(suiteTimeUrl, "com.quux.Quux: 20");

        updateEnv(env, TlbConstants.TlbServer.PARTITION_NUMBER, "4");
        talkToTlb.testClassTime("com.foo.Foo", 200);
        talkToTlb.testClassTime("com.bar.Bar", 160);
        updateEnv(env, TlbConstants.TlbServer.PARTITION_NUMBER, "2");
        talkToTlb.testClassTime("com.baz.Baz", 15);
        updateEnv(env, TlbConstants.TlbServer.PARTITION_NUMBER, "15");
        talkToTlb.testClassTime("com.quux.Quux", 160);
        String response = httpAction.get(suiteTimeUrl);
        List<SuiteTimeEntry> entryList = SuiteTimeEntry.parse(response);
        assertThat(entryList.size(), is(4));
        assertThat(entryList, hasItem(new SuiteTimeEntry("com.foo.Foo", 150)));
        assertThat(entryList, hasItem(new SuiteTimeEntry("com.bar.Bar", 140)));
        assertThat(entryList, hasItem(new SuiteTimeEntry("com.baz.Baz", 15)));
        assertThat(entryList, hasItem(new SuiteTimeEntry("com.quux.Quux", 90)));
    }

    @Test
    public void shouldBeAbleToPostSuiteResult() throws NoSuchFieldException, IllegalAccessException {
        talkToTlb.testClassFailure("com.foo.Foo", true);
        talkToTlb.testClassFailure("com.bar.Bar", false);
        updateEnv(env, TlbConstants.TlbServer.PARTITION_NUMBER, "2");
        talkToTlb.testClassFailure("com.baz.Baz", true);
        updateEnv(env, TlbConstants.TlbServer.PARTITION_NUMBER, "15");
        talkToTlb.testClassFailure("com.quux.Quux", true);
        final String response = httpAction.get(String.format("http://localhost:%s/job/suite_result", freePort));
        final List<SuiteResultEntry> entryList = SuiteResultEntry.parse(response);
        assertThat(entryList.size(), is(4));
        assertThat(entryList, hasItem(new SuiteResultEntry("com.foo.Foo", true)));
        assertThat(entryList, hasItem(new SuiteResultEntry("com.bar.Bar", false)));
        assertThat(entryList, hasItem(new SuiteResultEntry("com.baz.Baz", true)));
        assertThat(entryList, hasItem(new SuiteResultEntry("com.quux.Quux", true)));
    }

    @Test
    public void shouldBeAbleToFetchSuiteTimesUnaffectedByFurtherUpdates() throws NoSuchFieldException, IllegalAccessException {
        final String url = String.format("http://localhost:%s/job/suite_time", freePort);
        httpAction.put(url, "com.foo.Foo: 10");
        httpAction.put(url, "com.bar.Bar: 12");
        httpAction.put(url, "com.baz.Baz: 17");
        httpAction.put(url, "com.quux.Quux: 150");

        List<SuiteTimeEntry> entryList = talkToTlb.getLastRunTestTimes();
        assertThat(entryList.size(), is(4));
        assertThat(entryList, hasItem(new SuiteTimeEntry("com.foo.Foo", 10)));
        assertThat(entryList, hasItem(new SuiteTimeEntry("com.bar.Bar", 12)));
        assertThat(entryList, hasItem(new SuiteTimeEntry("com.baz.Baz", 17)));
        assertThat(entryList, hasItem(new SuiteTimeEntry("com.quux.Quux", 150)));

        talkToTlb = new TalkToTlbServer(env, httpAction);
        httpAction.put(url, "com.foo.Foo: 18");
        httpAction.put(url, "com.foo.Bang: 103");

        entryList = talkToTlb.getLastRunTestTimes();
        assertThat(entryList.size(), is(4));
        assertThat(entryList, hasItem(new SuiteTimeEntry("com.foo.Foo", 10)));
        assertThat(entryList, hasItem(new SuiteTimeEntry("com.bar.Bar", 12)));
        assertThat(entryList, hasItem(new SuiteTimeEntry("com.baz.Baz", 17)));
        assertThat(entryList, hasItem(new SuiteTimeEntry("com.quux.Quux", 150)));

        updateEnv(env, TlbConstants.TlbServer.PARTITION_NUMBER, "2");
        talkToTlb = new TalkToTlbServer(env, httpAction);
        entryList = talkToTlb.getLastRunTestTimes();
        assertThat(entryList.size(), is(4));
        assertThat(entryList, hasItem(new SuiteTimeEntry("com.foo.Foo", 10)));
        assertThat(entryList, hasItem(new SuiteTimeEntry("com.bar.Bar", 12)));
        assertThat(entryList, hasItem(new SuiteTimeEntry("com.baz.Baz", 17)));
        assertThat(entryList, hasItem(new SuiteTimeEntry("com.quux.Quux", 150)));

        //should fetch latest for unknown version
        updateEnv(env, TlbConstants.TlbServer.JOB_VERSION, "bar");
        talkToTlb = new TalkToTlbServer(env, httpAction);
        entryList = talkToTlb.getLastRunTestTimes();
        assertThat(entryList.size(), is(5));
        assertThat(entryList, hasItem(new SuiteTimeEntry("com.foo.Foo", 18)));
        assertThat(entryList, hasItem(new SuiteTimeEntry("com.bar.Bar", 12)));
        assertThat(entryList, hasItem(new SuiteTimeEntry("com.baz.Baz", 17)));
        assertThat(entryList, hasItem(new SuiteTimeEntry("com.quux.Quux", 150)));
        assertThat(entryList, hasItem(new SuiteTimeEntry("com.foo.Bang", 103)));
    }

    @Test
    public void shouldBeAbleToFetchSuiteResults() throws NoSuchFieldException, IllegalAccessException {
        final String url = String.format("http://localhost:%s/job/suite_result", freePort);
        httpAction.put(url, "com.foo.Foo: true");
        httpAction.put(url, "com.bar.Bar: false");
        httpAction.put(url, "com.baz.Baz: false");
        httpAction.put(url, "com.quux.Quux: true");

        List<SuiteResultEntry> entryList = talkToTlb.getLastRunFailedTests();
        assertThat(entryList.size(), is(4));
        assertThat(entryList, hasItem(new SuiteResultEntry("com.foo.Foo", true)));
        assertThat(entryList, hasItem(new SuiteResultEntry("com.bar.Bar", false)));
        assertThat(entryList, hasItem(new SuiteResultEntry("com.baz.Baz", false)));
        assertThat(entryList, hasItem(new SuiteResultEntry("com.quux.Quux", true)));

        updateEnv(env, TlbConstants.TlbServer.PARTITION_NUMBER, "2");
        entryList = talkToTlb.getLastRunFailedTests();
        assertThat(entryList.size(), is(4));
        assertThat(entryList, hasItem(new SuiteResultEntry("com.foo.Foo", true)));
        assertThat(entryList, hasItem(new SuiteResultEntry("com.bar.Bar", false)));
        assertThat(entryList, hasItem(new SuiteResultEntry("com.baz.Baz", false)));
        assertThat(entryList, hasItem(new SuiteResultEntry("com.quux.Quux", true)));
    }
    
    @Test
    public void shouldReadTotalPartitionsFromEnvironmentVariables() throws NoSuchFieldException, IllegalAccessException {
        assertThat(talkToTlb.totalPartitions(), is(15));
        updateEnv(env, TlbConstants.TlbServer.TOTAL_PARTITIONS, "7");
        assertThat(talkToTlb.totalPartitions(), is(7));
    }
}

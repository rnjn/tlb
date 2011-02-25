package tlb.service;

import org.hamcrest.core.Is;
import org.junit.*;
import org.junit.matchers.JUnitMatchers;
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
public class TlbServerTest {
    private static Component component;
    private TlbServer server;
    private static String freePort;
    private HashMap<String,String> clientEnv;
    private DefaultHttpAction httpAction;
    private SystemEnvironment env;

    @BeforeClass
    public static void startTlbServer() throws Exception {
        HashMap<String, String> serverEnv = new HashMap<String, String>();
        serverEnv.put(TlbConstants.TLB_SMOOTHING_FACTOR, "0.1");
        freePort = TestUtil.findFreePort();
        serverEnv.put(TlbConstants.Server.TLB_SERVER_PORT, freePort);
        serverEnv.put(TlbConstants.Server.TLB_DATA_DIR, TestUtil.createTempFolder().getAbsolutePath());
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
        clientEnv.put(TlbConstants.TlbServer.TLB_JOB_NAME, "job");
        clientEnv.put(TlbConstants.TlbServer.TLB_PARTITION_NUMBER, "4");
        clientEnv.put(TlbConstants.TlbServer.TLB_TOTAL_PARTITIONS, "15");
        clientEnv.put(TlbConstants.TlbServer.TLB_JOB_VERSION, String.valueOf(UUID.randomUUID()));
        String url = "http://localhost:" + freePort;
        clientEnv.put(TlbConstants.TlbServer.TLB_BASE_URL, url);
        httpAction = new DefaultHttpAction();
        env = new SystemEnvironment(clientEnv);
        server = new TlbServer(env, httpAction);
        server.clearCachingFiles();
    }

    @Test
    public void shouldBeAbleToPostSubsetSize() throws NoSuchFieldException, IllegalAccessException {
        server.publishSubsetSize(10);
        server.publishSubsetSize(20);
        server.publishSubsetSize(17);
        Assert.assertThat(httpAction.get(String.format("http://localhost:%s/job-4/subset_size", freePort)), Is.is("10\n20\n17\n"));
        updateEnv(env, TlbConstants.TlbServer.TLB_PARTITION_NUMBER, "5");
        server.publishSubsetSize(12);
        server.publishSubsetSize(13);
        Assert.assertThat(httpAction.get(String.format("http://localhost:%s/job-5/subset_size", freePort)), Is.is("12\n13\n"));
    }

    @Test
    public void shouldBeAbleToPostSuiteTime() throws NoSuchFieldException, IllegalAccessException {
        server.testClassTime("com.foo.Foo", 100);
        server.testClassTime("com.bar.Bar", 120);
        updateEnv(env, TlbConstants.TlbServer.TLB_PARTITION_NUMBER, "2");
        server.testClassTime("com.baz.Baz", 15);
        updateEnv(env, TlbConstants.TlbServer.TLB_PARTITION_NUMBER, "15");
        server.testClassTime("com.quux.Quux", 137);
        final String response = httpAction.get(String.format("http://localhost:%s/job/suite_time", freePort));
        final List<SuiteTimeEntry> entryList = SuiteTimeEntry.parse(response);
        Assert.assertThat(entryList.size(), Is.is(4));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteTimeEntry("com.foo.Foo", 100)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteTimeEntry("com.bar.Bar", 120)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteTimeEntry("com.baz.Baz", 15)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteTimeEntry("com.quux.Quux", 137)));
    }

    @Test
    public void shouldBeAbleToPostSmoothenedSuiteTimeToRepo() throws NoSuchFieldException, IllegalAccessException {
        updateEnv(env, TlbConstants.TlbServer.TLB_JOB_NAME, "foo-job");
        updateEnv(env, TlbConstants.TLB_SMOOTHING_FACTOR, "0.5");
        String suiteTimeUrl = String.format("http://localhost:%s/foo-job/suite_time", freePort);
        httpAction.put(suiteTimeUrl, "com.foo.Foo: 100");
        httpAction.put(suiteTimeUrl, "com.bar.Bar: 120");
        httpAction.put(suiteTimeUrl, "com.quux.Quux: 20");

        updateEnv(env, TlbConstants.TlbServer.TLB_PARTITION_NUMBER, "4");
        server.testClassTime("com.foo.Foo", 200);
        server.testClassTime("com.bar.Bar", 160);
        updateEnv(env, TlbConstants.TlbServer.TLB_PARTITION_NUMBER, "2");
        server.testClassTime("com.baz.Baz", 15);
        updateEnv(env, TlbConstants.TlbServer.TLB_PARTITION_NUMBER, "15");
        server.testClassTime("com.quux.Quux", 160);
        String response = httpAction.get(suiteTimeUrl);
        List<SuiteTimeEntry> entryList = SuiteTimeEntry.parse(response);
        Assert.assertThat(entryList.size(), Is.is(4));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteTimeEntry("com.foo.Foo", 150)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteTimeEntry("com.bar.Bar", 140)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteTimeEntry("com.baz.Baz", 15)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteTimeEntry("com.quux.Quux", 90)));
    }

    @Test
    public void shouldBeAbleToPostSuiteResult() throws NoSuchFieldException, IllegalAccessException {
        server.testClassFailure("com.foo.Foo", true);
        server.testClassFailure("com.bar.Bar", false);
        updateEnv(env, TlbConstants.TlbServer.TLB_PARTITION_NUMBER, "2");
        server.testClassFailure("com.baz.Baz", true);
        updateEnv(env, TlbConstants.TlbServer.TLB_PARTITION_NUMBER, "15");
        server.testClassFailure("com.quux.Quux", true);
        final String response = httpAction.get(String.format("http://localhost:%s/job/suite_result", freePort));
        final List<SuiteResultEntry> entryList = SuiteResultEntry.parse(response);
        Assert.assertThat(entryList.size(), Is.is(4));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteResultEntry("com.foo.Foo", true)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteResultEntry("com.bar.Bar", false)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteResultEntry("com.baz.Baz", true)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteResultEntry("com.quux.Quux", true)));
    }

    @Test
    public void shouldBeAbleToFetchSuiteTimesUnaffectedByFurtherUpdates() throws NoSuchFieldException, IllegalAccessException {
        final String url = String.format("http://localhost:%s/job/suite_time", freePort);
        httpAction.put(url, "com.foo.Foo: 10");
        httpAction.put(url, "com.bar.Bar: 12");
        httpAction.put(url, "com.baz.Baz: 17");
        httpAction.put(url, "com.quux.Quux: 150");

        List<SuiteTimeEntry> entryList = server.getLastRunTestTimes();
        Assert.assertThat(entryList.size(), Is.is(4));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteTimeEntry("com.foo.Foo", 10)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteTimeEntry("com.bar.Bar", 12)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteTimeEntry("com.baz.Baz", 17)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteTimeEntry("com.quux.Quux", 150)));

        server = new TlbServer(env, httpAction);
        httpAction.put(url, "com.foo.Foo: 18");
        httpAction.put(url, "com.foo.Bang: 103");

        entryList = server.getLastRunTestTimes();
        Assert.assertThat(entryList.size(), Is.is(4));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteTimeEntry("com.foo.Foo", 10)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteTimeEntry("com.bar.Bar", 12)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteTimeEntry("com.baz.Baz", 17)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteTimeEntry("com.quux.Quux", 150)));

        updateEnv(env, TlbConstants.TlbServer.TLB_PARTITION_NUMBER, "2");
        server = new TlbServer(env, httpAction);
        entryList = server.getLastRunTestTimes();
        Assert.assertThat(entryList.size(), Is.is(4));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteTimeEntry("com.foo.Foo", 10)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteTimeEntry("com.bar.Bar", 12)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteTimeEntry("com.baz.Baz", 17)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteTimeEntry("com.quux.Quux", 150)));

        //should fetch latest for unknown version
        updateEnv(env, TlbConstants.TlbServer.TLB_JOB_VERSION, "bar");
        server = new TlbServer(env, httpAction);
        entryList = server.getLastRunTestTimes();
        Assert.assertThat(entryList.size(), Is.is(5));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteTimeEntry("com.foo.Foo", 18)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteTimeEntry("com.bar.Bar", 12)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteTimeEntry("com.baz.Baz", 17)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteTimeEntry("com.quux.Quux", 150)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteTimeEntry("com.foo.Bang", 103)));
    }

    @Test
    public void shouldBeAbleToFetchSuiteResults() throws NoSuchFieldException, IllegalAccessException {
        final String url = String.format("http://localhost:%s/job/suite_result", freePort);
        httpAction.put(url, "com.foo.Foo: true");
        httpAction.put(url, "com.bar.Bar: false");
        httpAction.put(url, "com.baz.Baz: false");
        httpAction.put(url, "com.quux.Quux: true");

        List<SuiteResultEntry> entryList = server.getLastRunFailedTests();
        Assert.assertThat(entryList.size(), Is.is(4));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteResultEntry("com.foo.Foo", true)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteResultEntry("com.bar.Bar", false)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteResultEntry("com.baz.Baz", false)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteResultEntry("com.quux.Quux", true)));

        updateEnv(env, TlbConstants.TlbServer.TLB_PARTITION_NUMBER, "2");
        entryList = server.getLastRunFailedTests();
        Assert.assertThat(entryList.size(), Is.is(4));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteResultEntry("com.foo.Foo", true)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteResultEntry("com.bar.Bar", false)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteResultEntry("com.baz.Baz", false)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteResultEntry("com.quux.Quux", true)));
    }
    
    @Test
    public void shouldReadTotalPartitionsFromEnvironmentVariables() throws NoSuchFieldException, IllegalAccessException {
        Assert.assertThat(server.totalPartitions(), Is.is(15));
        updateEnv(env, TlbConstants.TlbServer.TLB_TOTAL_PARTITIONS, "7");
        Assert.assertThat(server.totalPartitions(), Is.is(7));
    }
}

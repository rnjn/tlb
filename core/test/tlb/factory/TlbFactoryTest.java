package tlb.factory;

import org.hamcrest.core.Is;
import org.junit.Test;
import tlb.TlbConstants;
import tlb.TlbSuiteFile;
import tlb.balancer.BalancerInitializer;
import tlb.orderer.FailedFirstOrderer;
import tlb.orderer.TestOrderer;
import tlb.server.ServerInitializer;
import tlb.server.TlbServerInitializer;
import tlb.service.Server;
import tlb.service.GoServer;
import tlb.service.TalksToServer;
import tlb.service.TlbServer;
import tlb.splitter.*;
import tlb.utils.SystemEnvironment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class TlbFactoryTest {

    @Test
    public void shouldReturnDefaultMatchAllCriteriaForEmpty() {
        TestSplitter criteria = TlbFactory.getCriteria(null, env("tlb.service.GoServer"));
        assertThat(criteria, Is.is(JobFamilyAwareSplitter.MATCH_ALL_FILE_SET));
        criteria = TlbFactory.getCriteria("", env("tlb.service.GoServer"));
        assertThat(criteria, is(JobFamilyAwareSplitter.MATCH_ALL_FILE_SET));
    }
    
    @Test
    public void shouldReturnNoOPOrdererForEmpty() {
        TestOrderer orderer = TlbFactory.getOrderer(null, env("tlb.service.GoServer"));
        assertThat(orderer, Is.is(TestOrderer.NO_OP));
        orderer = TlbFactory.getOrderer("", env("tlb.service.GoServer"));
        assertThat(orderer, is(TestOrderer.NO_OP));
    }

    @Test
    public void shouldThrowAnExceptionWhenTheCriteriaClassIsNotFound() {
        try {
            TlbFactory.getCriteria("com.thoughtworks.cruise.tlb.MissingCriteria", env("tlb.service.GoServer"));
            fail("should not be able to create random criteria!");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Unable to locate class 'com.thoughtworks.cruise.tlb.MissingCriteria'"));
        }

        try {
            TlbFactory.getOrderer("com.thoughtworks.cruise.tlb.MissingOrderer", env("tlb.service.GoServer"));
            fail("should not be able to create random orderer!");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Unable to locate class 'com.thoughtworks.cruise.tlb.MissingOrderer'"));
        }
    }

    @Test
    public void shouldThrowExceptionIfClassByGivenNameNotAssignableToInterfaceFactoryIsCreatedWith() {
        final TlbFactory<ServerInitializer> factory = new TlbFactory<ServerInitializer>(ServerInitializer.class, null);
        try {
            factory.getInstance("tlb.balancer.TlbClient", new SystemEnvironment());
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Class 'tlb.balancer.TlbClient' is-not/does-not-implement 'class tlb.server.ServerInitializer'"));
        }
    }
    
    @Test
    public void shouldThrowAnExceptionWhenTheCriteriaClassDoesNotImplementTestSplitterCriteria() {
        try {
            TlbFactory.getCriteria("java.lang.String", env("tlb.service.GoServer"));
            fail("should not be able to create criteria that doesn't implement TestSplitter");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Class 'java.lang.String' is-not/does-not-implement 'class tlb.splitter.TestSplitter'"));
        }
    }

    @Test
    public void shouldReturnCountBasedCriteria() {
        TestSplitter criteria = TlbFactory.getCriteria("tlb.splitter.CountBasedTestSplitter", env("tlb.service.GoServer"));
        assertThat(criteria, instanceOf(CountBasedTestSplitter.class));
    }
    
    @Test
    public void shouldInjectTlbCommunicatorWhenImplementsTalkToService() {
        TlbFactory<TestSplitter> criteriaFactory = new TlbFactory<TestSplitter>(TestSplitter.class, JobFamilyAwareSplitter.MATCH_ALL_FILE_SET);
        TestSplitter criteria = criteriaFactory.getInstance(MockSplitter.class, env("tlb.service.TlbServer"));
        assertThat(criteria, instanceOf(MockSplitter.class));
        assertThat(((MockSplitter)criteria).calledTalksToService, is(true));
        assertThat(((MockSplitter)criteria).talker, is(TlbServer.class));
    }

    @Test
    public void shouldInjectCruiseCommunicatorWhenImplementsTalkToService() {
        TlbFactory<TestSplitter> criteriaFactory = new TlbFactory<TestSplitter>(TestSplitter.class, JobFamilyAwareSplitter.MATCH_ALL_FILE_SET);
        TestSplitter criteria = criteriaFactory.getInstance(MockSplitter.class, env("tlb.service.GoServer"));
        assertThat(criteria, instanceOf(MockSplitter.class));
        assertThat(((MockSplitter)criteria).calledTalksToService, is(true));
        assertThat(((MockSplitter)criteria).talker, is(GoServer.class));
    }

    @Test
    public void shouldReturnTimeBasedCriteria() {
        TestSplitter criteria = TlbFactory.getCriteria("tlb.splitter.TimeBasedTestSplitter", env("tlb.service.GoServer"));
        assertThat(criteria, instanceOf(TimeBasedTestSplitter.class));
    }

    @Test
    public void shouldReturnFailedFirstOrderer() {
        TestOrderer failedTestsFirstOrderer = TlbFactory.getOrderer("tlb.orderer.FailedFirstOrderer", env("tlb.service.GoServer"));
        assertThat(failedTestsFirstOrderer, instanceOf(FailedFirstOrderer.class));
    }

    @Test
    public void shouldReturnTalkToTlbServer() {
        final Map<String, String> map = new HashMap<String, String>();
        map.put(TlbConstants.TlbServer.TLB_BASE_URL, "http://localhost:7019");
        map.put(TlbConstants.TYPE_OF_SERVER, "tlb.service.TlbServer");
        Server server = TlbFactory.getTalkToService(new SystemEnvironment(map));
        assertThat(server, is(TlbServer.class));
    }
    
    @Test
    public void shouldReturnTalkToCruise() {
        final Map<String, String> map = new HashMap<String, String>();
        map.put(TlbConstants.Go.GO_SERVER_URL, "http://localhost:8153/cruise");
        map.put(TlbConstants.TYPE_OF_SERVER, "tlb.service.GoServer");
        Server server = TlbFactory.getTalkToService(new SystemEnvironment(map));
        assertThat(server, is(GoServer.class));
    }

    @Test
    public void shouldReturnTlbServerRestletLauncher() {
        ServerInitializer launcher = TlbFactory.getRestletLauncher("tlb.server.TlbServerInitializer", new SystemEnvironment(new HashMap<String, String>()));
        assertThat(launcher, is(TlbServerInitializer.class));
    }

    @Test
    public void shouldReturnBalancerRestletLauncher() {
        ServerInitializer launcher = TlbFactory.getRestletLauncher("tlb.balancer.BalancerInitializer", new SystemEnvironment(new HashMap<String, String>()));
        assertThat(launcher, is(BalancerInitializer.class));
    }
    
    @Test
    public void shouldReturnTlbServerRestletLauncherIfNoneGiven() {
        ServerInitializer launcher = TlbFactory.getRestletLauncher(null, new SystemEnvironment(new HashMap<String, String>()));
        assertThat(launcher, is(TlbServerInitializer.class));
    }

    private SystemEnvironment env(String talkToService) {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(TlbConstants.Go.GO_SERVER_URL, "https://localhost:8154/cruise");
        map.put(TlbConstants.TlbServer.TLB_BASE_URL, "http://localhost:7019");
        map.put(TlbConstants.TYPE_OF_SERVER, talkToService);
        return new SystemEnvironment(map);
    }

    private static class MockSplitter extends JobFamilyAwareSplitter implements TalksToServer {
        private boolean calledFilter = false;
        private boolean calledTalksToService = false;
        private Server talker;

        public MockSplitter(SystemEnvironment env) {
            super(env);
        }

        protected List<TlbSuiteFile> subset(List<TlbSuiteFile> fileResources) {
            this.calledFilter = true;
            return null;
        }

        public void talksToServer(Server service) {
            this.calledTalksToService = true;
            this.talker = service;
        }
    }
}

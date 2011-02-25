package tlb.factory;

import tlb.orderer.TestOrderer;
import tlb.splitter.JobFamilyAwareSplitter;
import tlb.splitter.TestSplitter;
import tlb.utils.SystemEnvironment;

/**
 * @understands creating balancer specific algorithm implementations
 */
public class TlbBalancerFactory {
    private static TlbFactory<TestSplitter> criteriaFactory;
    private static TlbFactory<TestOrderer> testOrderer;

    public static TestSplitter getCriteria(String criteriaName, SystemEnvironment environment) {
        if (criteriaFactory == null)
            criteriaFactory = new TlbFactory<TestSplitter>(TestSplitter.class, JobFamilyAwareSplitter.MATCH_ALL_FILE_SET);
        return criteriaFactory.getInstance(criteriaName, environment);
    }

    public static TestOrderer getOrderer(String ordererName, SystemEnvironment environment) {
        if (testOrderer == null)
            testOrderer = new TlbFactory<TestOrderer>(TestOrderer.class, TestOrderer.NO_OP);
        return testOrderer.getInstance(ordererName, environment);
    }
}

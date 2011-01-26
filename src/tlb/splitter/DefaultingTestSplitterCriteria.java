package tlb.splitter;

import org.apache.log4j.Logger;
import tlb.TlbConstants;
import tlb.TlbSuiteFile;
import tlb.factory.TlbFactory;
import tlb.utils.SystemEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;


/**
 * @understands choosing criteria in order of preference
 */
public class DefaultingTestSplitterCriteria extends TestSplitterCriteria {
    private static final Logger logger = Logger.getLogger(DefaultingTestSplitterCriteria.class.getName());

    private ArrayList<TestSplitterCriteria> criterion;

    public DefaultingTestSplitterCriteria(SystemEnvironment env) {
        super(env);
        criterion = new ArrayList<TestSplitterCriteria>();
        String[] criteriaNames = criteriaNames(env);
        for (String criteriaName : criteriaNames) {
            TestSplitterCriteria splitterCriteria = TlbFactory.getCriteria(criteriaName, env);
            criterion.add(splitterCriteria);
        }
    }

    @Override
    public List<TlbSuiteFile> filterSuites(List<TlbSuiteFile> fileResources) {
        for (TestSplitterCriteria criteria : criterion) {
            try {
                List<TlbSuiteFile> subset = criteria.filterSuites(fileResources);
                logger.info(String.format("Used %s to balance.", criteria.getClass().getCanonicalName()));
                return subset;
            } catch (Exception e) {
                logger.warn(String.format("Could not use %s for balancing because: %s.", criteria.getClass().getCanonicalName(), e.getMessage()), e);
                continue;
            }
        }
        throw new IllegalStateException(String.format("None of %s could successfully split the test suites.", Arrays.asList(criteriaNames(env))));
    }

    private String[] criteriaNames(SystemEnvironment env) {
        return env.val(TlbConstants.CRITERIA_DEFAULTING_ORDER).split("\\s*:\\s*");
    }
}

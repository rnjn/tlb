package tlb;

import org.junit.Test;
import tlb.balancer.BalancerInitializer;
import tlb.utils.SystemEnvironment;

import java.util.Collections;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @understands
 */
public class MainBalancerTest {
    @Test
    public void shouldCreateServerInitializerWhenTlbAppSetToBalancer() {
        final Main main = new Main();
        assertThat(main.restletInitializer(new SystemEnvironment(Collections.singletonMap(TlbConstants.TLB_APP, "tlb.balancer.BalancerInitializer"))), is(BalancerInitializer.class));
    }
}

package tlb;

import org.junit.Test;
import tlb.balancer.BalancerInitializer;
import tlb.utils.SystemEnvironment;

import java.util.Collections;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class BalancerMainTest {
    @Test
    public void shouldCreateBalancerInitializerWhenTlbAppSetToTlbServer() {
        final Main main = new Main();
        assertThat(main.restletInitializer(new SystemEnvironment(Collections.singletonMap(TlbConstants.TLB_APP, "tlb.balancer.BalancerInitializer"))), is(BalancerInitializer.class));
    }
}

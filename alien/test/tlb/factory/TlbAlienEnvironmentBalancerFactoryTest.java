package tlb.factory;

import org.junit.Test;
import tlb.balancer.BalancerInitializer;
import tlb.server.ServerInitializer;
import tlb.utils.SystemEnvironment;

import java.util.HashMap;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @understands
 */
public class TlbAlienEnvironmentBalancerFactoryTest {
    @Test
    public void shouldReturnBalancerRestletLauncher() {
        ServerInitializer launcher = TlbServerFactory.getRestletLauncher("tlb.balancer.BalancerInitializer", new SystemEnvironment(new HashMap<String, String>()));
        assertThat(launcher, is(BalancerInitializer.class));
    }
}

package tlb.factory;

import org.junit.Test;
import tlb.server.ServerInitializer;
import tlb.server.TlbServerInitializer;
import tlb.utils.SystemEnvironment;

import java.util.HashMap;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

/**
 * @understands
 */
public class TlbServerFactoryTest {
    @Test
    public void shouldReturnTlbServerRestletLauncher() {
        ServerInitializer launcher = TlbServerFactory.getRestletLauncher("tlb.server.TlbServerInitializer", new SystemEnvironment(new HashMap<String, String>()));
        assertThat(launcher, is(TlbServerInitializer.class));
    }

    @Test
    public void shouldReturnTlbServerRestletLauncherIfNoneGiven() {
        ServerInitializer launcher = TlbServerFactory.getRestletLauncher(null, new SystemEnvironment(new HashMap<String, String>()));
        assertThat(launcher, is(nullValue()));
    }

}

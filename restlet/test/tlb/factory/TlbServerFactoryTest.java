package tlb.factory;

import org.junit.Test;
import tlb.server.ServerInitializer;
import tlb.utils.SystemEnvironment;

import java.util.HashMap;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class TlbServerFactoryTest {

    @Test
    public void shouldReturnTlbServerRestletLauncher() {
        ServerInitializer launcher = TlbServerFactory.getRestletLauncher(TestInitializer.class.getCanonicalName(), new SystemEnvironment(new HashMap<String, String>()));
        assertThat(launcher, is(TestInitializer.class));
    }

    @Test
    public void shouldReturnTlbServerRestletLauncherIfNoneGiven() {
        ServerInitializer launcher = TlbServerFactory.getRestletLauncher(null, new SystemEnvironment(new HashMap<String, String>()));
        assertThat(launcher, is(nullValue()));
    }

}

package tlb;

import tlb.server.TlbServerInitializer;
import tlb.utils.SystemEnvironment;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class MainTest {
    @Test
    public void shouldCreateServerInitializer() {
        final Main main = new Main();
        assertThat(main.restletInitializer(new SystemEnvironment(new HashMap<String, String>())), is(TlbServerInitializer.class));
    }

    @Test
    public void shouldCreateServerInitializerWhenTlbAppSetToTlbServer() {
        final Main main = new Main();
        assertThat(main.restletInitializer(new SystemEnvironment(Collections.singletonMap(TlbConstants.TLB_APP, "tlb.server.TlbServerInitializer"))), is(TlbServerInitializer.class));
    }

}

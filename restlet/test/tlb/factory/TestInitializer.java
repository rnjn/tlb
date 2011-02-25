package tlb.factory;

import org.restlet.Restlet;
import tlb.server.ServerInitializer;
import tlb.utils.SystemEnvironment;

public class TestInitializer extends ServerInitializer {

    public TestInitializer(SystemEnvironment env) {
    }

    @Override
    protected Restlet application() {
        return null;
    }

    @Override
    protected int appPort() {
        return 0;
    }
}

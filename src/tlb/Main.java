package tlb;

import org.apache.log4j.Logger;
import tlb.factory.TlbFactory;
import tlb.server.ServerInitializer;
import tlb.utils.SystemEnvironment;

/**
 * @understands launching a restlet server
 */
public class Main {
    public static void main(String[] args) {
        final Main main = new Main();
        try {
            main.restletInitializer(new SystemEnvironment()).init().start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    ServerInitializer restletInitializer(SystemEnvironment environment) {
        return TlbFactory.getRestletLauncher(environment.val(TlbConstants.TLB_APP), environment);
    }
}

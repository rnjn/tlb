package tlb;

import tlb.factory.TlbServerFactory;
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
        //TODO: we are using 2 different jars for server and balancer. no need to take a ServerInitializer through TLB_APP anymore, just default it in both cases.
        return TlbServerFactory.getRestletLauncher(environment.val(TlbConstants.TLB_APP, TlbConstants.Server.DEFAULT_SERVER_INITIALIZER), environment);
    }
}

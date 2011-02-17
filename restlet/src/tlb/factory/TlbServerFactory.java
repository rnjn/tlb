package tlb.factory;

import tlb.server.ServerInitializer;
import tlb.utils.SystemEnvironment;

/**
 * @understands
 */
public class TlbServerFactory {
    private static TlbFactory<ServerInitializer> restletLauncherFactory;

    public static ServerInitializer getRestletLauncher(String restletLauncherName, SystemEnvironment environment) {
        if (restletLauncherFactory == null)
            restletLauncherFactory = new TlbFactory<ServerInitializer>(ServerInitializer.class, null);
        return restletLauncherFactory.getInstance(restletLauncherName, environment);
    }

}

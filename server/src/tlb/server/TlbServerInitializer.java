package tlb.server;

import tlb.TlbConstants;
import tlb.server.repo.EntryRepoFactory;
import tlb.utils.SystemEnvironment;
import org.restlet.Context;
import org.restlet.Restlet;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @understands running the server as a standalone process
 */
public class TlbServerInitializer extends ServerInitializer {
    private final SystemEnvironment env;
    private final Timer timer;

    public TlbServerInitializer(SystemEnvironment env) {
        this(env, new Timer());
    }

    public TlbServerInitializer(SystemEnvironment env, Timer timer) {
        this.env = env;
        this.timer = timer;
    }

    protected Restlet application() {
        Context applicationContext = new Context();
        initializeApplicationContext(applicationContext);
        return new TlbApplication(applicationContext);
    }

    public void initializeApplicationContext(Context applicationContext) {
        HashMap<String, Object> appMap = new HashMap<String, Object>();
        final EntryRepoFactory repoFactory = repoFactory();

        setupTimerForPurgingOlderVersions(repoFactory);

        repoFactory.registerExitHook();
        appMap.put(TlbConstants.Server.REPO_FACTORY, repoFactory);
        applicationContext.setAttributes(appMap);
    }

    private void setupTimerForPurgingOlderVersions(final EntryRepoFactory repoFactory) {
        final int versionLifeInDays = versionInLife();
        if (versionLifeInDays == -1) {
            return;
        }
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                repoFactory.purgeVersionsOlderThan(versionLifeInDays);
            }
        }, 0, 1*24*60*60*1000);
    }

    private int versionInLife() {
        return Integer.parseInt(env.val(TlbConstants.Server.TLB_VERSION_LIFE_IN_DAYS));
    }

    @Override
    protected int appPort()  {
        return Integer.parseInt(env.val(TlbConstants.Server.TLB_SERVER_PORT));
    }

    EntryRepoFactory repoFactory() {
        return new EntryRepoFactory(env);
    }
}

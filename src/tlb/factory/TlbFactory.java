package tlb.factory;

import tlb.TlbConstants;
import tlb.server.ServerInitializer;
import tlb.server.TlbServerInitializer;
import tlb.service.Server;
import tlb.service.TalksToServer;
import tlb.splitter.JobFamilyAwareSplitter;
import tlb.splitter.TestSplitter;
import tlb.orderer.TestOrderer;
import tlb.utils.SystemEnvironment;

import java.lang.reflect.InvocationTargetException;

/**
 * @understands creating a criteria based on the class
 */
public class TlbFactory<T> {
    private Class<T> klass;
    private T defaultValue;
    private static TlbFactory<TestSplitter> criteriaFactory;
    private static TlbFactory<TestOrderer> testOrderer;
    private static TlbFactory<Server> talkToServiceFactory;
    private static TlbFactory<ServerInitializer> restletLauncherFactory;

    TlbFactory(Class<T> klass, T defaultValue) {
        this.klass = klass;
        this.defaultValue = defaultValue;
    }

    public <T> T getInstance(String klassName, SystemEnvironment environment) {
        if (klassName == null || klassName.isEmpty()) {
            return (T) defaultValue;
        }
        try {
            Class<?> criteriaClass = Class.forName(klassName);
            if(!klass.isAssignableFrom(criteriaClass)) {
                throw new IllegalArgumentException("Class '" + klassName + "' is-not/does-not-implement '" + klass + "'");
            }
            return getInstance((Class<? extends T>) criteriaClass, environment);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Unable to locate class '" + klassName + "'");
        }
    }

    <T> T getInstance(Class<? extends T> actualKlass, SystemEnvironment environment) {
        try {
            T criteria = actualKlass.getConstructor(SystemEnvironment.class).newInstance(environment);
            if (TalksToServer.class.isInstance(criteria)) {
                Server service = getTalkToService(environment);
                ((TalksToServer)criteria).talksToServer(service);
            }
            return criteria;
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Public constructor matching " + actualKlass.getName() + "(SystemEnvironment) was not found", e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Public constructor matching " + actualKlass.getName() + "(SystemEnvironment) was not found", e);
        } catch (InstantiationException e) {
            throw new IllegalArgumentException("Unable to create abstract class " + actualKlass.getName(), e);
        }
    }

    public static TestSplitter getCriteria(String criteriaName, SystemEnvironment environment) {
        if (criteriaFactory == null)
            criteriaFactory = new TlbFactory<TestSplitter>(TestSplitter.class, JobFamilyAwareSplitter.MATCH_ALL_FILE_SET);
        return criteriaFactory.getInstance(criteriaName, environment);
    }

    public static TestOrderer getOrderer(String ordererName, SystemEnvironment environment) {
        if (testOrderer == null)
            testOrderer = new TlbFactory<TestOrderer>(TestOrderer.class, TestOrderer.NO_OP);
        return testOrderer.getInstance(ordererName, environment);
    }

    public static Server getTalkToService(SystemEnvironment environment) {
        if (talkToServiceFactory == null)
            talkToServiceFactory = new TlbFactory<Server>(Server.class, null);
        return talkToServiceFactory.getInstance(environment.val(TlbConstants.TYPE_OF_SERVER), environment);
    }

    public static ServerInitializer getRestletLauncher(String restletLauncherName, SystemEnvironment environment) {
        if (restletLauncherFactory == null)
            restletLauncherFactory = new TlbFactory<ServerInitializer>(ServerInitializer.class, new TlbServerInitializer(environment));
        return restletLauncherFactory.getInstance(restletLauncherName, environment);
    }
}

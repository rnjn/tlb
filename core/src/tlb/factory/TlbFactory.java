package tlb.factory;

import org.apache.log4j.Logger;
import tlb.TlbConstants;
import tlb.service.Server;
import tlb.service.TalksToServer;
import tlb.utils.SystemEnvironment;

import java.lang.reflect.InvocationTargetException;

/**
 * @understands creating a criteria based on the class
 */
public class TlbFactory<T> {
    private Class<T> klass;
    private T defaultValue;

    private static TlbFactory<Server> talkToServiceFactory;

    private static final Logger LOGGER = Logger.getLogger(TlbFactory.class);

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
            LOGGER.fatal(getInstanceExceptionMessage(actualKlass, environment, e), e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            LOGGER.fatal(getInstanceExceptionMessage(actualKlass, environment, e), e);
            throw new IllegalArgumentException(e);
        }
    }

    private static String getInstanceExceptionMessage(Class actualKlass, SystemEnvironment environment, Exception e) {
        return String.format("Message: %s, Args: %s, %s", e.getMessage(), actualKlass, environment);
    }

    public static Server getTalkToService(SystemEnvironment environment) {
        if (talkToServiceFactory == null)
            talkToServiceFactory = new TlbFactory<Server>(Server.class, null);
        return talkToServiceFactory.getInstance(environment.val(new SystemEnvironment.EnvVar(TlbConstants.TYPE_OF_SERVER)), environment);
    }

}

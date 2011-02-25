package tlb.domain;

import java.util.GregorianCalendar;

/**
 * @understands system time
 */
public class TimeProvider {
    public GregorianCalendar now() {
        //NOTE: Do not cache, mutating methods while trying to find out if needs to purge old versions will dirty it if cached. -janmejay
        return new GregorianCalendar();
    }
}

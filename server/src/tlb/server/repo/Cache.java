package tlb.server.repo;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

/**
 * @understands caching against a string key
 */
public class Cache<T> {
    public static final CacheManager CACHE_MANAGER = CacheManager.create();
    private final net.sf.ehcache.Cache cache;

    public Cache(final String cacheName) {
        cache = new net.sf.ehcache.Cache(
                new CacheConfiguration(cacheName, 1000)
                        .memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LRU)
                        .overflowToDisk(false)
                        .eternal(true)
                        .diskPersistent(false));
        CACHE_MANAGER.addCache(cache);
    }

    public void put(final String key, final T value) {
        cache.put(new Element(key, value));
    }

    public T get(final String key) {
        Element element = cache.get(key);
        if (element == null) {
            return null;
        }
        return (T) element.getValue();
    }
}

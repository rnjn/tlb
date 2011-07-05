package tlb.server.repo;

import org.junit.Test;

import java.io.Serializable;

import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;

public class CacheTest {
    final class TestObject implements Serializable {}

    @Test
    public void shouldLoadObjectsPutIntoCache() {
        Cache<TestObject> testObjectCache = new Cache<TestObject>("testCache");
        TestObject cachedObject = new TestObject();
        testObjectCache.put("foo", cachedObject);
        assertThat(testObjectCache.get("foo"), sameInstance(cachedObject));
    }
}

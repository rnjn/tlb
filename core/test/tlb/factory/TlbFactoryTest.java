package tlb.factory;

import org.junit.Test;
import tlb.utils.SystemEnvironment;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class TlbFactoryTest {
    public abstract class FooBarable {

    }

    @Test
    public void shouldThrowExceptionIfClassByGivenNameNotAssignableToInterfaceFactoryIsCreatedWith() {
        final TlbFactory<FooBarable> factory = new TlbFactory<FooBarable>(FooBarable.class, null);
        try {
            factory.getInstance("java.lang.String", new SystemEnvironment());
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Class 'java.lang.String' is-not/does-not-implement 'class tlb.factory.TlbFactoryTest$FooBarable'"));
        }
    }

}

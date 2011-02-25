package tlb.server;

import org.junit.Before;
import org.junit.Test;
import org.restlet.Context;
import org.restlet.Finder;
import org.restlet.Restlet;
import tlb.server.resources.*;

import java.util.HashMap;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;
import static org.mockito.Mockito.mock;
import static tlb.RestletTestUtil.getRoutePatternsAndResources;

public class TlbApplicationTest {
    private TlbApplication app;

    @Before
    public void setUp() {
        Context context = mock(Context.class);
        app = new TlbApplication(context);
    }

    @Test
    public void shouldHaveRouteForSubsetSize() {
        HashMap<String, Restlet> routeMaping = getRoutePatternsAndResources(app);
        assertThat(routeMaping.keySet(), hasItem("/{namespace}/subset_size"));
        Restlet restlet = routeMaping.get("/{namespace}/subset_size");
        assertThat(((Finder)restlet).getTargetClass().getName(), is(SubsetSizeResource.class.getName()));
    }

    @Test
    public void shouldHaveRouteForSuiteTime() {
        HashMap<String, Restlet> routeMaping = getRoutePatternsAndResources(app);
        assertThat(routeMaping.keySet(), hasItem("/{namespace}/suite_time"));
        Restlet restlet = routeMaping.get("/{namespace}/suite_time");
        assertThat(((Finder)restlet).getTargetClass().getName(), is(SuiteTimeResource.class.getName()));
    }
    
    @Test
    public void shouldHaveRouteForVersionedSuiteTime() {
        HashMap<String, Restlet> routeMaping = getRoutePatternsAndResources(app);
        assertThat(routeMaping.keySet(), hasItem("/{namespace}/suite_time/{listing_version}"));
        Restlet restlet = routeMaping.get("/{namespace}/suite_time/{listing_version}");
        assertThat(((Finder)restlet).getTargetClass().getName(), is(VersionedSuiteTimeResource.class.getName()));
    }

    @Test
    public void shouldHaveRouteForSuiteResult() {
        HashMap<String, Restlet> routeMaping = getRoutePatternsAndResources(app);
        assertThat(routeMaping.keySet(), hasItem("/{namespace}/suite_result"));
        Restlet restlet = routeMaping.get("/{namespace}/suite_result");
        assertThat(((Finder)restlet).getTargetClass().getName(), is(SuiteResultResource.class.getName()));
    }
}

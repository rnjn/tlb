package tlb.server.resources;

import org.junit.Before;
import org.junit.Test;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import tlb.TlbConstants;
import tlb.server.repo.EntryRepo;
import tlb.server.repo.EntryRepoFactory;
import tlb.server.repo.SuiteTimeRepo;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VersionedSuiteTimeResourceTest {
    private VersionedSuiteTimeResource suiteTimeResource;
    protected HashMap<String, Object> attributeMap;
    protected EntryRepoFactory factory;
    protected SuiteTimeRepo repo;

    @Before
    public void setUp() throws ClassNotFoundException, IOException {
        Context context = new Context();
        Request request = mock(Request.class);
        factory = mock(EntryRepoFactory.class);
        context.setAttributes(Collections.singletonMap(TlbConstants.Server.REPO_FACTORY, (Object) factory));
        attributeMap = new HashMap<String, Object>();
        attributeMap.put(TlbConstants.Server.REQUEST_NAMESPACE, "namespace");
        when(request.getAttributes()).thenReturn(attributeMap);
        repo = mock(SuiteTimeRepo.class);
        when(factory.createSuiteTimeRepo("namespace", EntryRepoFactory.LATEST_VERSION)).thenReturn(repo);
        suiteTimeResource = new VersionedSuiteTimeResource(context, request, mock(Response.class));
    }

    @Test
    public void shouldUseSuiteTimeRepo() throws IOException, ClassNotFoundException {
        EntryRepo repo = suiteTimeResource.getRepo(factory, "namespace");
        assertThat((SuiteTimeRepo) repo, sameInstance(this.repo));
    }
}

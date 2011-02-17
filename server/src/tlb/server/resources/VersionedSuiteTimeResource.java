package tlb.server.resources;

import tlb.server.repo.EntryRepo;
import tlb.server.repo.EntryRepoFactory;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;

import java.io.IOException;

/**
 * @understands versioned run time of suite reported by job
 */
public class VersionedSuiteTimeResource extends VersionedResource {
    public VersionedSuiteTimeResource(Context context, Request request, Response response) {
        super(context, request, response);
    }

    @Override
    protected EntryRepo getRepo(EntryRepoFactory repoFactory, String key) throws ClassNotFoundException, IOException {
        return repoFactory.createSuiteTimeRepo(key, EntryRepoFactory.LATEST_VERSION);
    }
}
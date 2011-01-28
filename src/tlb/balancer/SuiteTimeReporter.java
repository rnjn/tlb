package tlb.balancer;

import org.apache.log4j.Logger;
import tlb.domain.SuiteTimeEntry;
import tlb.service.TalkToService;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;


/**
 * @understands posting the suite time entry to server
 */
public class SuiteTimeReporter extends Resource {
    private static final Logger logger = Logger.getLogger(SuiteTimeReporter.class.getName());

    protected TalkToService talkToService;

    public SuiteTimeReporter(Context context, Request request, Response response) {
        super(context, request, response);
        getVariants().add(new Variant(MediaType.TEXT_PLAIN));
        talkToService = (TalkToService) context.getAttributes().get(TlbClient.TALK_TO_SERVICE);
    }

    @Override
    public void acceptRepresentation(Representation entity) throws ResourceException {
        try {
            final List<SuiteTimeEntry> entries = SuiteTimeEntry.parse(entity.getText());
            for (SuiteTimeEntry entry : entries) {
                talkToService.testClassTime(entry.getName(), entry.getTime());
            }
        } catch (IOException e) {
            logger.warn(String.format("could not report test time: '%s'", e.getMessage()), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean allowGet() {
        return false;
    }

    @Override
    public boolean allowPost() {
        return true;//always allowing post as we don't know what kind of support for HTTP PUT requests exists in libraries in other languages, POST is more widely accepted
    }
}

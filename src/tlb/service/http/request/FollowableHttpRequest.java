package tlb.service.http.request;

import org.apache.commons.httpclient.HttpMethodBase;

import java.io.IOException;


import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.log4j.Logger;
import tlb.utils.RetryAfter;
import tlb.service.http.DefaultHttpAction;

/**
 * @understands error ressilient http request handling
 */
public abstract class FollowableHttpRequest {
    private DefaultHttpAction defaultHttpAction;
    private RetryAfter retryer;
    private static final Logger logger = Logger.getLogger(FollowableHttpRequest.class.getName());

    public FollowableHttpRequest(DefaultHttpAction defaultHttpAction) {
        this(defaultHttpAction, new RetryAfter(RetryAfter.seq(10, 8*6)));
    }

    FollowableHttpRequest(DefaultHttpAction defaultHttpAction, RetryAfter retryer) {
        this.defaultHttpAction = defaultHttpAction;
        this.retryer = retryer;
    }

    public RetryAfter getRetryer() {
        return retryer;
    }

    public abstract HttpMethodBase createMethod(String url);

    public String executeRequest(String url) {
        String pathAndQuery = null;
        URI uri = null;
        try {
            uri = new URI(url, false);
            pathAndQuery = uri.getPathQuery();
        } catch (URIException e1) {
            throw new RuntimeException(e1);
        }

        final HttpMethodBase method = createMethod(pathAndQuery);
        
        String baseMessage = String.format("http request to %s with %s", url, method.getClass().getSimpleName());
        logger.info("attempting " + baseMessage);
        try {
            final URI finalUri = uri;
            int result = retryer.tryFn(new RetryAfter.Fn<Integer>() {
                public Integer fn() throws Exception {
                    return defaultHttpAction.executeMethod(method, finalUri);
                }
            });
            logger.info(baseMessage + " returned " + result);
            if (result >= 300 && result < 400) {
                return executeRequest(method.getResponseHeader("Location").getValue());
            }
            if (result < 200 || result >= 300) {
                throw new RuntimeException(String.format("Something went horribly wrong. Bu Hao. The response[status: %s] looks like: %s", result, method.getResponseBodyAsString()));
            }
            return method.getResponseBodyAsString();
        } catch (IOException e) {
            throw new RuntimeException("Oops! Something went wrong", e);
        }
    }

}

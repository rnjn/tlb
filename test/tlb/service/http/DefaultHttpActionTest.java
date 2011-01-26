package tlb.service.http;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import tlb.HttpTestUtil;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@RunWith(Theories.class)
public class DefaultHttpActionTest {
    public static final int HTTP_PORT = 2080;
    public static final int HTTPS_PORT = 2443;

    public static final int HTTP_PORT_OTHER = 3080;
    public static final int HTTPS_PORT_OTHER = 3443;

    private static final DefaultHttpAction action = new DefaultHttpAction(new HttpClient());

    private static HttpTestUtil httpTestUtil;

    @BeforeClass
    public static void beforeAll() throws InterruptedException {
        httpTestUtil = new HttpTestUtil();
        httpTestUtil.httpConnector(HTTP_PORT);
        httpTestUtil.httpConnector(HTTP_PORT_OTHER);
        httpTestUtil.httpsConnector(HTTPS_PORT);
        httpTestUtil.httpsConnector(HTTPS_PORT_OTHER);
        httpTestUtil.start();
    }

    @AfterClass
    public static void afterAll() {
        httpTestUtil.stop();
    }

    private static interface DoAction {
        void act(DefaultHttpAction action, URI url, final String pathQueryExpected, final String paramsString) throws URIException;
    }

    private static class GetAction implements DoAction {
        public void act(DefaultHttpAction action, URI url, final String pathQueryExpected, final String paramsString) throws URIException {
            String responseBody = action.get(url.toString());
            assertThat(responseBody.startsWith(String.format("GET(%s): %s", url.getPort(), pathQueryExpected)), is(true));
            assertThat(responseBody.contains(paramsString), is(true));
        }
    }

    private static class PostActionWithMap implements DoAction {
        public void act(DefaultHttpAction action, URI url, final String pathQueryExpected, final String paramsString) throws URIException {
            Map<String, String> payload = new HashMap<String, String>();
            payload.put("foo", "bar");
            payload.put("hello", "world");
            String responseBody = action.post(url.toString(), payload);
            assertThat(responseBody.startsWith(String.format("POST(%s): %s", url.getPort(), pathQueryExpected)), is(true));
            assertThat(responseBody.contains(paramsString), is(true));
            assertThat(responseBody.contains("foo=[bar]"), is(true));
            assertThat(responseBody.contains("hello=[world]"), is(true));
        }
    }

    private static class PostActionWithString implements DoAction {
        public void act(DefaultHttpAction action, URI url, final String pathQueryExpected, final String paramsString) throws URIException {
            String responseBody = action.post(url.toString(), "some_random_data");
            assertThat(responseBody.startsWith(String.format("POST(%s): %s", url.getPort(), pathQueryExpected)), is(true));
            assertThat(responseBody.contains(paramsString), is(true));
            assertThat(responseBody.contains("[some_random_data]"), is(true));
        }
    }

    private static class PutActionWithString implements DoAction {
        public void act(DefaultHttpAction action, URI url, final String pathQueryExpected, final String paramsString) throws URIException {
            String responseBody = action.put(url.toString(), "some_random_data");
            assertThat(responseBody.startsWith(String.format("PUT(%s): %s", url.getPort(), pathQueryExpected)), is(true));
            assertThat(responseBody.contains(paramsString), is(true));
            assertThat(responseBody.contains("[some_random_data]"), is(true));
        }
    }


    private static interface UriProvider {
        URI uri();
    }

    private static class HttpUriProvider implements UriProvider {
        public URI uri() {
            try {
                return new URI("http://localhost:" + HTTP_PORT, true);
            } catch (URIException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class HttpsUriProvider implements UriProvider {
        private String localhost;
        private int port;

        public HttpsUriProvider(final int port) {
            localhost = "localhost";
            this.port = port;
        }

        public URI uri() {
            try {
                return new URI("https://" + localhost + ":" + port, true);
            } catch (URIException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static final class Action {
        private final DoAction doAction;
        private URI uri;

        Action(UriProvider getUrl, DoAction doAction) {
            this.doAction = doAction;
            uri = getUrl.uri();
        }

        public void act(String pathQuery, final String paramsString) throws URIException {
            URI callUri = new URI(uri, pathQuery);
            doAction.act(action, callUri, pathQuery, paramsString);
        }
    }


    @DataPoint public static final Action httpGet = new Action(new HttpUriProvider(), new GetAction());
    @DataPoint public static final Action httpsGet = new Action(new HttpsUriProvider(HTTPS_PORT), new GetAction());
    @DataPoint public static final Action httpPostWithMap = new Action(new HttpUriProvider(), new PostActionWithMap());
    @DataPoint public static final Action httpsPostWithMap = new Action(new HttpsUriProvider(HTTPS_PORT), new PostActionWithMap());
    @DataPoint public static final Action httpPostWithString = new Action(new HttpUriProvider(), new PostActionWithString());
    @DataPoint public static final Action httpsPostWithString = new Action(new HttpsUriProvider(HTTPS_PORT), new PostActionWithString());
    @DataPoint public static final Action httpPutWithString = new Action(new HttpUriProvider(), new PutActionWithString());
    @DataPoint public static final Action httpsPutWithString = new Action(new HttpsUriProvider(HTTPS_PORT), new PutActionWithString());

    @DataPoint public static final Action httpsGetFromIp = new Action(new HttpsUriProvider(HTTPS_PORT_OTHER), new GetAction());

    @Theory
    public void shouldTalkHttpWell(Action action) throws URIException {
        action.act("/foo/bar?baz=quux", "baz=[quux]");
    }
}

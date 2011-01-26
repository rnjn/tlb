package tlb.service.http;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.log4j.Logger;
import tlb.service.http.request.FollowableHttpRequest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;


/**
 * @understands talking http
 */
public class DefaultHttpAction implements HttpAction {
    private final HttpClient client;
    private static final Logger logger = Logger.getLogger(DefaultHttpAction.class.getName());
    private final Map<HostPortCombination,Protocol> hostCfgProtocolMap;

    private static class HostPortCombination {
        private final String host;
        private final int port;

        private HostPortCombination(String host, int port) {
            this.host = host;
            this.port = port;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            HostPortCombination that = (HostPortCombination) o;

            if (port != that.port) return false;
            if (host != null ? !host.equals(that.host) : that.host != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = host != null ? host.hashCode() : 0;
            result = 31 * result + port;
            return result;
        }
    }

    public DefaultHttpAction(HttpClient client) {
        this.client = client;
        this.hostCfgProtocolMap = new HashMap<HostPortCombination, Protocol>();
    }

    private void ensureProtocolRegistered(URI url) {
        try {
            Protocol protocol = protocol(url);
            this.client.getHostConfiguration().setHost(url.getHost(), url.getPort(), protocol);
        } catch (URIException e) {
            throw new RuntimeException(e);
        }
    }

    private Protocol protocol(URI url) throws URIException {
        String host = url.getHost();
        int port = url.getPort();
        HostPortCombination hostPort = new HostPortCombination(host, port);
        Protocol protocol = hostCfgProtocolMap.get(hostPort);
        if (protocol == null) {
            synchronized (hostCfgProtocolMap) {
                protocol = url.getScheme().equals("https") ? new Protocol("https", (ProtocolSocketFactory) new PermissiveSSLProtocolSocketFactory(), url.getPort()) : Protocol.getProtocol("http");
                hostCfgProtocolMap.put(hostPort, protocol);
            }
        }
        return protocol;
    }

    public synchronized int executeMethod(HttpMethodBase method, URI uri) {
        try {
            ensureProtocolRegistered(uri);
            logger.info(String.format("Executing http request with %s", method.getClass().getSimpleName()));
            return client.executeMethod(method);
        } catch (IOException e) {
            throw new RuntimeException("Oops! Something went wrong", e);
        }
    }

    class FollowableGetRequest extends FollowableHttpRequest {
        protected FollowableGetRequest(DefaultHttpAction action) {
            super(action);
        }

        public HttpMethodBase createMethod(String url) {
            return new GetMethod(url);
        }
    }

    class FollowablePutRequest extends FollowableHttpRequest {
        private String data;

        protected FollowablePutRequest(DefaultHttpAction action, String data) {
            super(action);
            this.data = data;
        }

        public HttpMethodBase createMethod(String url) {
            PutMethod method = new PutMethod(url);
            try {
                method.setRequestEntity(new StringRequestEntity(data, "text/plain", "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            return method;
        }
    }

    class FollowablePostRequest extends FollowableHttpRequest {
        private Map<String, String> data;

        protected FollowablePostRequest(DefaultHttpAction action, Map<String, String> data) {
            super(action);
            this.data = data;
        }

        public HttpMethodBase createMethod(String url) {
            PostMethod method = new PostMethod(url);
            for (Map.Entry<String, String> param : data.entrySet()) {
                method.addParameter(param.getKey(), param.getValue());
            }
            return method;
        }
    }

    class FollowableRawPostRequest extends FollowableHttpRequest {
        private String data;

        protected FollowableRawPostRequest(DefaultHttpAction action, String data) {
            super(action);
            this.data = data;
        }

        public HttpMethodBase createMethod(String url) {
            PostMethod method = new PostMethod(url);
            try {
                method.setRequestEntity(new StringRequestEntity(data, "text/plain", "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            return method;
        }
    }
    

    public String get(String url) {
        FollowableGetRequest request = new FollowableGetRequest(this);
        return request.executeRequest(url);
    }

    public String post(String url, Map<String,String> data) {
        FollowablePostRequest request = new FollowablePostRequest(this, data);
        return request.executeRequest(url);
    }

    public String post(String url, String data) {
        FollowableRawPostRequest request = new FollowableRawPostRequest(this, data);
        return request.executeRequest(url);
    }

    public String put(String url, String data) {
        FollowablePutRequest request = new FollowablePutRequest(this, data);
        return request.executeRequest(url);
    }
}

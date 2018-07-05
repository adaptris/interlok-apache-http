package com.adaptris.core.http.apache;

import static com.adaptris.core.AdaptrisMessageFactory.defaultIfNull;
import static org.apache.commons.lang.StringUtils.isBlank;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;

import javax.validation.Valid;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.client.SystemDefaultCredentialsProvider;
import org.apache.http.protocol.HttpContext;

import com.adaptris.annotation.AdapterComponent;
import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageProducerImp;
import com.adaptris.core.CoreException;
import com.adaptris.core.NullConnection;
import com.adaptris.core.ProduceDestination;
import com.adaptris.core.ProduceException;
import com.adaptris.core.http.ResourceAuthenticator.ResourceTarget;
import com.adaptris.core.http.auth.HttpAuthenticator;
import com.adaptris.core.http.auth.ResourceTargetMatcher;
import com.adaptris.core.util.ExceptionHelper;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Producer implementation that uses the Apache HTTP Client as the underlying transport.
 * 
 * @config apache-http-producer
 */
@XStreamAlias("apache-http-producer")
@AdapterComponent
@ComponentProfile(summary = "Make a HTTP(s) request to a remote server using the Apache HTTP Client", tag = "producer,http,https", metadata =
{
    "adphttpresponse"

}, recommended =
{
    NullConnection.class
}, author = "Adaptris Ltd")
@DisplayOrder(order =
{
    "username", "password", "authenticator", "httpProxy", "allowRedirect", "ignoreServerResponseCode", "methodProvider",
    "contentTypeProvider", "requestHeaderProvider", "responseHeaderHandler", "responseHandlerFactory"
})
public class ApacheHttpProducer extends HttpProducer {

  private static final ResponseHandlerFactory DEFAULT_HANDLER = new PayloadResponseHandlerFactory();

  @AdvancedConfig
  @Valid
  private ResponseHandlerFactory responseHandlerFactory;

  @AdvancedConfig
  private String httpProxy;


  public ApacheHttpProducer() {
    super();
  }

  public ApacheHttpProducer(ProduceDestination d) {
    this();
    setDestination(d);
  }


  public ResponseHandlerFactory getResponseHandlerFactory() {
    return responseHandlerFactory;
  }

  /**
   * Set how to handle the response.
   * 
   * @param fac the factory; default is {@link PayloadResponseHandlerFactory}.
   */
  public void setResponseHandlerFactory(ResponseHandlerFactory fac) {
    this.responseHandlerFactory = fac;
  }


  /**
   * @return the httpProxy
   */
  public String getHttpProxy() {
    return httpProxy;
  }

  /**
   * Explicitly configure a proxy server.
   * 
   * @param proxy the httpProxy to generally {@code scheme://host:port} or more simply {@code host:port}
   */
  public void setHttpProxy(String proxy) {
    this.httpProxy = proxy;
  }


  ResponseHandlerFactory responseHandlerFactory() {
    return getResponseHandlerFactory() != null ? getResponseHandlerFactory() : DEFAULT_HANDLER;
  }

  /**
   * @see AdaptrisMessageProducerImp #request(AdaptrisMessage, ProduceDestination, long)
   */
  @Override
  protected AdaptrisMessage doRequest(AdaptrisMessage msg, ProduceDestination destination, long timeout) throws ProduceException {
    AdaptrisMessage reply = defaultIfNull(getMessageFactory()).newMessage();

    try (HttpAuthenticator auth = authenticator()) {
      String uri = destination.getDestination(msg);
      HttpRequestBase httpOperation = getMethod(msg).create(uri);
      auth.setup(uri, msg, new ApacheResourceTargetMatcher(httpOperation.getURI()));
      try (CloseableHttpClient httpclient = createClient(timeout)) {
        if (auth instanceof ApacheRequestAuthenticator) {
          ((ApacheRequestAuthenticator) auth).configure(httpOperation);
        }
        addData(msg, getRequestHeaderProvider().addHeaders(msg, httpOperation));
        reply = httpclient.execute(httpOperation, responseHandlerFactory().createResponseHandler(this));
      }
      copyHeaders(msg, reply);
    } catch (Exception e) {
      throw ExceptionHelper.wrapProduceException(e);
    }
    return reply;
  }

  private CloseableHttpClient createClient(long timeout) throws Exception {
    HttpClientBuilder builder = customiseTimeouts(HttpClients.custom(), timeout);
    builder.addInterceptorLast(new HttpRequestInterceptor() {
      public void process(HttpRequest request, HttpContext context){
        request.removeHeaders("Accept-Encoding");
        request.removeHeaders("Connection");
        request.removeHeaders("User-Agent");
      }
    });
    return customise(builder).build();
  }

  protected HttpClientBuilder customiseTimeouts(HttpClientBuilder builder, long timeout) throws Exception {
    RequestConfig.Builder requestCfg = RequestConfig.custom();
    if (getConnectTimeout() != null) {
      requestCfg.setConnectTimeout(Long.valueOf(getConnectTimeout().toMilliseconds()).intValue());
    }
    if (getReadTimeout() != null) {
      requestCfg.setSocketTimeout(Long.valueOf(getReadTimeout().toMilliseconds()).intValue());
    }
    if (timeout != DEFAULT_TIMEOUT) {
      requestCfg.setSocketTimeout(Long.valueOf(timeout).intValue());
      builder.setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(Long.valueOf(timeout).intValue()).build());
    }
    return builder;
  }

  protected HttpClientBuilder customise(HttpClientBuilder builder) throws Exception {
    if (!handleRedirection()) {
      builder.disableRedirectHandling();
    } else {
      builder.setRedirectStrategy(new LaxRedirectStrategy());
    }
    String httpProxy = getHttpProxy();
    if (!isBlank(httpProxy) && !httpProxy.equals(":")) {
      builder.setProxy(HttpHost.create(httpProxy));
    }
    return builder.setDefaultCredentialsProvider(new SystemDefaultCredentialsProvider()).useSystemProperties();
  }
  
  private HttpRequestBase addData(AdaptrisMessage msg, HttpRequestBase base) throws IOException,
      CoreException {
    if (base instanceof HttpEntityEnclosingRequestBase) {
      AdaptrisMessageEntity entity = new AdaptrisMessageEntity(msg, getContentTypeProvider());
      ((HttpEntityEnclosingRequestBase) base).setEntity(entity);
    }
    return base;
  }

  private class ApacheResourceTargetMatcher implements ResourceTargetMatcher {

    private URI uri;
    private String host;
    private int port;

    ApacheResourceTargetMatcher(URI uri) {
      this.uri = uri;
      this.port = derivePort(uri);
      host = uri.getHost();
    }

    private int derivePort(URI uri) {
      int result = uri.getPort();
      if (result == -1) {
        if ("http".equalsIgnoreCase(uri.getScheme())) {
          result = 80;
        }
        if ("https".equalsIgnoreCase(uri.getScheme())) {
          result = 443;
        }
      }
      return result;
    }

    @Override
    public boolean matches(ResourceTarget target) {
      if (doMatch(target)) {
        log.trace("Matched authentication request for [{}://{}:{}].", target.getRequestingScheme(), target.getRequestingHost(),
            target.getRequestingPort());
        return true;
      }
      log.trace("Unmatched authentication request for [{}://{}:{}]. My target is [{}]", target.getRequestingScheme(),
          target.getRequestingHost(), target.getRequestingPort(), uri);
      return false;
    }

    private boolean doMatch(ResourceTarget target) {
      boolean rc = false;
      try {
        if (target.getRequestingURL() == null) {
          rc = host.equalsIgnoreCase(target.getRequestingHost()) && port == target.getRequestingPort();
        }
        else {
          rc = uri.toURL().equals(target.getRequestingURL());
        }
      }
      catch (MalformedURLException e) {

      }
      return rc;
    }
  }
}

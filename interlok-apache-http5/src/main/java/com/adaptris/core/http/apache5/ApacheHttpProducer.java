package com.adaptris.core.http.apache5;

import com.adaptris.annotation.AdapterComponent;
import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.NullConnection;
import com.adaptris.core.ProduceException;
import com.adaptris.core.http.ResourceAuthenticator.ResourceTarget;
import com.adaptris.core.http.auth.HttpAuthenticator;
import com.adaptris.core.http.auth.ResourceTargetMatcher;
import com.adaptris.core.util.ExceptionHelper;
import com.adaptris.interlok.util.Closer;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;

import javax.validation.Valid;
import java.net.MalformedURLException;
import java.net.URI;

/**
 * Producer implementation that uses the Apache HTTP Client as the underlying transport.
 *
 * @config apache-http-producer
 */
@XStreamAlias("apache-http5-producer")
@AdapterComponent
@ComponentProfile(summary = "Make a HTTP(s) request to a remote server using the Apache HTTP Client", tag = "producer,http,https", metadata =
{
    "adphttpresponse"

}, recommended =
{
    NullConnection.class
    }, author = "Adaptris Ltd", since = "4.1.0")
@DisplayOrder(order =
{
    "url", "authenticator", "ignoreServerResponseCode",
    "methodProvider", "contentTypeProvider", "requestHeaderProvider", "responseHeaderHandler", "responseHandlerFactory",
    "clientConfig"
})
@NoArgsConstructor
public class ApacheHttpProducer extends HttpProducer {

  private static final ResponseHandlerFactory DEFAULT_HANDLER = new PayloadResponseHandlerFactory();

  /**
   * How to handle the response from the HTTP Server.
   * <p>
   * If not explicitly configured then the response payload is stored as the payload of the
   * resulting {@link AdaptrisMessage}.
   * </p>
   */
  @AdvancedConfig
  @Valid
  @Getter
  @Setter
  private ResponseHandlerFactory responseHandlerFactory;

  /*
   * IMPORTANT: Always re-use CloseableHttpClient instances. They are
   * expensive to create, but they are also fully thread safe, so
   * multiple threads can use the same instance of CloseableHttpClient
   * to execute multiple requests concurrently taking full advantage of
   * persistent connection re-use and connection pooling.
   */
  private static transient CloseableHttpClient httpClient;

  protected ResponseHandlerFactory responseHandlerFactory() {
    return ObjectUtils.defaultIfNull(getResponseHandlerFactory(), DEFAULT_HANDLER);
  }

  @Override
  public synchronized void close() {
    Closer.closeQuietly(httpClient);
    httpClient = null;
    super.close();
  }

  @Override
  protected AdaptrisMessage doRequest(AdaptrisMessage msg, String uri, long timeout) throws ProduceException {
    try (HttpAuthenticator auth = authenticator())
    {
      HttpUriRequestBase httpOperation = getMethod(msg).create(uri);
      auth.setup(uri, msg, new ApacheResourceTargetMatcher(httpOperation.getUri()));
      log.trace("Attempting [{}] against [{}]", httpOperation.getMethod(), httpOperation.getUri());
      HttpClient httpClient = createClient(httpOperation, getClientConfig(), timeout);
      if (auth instanceof ApacheRequestAuthenticator)
      {
        ((ApacheRequestAuthenticator) auth).configure(httpOperation);
      }
      getRequestHeaderProvider().addHeaders(msg, httpOperation).setEntity(new AdaptrisMessageEntity(msg, getContentTypeProvider()));

      AdaptrisMessage reply = httpClient.execute(httpOperation, responseHandlerFactory().createResponseHandler(this));
      preserveRequestPayload(msg, reply);
      return reply;
    }
    catch (Exception e)
    {
      throw ExceptionHelper.wrapProduceException(e);
    }
  }

  private static synchronized CloseableHttpClient createClient(HttpUriRequestBase httpOperation, HttpClientBuilderConfigurator clientConfig, long timeout) throws Exception
  {
    if (httpClient == null)
    {
      httpClient = HttpClientBuilderConfigurator.defaultIfNull(clientConfig).configure(HttpClients.custom(), timeout).build();
    }
    else if (timeout > 0)
    {
      RequestConfig requestConfig = httpOperation.getConfig();
      RequestConfig.Builder builder = requestConfig != null ? RequestConfig.copy(requestConfig) : RequestConfig.custom();
      builder.setConnectionRequestTimeout(Timeout.ofMilliseconds(timeout));
      builder.setConnectTimeout(Timeout.ofMilliseconds(timeout));
      builder.setResponseTimeout(Timeout.ofMilliseconds(timeout));
      httpOperation.setConfig(requestConfig);
    }
    return httpClient;
  }

  protected class ApacheResourceTargetMatcher implements ResourceTargetMatcher {

    private URI uri;
    private String host;
    private int port;

    protected ApacheResourceTargetMatcher(URI uri) {
      this.uri = uri;
      port = derivePort(uri);
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
          rc = BooleanUtils.and(new boolean[]
          {
              host.equalsIgnoreCase(target.getRequestingHost()), port == target.getRequestingPort(),
              // Do we need to check the scheme ? would you have different logins
              // for https vs http ?
              // uri.getScheme().equalsIgnoreCase(target.getRequestingScheme())
          });
        }
        else {
          rc = uri.toURL().sameFile(target.getRequestingURL());
        }
      }
      catch (MalformedURLException e) {

      }
      return rc;
    }
  }
}

package com.adaptris.core.http.apache;

import static com.adaptris.core.AdaptrisMessageFactory.defaultIfNull;

import com.adaptris.annotation.AdapterComponent;
import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.CoreException;
import com.adaptris.core.NullConnection;
import com.adaptris.core.ProduceException;
import com.adaptris.core.http.ResourceAuthenticator.ResourceTarget;
import com.adaptris.core.http.auth.HttpAuthenticator;
import com.adaptris.core.http.auth.ResourceTargetMatcher;
import com.adaptris.core.util.ExceptionHelper;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import javax.validation.Valid;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;

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

  protected ResponseHandlerFactory responseHandlerFactory() {
    return ObjectUtils.defaultIfNull(getResponseHandlerFactory(), DEFAULT_HANDLER);
  }


  @Override
  protected AdaptrisMessage doRequest(AdaptrisMessage msg, String uri, long timeout)
      throws ProduceException {
    AdaptrisMessage reply = defaultIfNull(getMessageFactory()).newMessage();

    try (HttpAuthenticator auth = authenticator()) {
      HttpRequestBase httpOperation = getMethod(msg).create(uri);
      auth.setup(uri, msg, new ApacheResourceTargetMatcher(httpOperation.getURI()));
      log.trace("Attempting [{}] against [{}]", httpOperation.getMethod(), httpOperation.getURI());
      try (CloseableHttpClient httpclient = createClient(timeout)) {
        if (auth instanceof ApacheRequestAuthenticator) {
          ((ApacheRequestAuthenticator) auth).configure(httpOperation);
        }
        addData(msg, requestHeaderProvider().addHeaders(msg, httpOperation));
        reply =
            httpclient.execute(httpOperation, responseHandlerFactory().createResponseHandler(this));
        preserveRequestPayload(msg, reply);
      }
    } catch (Exception e) {
      throw ExceptionHelper.wrapProduceException(e);
    }
    return reply;
  }

  private CloseableHttpClient createClient(long timeout) throws Exception {
    HttpClientBuilder builder = customise(
        HttpClientBuilderConfigurator.defaultIfNull(getClientConfig())
            .configure(HttpClients.custom(), timeout));
    return builder.build();
  }

  /**
   * Do any further customisations.
   *
   * @param builder the builder
   * @return the builder.
   */
  protected HttpClientBuilder customise(HttpClientBuilder builder) throws Exception {
    return builder;
  }

  private HttpRequestBase addData(AdaptrisMessage msg, HttpRequestBase base) throws IOException,
      CoreException {
    if (base instanceof HttpEntityEnclosingRequestBase) {
      AdaptrisMessageEntity entity = new AdaptrisMessageEntity(msg, contentTypeProvider());
      ((HttpEntityEnclosingRequestBase) base).setEntity(entity);
    }
    return base;
  }

  protected class ApacheResourceTargetMatcher implements ResourceTargetMatcher {

    private final URI uri;
    private final String host;
    private final int port;

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
      catch (MalformedURLException ignored) {

      }
      return rc;
    }
  }


}

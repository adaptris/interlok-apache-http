package com.adaptris.core.http.apache;

import static com.adaptris.core.AdaptrisMessageFactory.defaultIfNull;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isEmpty;

import java.io.IOException;

import javax.validation.Valid;

import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.client.SystemDefaultCredentialsProvider;

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
import com.adaptris.core.http.auth.ConfiguredUsernamePassword;
import com.adaptris.core.http.auth.HttpAuthenticator;
import com.adaptris.core.http.auth.NoAuthentication;
import com.adaptris.core.util.Args;
import com.adaptris.core.util.ExceptionHelper;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Producer implementation that uses the Apache HTTP Client as the underlying transport.
 * 
 * @config apache-http-producer
 * @license BASIC
 */
@XStreamAlias("apache-http-producer")
@AdapterComponent
@ComponentProfile(summary = "Make a HTTP request to a remote server using the Apache HTTP Client", tag = "producer,http,https",
    recommended = {NullConnection.class})
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
  @Valid
  private HttpAuthenticator authenticator;

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
      auth.setup(uri, msg);
      try (CloseableHttpClient httpclient = createClient()) {
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

  private CloseableHttpClient createClient() {
    HttpClientBuilder builder = HttpClients.custom();
    if (!handleRedirection()) {
      builder.disableRedirectHandling();
    }
    else {
      builder.setRedirectStrategy(new LaxRedirectStrategy());
    }
    if (!isBlank(getHttpProxy())) {
      builder.setProxy(HttpHost.create(getHttpProxy()));
    }
    return builder.setDefaultCredentialsProvider(new SystemDefaultCredentialsProvider()).useSystemProperties().build();
  }

  
  private HttpRequestBase addData(AdaptrisMessage msg, HttpRequestBase base) throws IOException,
      CoreException {
    if (base instanceof HttpEntityEnclosingRequestBase) {
      AdaptrisMessageEntity entity = new AdaptrisMessageEntity(msg, getContentTypeProvider());
      ((HttpEntityEnclosingRequestBase) base).setEntity(entity);
    }
    return base;
  }

  public HttpAuthenticator getAuthenticator() {
    return authenticator;
  }

  /**
   * Set the authentication method to use for the HTTP request
   */
  public void setAuthenticator(HttpAuthenticator authenticator) {
    this.authenticator = Args.notNull(authenticator, "authenticator");
  }

  HttpAuthenticator authenticator() {
    HttpAuthenticator authToUse = getAuthenticator() != null ? getAuthenticator() : new NoAuthentication();
    // If deprecated username/password are set and no authenticator is configured, transparently create a static authenticator
    if (getAuthenticator() instanceof NoAuthentication && !isEmpty(getUserName())) {
      authToUse = new ConfiguredUsernamePassword(getUserName(), getPassword());
    }
    return authToUse;
  }
}

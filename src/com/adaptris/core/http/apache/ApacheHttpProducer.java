package com.adaptris.core.http.apache;

import static com.adaptris.core.AdaptrisMessageFactory.defaultIfNull;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;

import javax.validation.Valid;

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.SystemDefaultCredentialsProvider;
import org.perf4j.aop.Profiled;

import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.CoreException;
import com.adaptris.core.ProduceDestination;
import com.adaptris.core.ProduceException;
import com.adaptris.core.http.AdapterResourceAuthenticator;
import com.adaptris.core.http.ResourceAuthenticator;
import com.adaptris.core.util.ExceptionHelper;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Producer implementation that uses the Apache HTTP Client as the underlying transport.
 * 
 * @config apache-http-producer
 * @license BASIC
 */
@XStreamAlias("apache-http-producer")
public class ApacheHttpProducer extends HttpProducer {

  private static final ResponseHandlerFactory DEFAULT_HANDLER = new PayloadResponseHandlerFactory();

  @AdvancedConfig
  @Valid
  private ResponseHandlerFactory responseHandlerFactory;

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

  ResponseHandlerFactory responseHandlerFactory() {
    return getResponseHandlerFactory() != null ? getResponseHandlerFactory() : DEFAULT_HANDLER;
  }

  /**
   * @see AdaptrisMessageProducerImp #request(AdaptrisMessage, ProduceDestination, long)
   */
  @Override
  @Profiled(tag = "{$this.getClass().getSimpleName()}.request()", logger = "com.adaptris.perf4j.http.apache.TimingLogger")
  protected AdaptrisMessage doRequest(AdaptrisMessage msg, ProduceDestination destination, long timeout) throws ProduceException {
    AdaptrisMessage reply = defaultIfNull(getMessageFactory()).newMessage();
    HttpAuthenticator myAuth = null;
    try {
      String uri = destination.getDestination(msg);
      HttpRequestBase httpOperation = getMethod(msg).create(uri);
      if (getPasswordAuthentication() != null) {
        myAuth = new HttpAuthenticator(httpOperation.getURI(), getPasswordAuthentication());
        Authenticator.setDefault(AdapterResourceAuthenticator.getInstance());
        AdapterResourceAuthenticator.getInstance().addAuthenticator(myAuth);
      }
      try (CloseableHttpClient httpclient = createClient()) {
        addData(msg, getRequestHeaderProvider().addHeaders(msg, httpOperation));
        reply = httpclient.execute(httpOperation, responseHandlerFactory.createResponseHandler(this));
      }
      copyHeaders(msg, reply);
    } catch (Exception e) {
      ExceptionHelper.rethrowProduceException(e);
    } finally {
      AdapterResourceAuthenticator.getInstance().removeAuthenticator(myAuth);
    }
    return reply;
  }

  private CloseableHttpClient createClient() {
    CloseableHttpClient result = null;
    HttpClientBuilder builder = HttpClients.custom();
    if (!handleRedirection()) {
      builder.disableRedirectHandling();
    }
    result = builder.setDefaultCredentialsProvider(new SystemDefaultCredentialsProvider()).build();
    return result;
  }

  
  private HttpRequestBase addData(AdaptrisMessage msg, HttpRequestBase base) throws IOException,
      CoreException {
    if (base instanceof HttpEntityEnclosingRequestBase) {
      AdaptrisMessageEntity entity = new AdaptrisMessageEntity(msg, getContentTypeProvider());
      ((HttpEntityEnclosingRequestBase) base).setEntity(entity);
    }
    return base;
  }

  private class HttpAuthenticator implements ResourceAuthenticator {

    private URI uri;
    private String host;
    private int port;
    private PasswordAuthentication auth;

    HttpAuthenticator(URI uri, PasswordAuthentication auth) {
      this.uri = uri;
      this.auth = auth;
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
    public PasswordAuthentication authenticate(ResourceTarget target) {
      if (host.equalsIgnoreCase(target.getRequestingHost()) && port == target.getRequestingPort()) {
        log.trace("Using user={} to login to [{}://{}:{}]", auth.getUserName(), target.getRequestingScheme(),
            target.getRequestingHost(), target.getRequestingPort());
        return auth;
      }
      return null;
    }
  }

}

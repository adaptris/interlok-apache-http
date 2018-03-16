package com.adaptris.core.http.apache;

import static com.adaptris.core.http.HttpConstants.DEFAULT_SOCKET_TIMEOUT;
import static org.apache.commons.lang.StringUtils.isEmpty;

import java.io.IOException;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpTrace;

import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.annotation.AutoPopulated;
import com.adaptris.annotation.InputFieldDefault;
import com.adaptris.annotation.InputFieldHint;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.CoreConstants;
import com.adaptris.core.CoreException;
import com.adaptris.core.ProduceDestination;
import com.adaptris.core.ProduceException;
import com.adaptris.core.RequestReplyProducerImp;
import com.adaptris.core.http.ConfiguredContentTypeProvider;
import com.adaptris.core.http.ContentTypeProvider;
import com.adaptris.core.http.auth.ConfiguredUsernamePassword;
import com.adaptris.core.http.auth.HttpAuthenticator;
import com.adaptris.core.http.auth.MetadataUsernamePassword;
import com.adaptris.core.http.auth.NoAuthentication;
import com.adaptris.core.http.client.ConfiguredRequestMethodProvider;
import com.adaptris.core.http.client.RequestHeaderProvider;
import com.adaptris.core.http.client.RequestMethodProvider;
import com.adaptris.core.http.client.RequestMethodProvider.RequestMethod;
import com.adaptris.core.http.client.ResponseHeaderHandler;
import com.adaptris.core.util.Args;
import com.adaptris.security.password.Password;

/**
 * Abstract base class for all Apache HTTP producer classes.
 * 
 * @author lchan
 * 
 */
public abstract class HttpProducer extends RequestReplyProducerImp {
  /**
   * Maps various methods supported by the Apache Http client.
   * 
   */
  public static enum HttpMethod {
    DELETE {
      @Override
      public HttpRequestBase create(String url) {
        return new HttpDelete(url);
      }
    },
    GET {
      @Override
      public HttpRequestBase create(String url) {
        return new HttpGet(url);
      }
    },
    HEAD {
      @Override
      public HttpRequestBase create(String url) {
        return new HttpHead(url);
      }
    },
    OPTIONS {
      @Override
      public HttpRequestBase create(String url) {
        return new HttpOptions(url);
      }
    },
    PATCH {
      @Override
      public HttpRequestBase create(String url) {
        return new HttpPatch(url);
      }
    },
    PUT {
      @Override
      public HttpRequestBase create(String url) {
        return new HttpPut(url);
      }
    },
    POST {
      @Override
      public HttpPost create(String url) {
        return new HttpPost(url);
      }
    },
    TRACE {
      @Override
      public HttpRequestBase create(String url) {
        return new HttpTrace(url);
      }
    };
    public abstract HttpRequestBase create(String url);
  }

  @NotNull
  @AutoPopulated
  @Valid
  private RequestMethodProvider methodProvider;
  @Deprecated
  private HttpMethod method;

  @Deprecated
  private String userName = null;
  @Deprecated
  @InputFieldHint(style = "PASSWORD", external = true)
  private String password = null;

  @NotNull
  @Valid
  @AutoPopulated
  @AdvancedConfig
  private ContentTypeProvider contentTypeProvider;

  @AdvancedConfig
  @Valid
  @NotNull
  @AutoPopulated
  private ResponseHeaderHandler<HttpResponse> responseHeaderHandler;

  @AdvancedConfig
  @Valid
  @NotNull
  @AutoPopulated
  private RequestHeaderProvider<HttpRequestBase> requestHeaderProvider;

  @AdvancedConfig
  @InputFieldDefault(value = "false")
  private Boolean ignoreServerResponseCode;
  @AdvancedConfig
  @InputFieldDefault(value = "true")
  private Boolean allowRedirect;
  @Valid
  private HttpAuthenticator authenticator;

  private transient String authString = null;

  public HttpProducer() {
    super();
    setContentTypeProvider(new ConfiguredContentTypeProvider());
    setResponseHeaderHandler(new DiscardResponseHeaders());
    setRequestHeaderProvider(new NoOpRequestHeaders());
    setMethodProvider(new ConfiguredRequestMethodProvider(RequestMethod.POST));
  }

  @Override
  public void start() throws CoreException {
  }

  @Override
  public void stop() {
  }

  @Override
  public void close() {
  }

  @Override
  public void init() throws CoreException {
  }


  /**
   * 
   * @see com.adaptris.core.RequestReplyProducerImp#defaultTimeout()
   */
  @Override
  protected long defaultTimeout() {
    return DEFAULT_SOCKET_TIMEOUT;
  }


  /**
   * 
   * @see RequestReplyProducerImp#produce(AdaptrisMessage, ProduceDestination)
   */
  @Override
  public void produce(AdaptrisMessage msg, ProduceDestination dest) throws ProduceException {
    doRequest(msg, dest, defaultTimeout());
  }

  /**
   * 
   * @param s the user name
   */
  public void setUserName(String s) {
    userName = s;
  }

  /**
   * Set the RFC 2617 password.
   * <p>
   * In additional to plain text passwords, the passwords can also be encoded using the appropriate {@link Password}
   * </p>
   * 
   * @param s the password
   * @deprecated since 3.6.0 use {@link #setAuthenticator(HttpAuthenticator)} instead
   */
  @Deprecated
  public void setPassword(String s) {
    password = s;
  }

  /**
   * Get the username.
   * 
   * @return username
   */
  public String getUserName() {
    return userName;
  }

  /**
   * Get the password.
   * 
   * @return the password
   * @deprecated since 3.6.0 use {@link #setAuthenticator(HttpAuthenticator)} instead
   */
  @Deprecated
  public String getPassword() {
    return password;
  }

  /**
   * Specify whether to automatically handle redirection.
   * 
   * @param b true or false.
   */
  public void setAllowRedirect(Boolean b) {
    allowRedirect = b;
  }

  boolean handleRedirection() {
    return allowRedirect != null ? allowRedirect.booleanValue() : false;
  }

  /**
   * Get the handle redirection flag.
   * 
   * @return true or false.
   */
  public Boolean getAllowRedirect() {
    return allowRedirect;
  }

  /**
   * Get the currently configured flag for ignoring server response code.
   * 
   * @return true or false
   * @see #setIgnoreServerResponseCode(Boolean)
   */
  public Boolean getIgnoreServerResponseCode() {
    return ignoreServerResponseCode;
  }

  boolean ignoreServerResponseCode() {
    return ignoreServerResponseCode != null ? ignoreServerResponseCode.booleanValue() : false;
  }

  /**
   * Set whether to ignore the server response code.
   * <p>
   * In some cases, you may wish to ignore any server response code (such as 500) as this may return meaningful data that you wish
   * to use. If that's the case, make sure this flag is true. It defaults to false.
   * </p>
   * <p>
   * In all cases the metadata key {@link CoreConstants#HTTP_PRODUCER_RESPONSE_CODE} is populated with the last server response.
   * </p>
   * 
   * @see CoreConstants#HTTP_PRODUCER_RESPONSE_CODE
   * @param b true
   */
  public void setIgnoreServerResponseCode(Boolean b) {
    ignoreServerResponseCode = b;
  }

  @SuppressWarnings("deprecation")
  protected void copyHeaders(AdaptrisMessage src, AdaptrisMessage dest) throws IOException, CoreException {
    dest.getObjectMetadata().putAll(src.getObjectMetadata());
    dest.setMetadata(src.getMetadata());
  }

  public ContentTypeProvider getContentTypeProvider() {
    return contentTypeProvider;
  }

  /**
   * Specify the Content-Type header associated with the HTTP operation.
   * 
   * @param ctp
   */
  public void setContentTypeProvider(ContentTypeProvider ctp) {
    this.contentTypeProvider = ctp;
  }

  /**
   * @deprecated since 3.0.6; use {@link #getMethodProvider()} instead.
   */
  @Deprecated
  public HttpMethod getMethod() {
    return method;
  }

  /**
   * Set the HTTP method to be used.
   * 
   * @param method the method; defaults to {@link HttpMethod#POST}
   * @deprecated since 3.0.6 use {@link #setMethodProvider(RequestMethodProvider)} instead.
   */
  @Deprecated
  public void setMethod(HttpMethod method) {
    this.method = Args.notNull(method, "Method");
  }


  public ResponseHeaderHandler<HttpResponse> getResponseHeaderHandler() {
    return responseHeaderHandler;
  }

  /**
   * Specify how we handle headers from the HTTP response.
   * 
   * @param handler the handler, default is a {@link DiscardResponseHeaders}.
   */
  public void setResponseHeaderHandler(ResponseHeaderHandler<HttpResponse> handler) {
    this.responseHeaderHandler = Args.notNull(handler, "ResponseHeaderHandler");
  }

  public RequestHeaderProvider<HttpRequestBase> getRequestHeaderProvider() {
    return requestHeaderProvider;
  }

  /**
   * Specify how we want to generate the initial set of HTTP Headers.
   * 
   * @param handler the handler, default is a {@link NoOpRequestHeaders}
   */
  public void setRequestHeaderProvider(RequestHeaderProvider<HttpRequestBase> handler) {
    this.requestHeaderProvider = Args.notNull(handler, "Request Header Handler");
  }

  public RequestMethodProvider getMethodProvider() {
    return methodProvider;
  }

  public void setMethodProvider(RequestMethodProvider p) {
    this.methodProvider = Args.notNull(p, "Method Provider");
  }

  public HttpAuthenticator getAuthenticator() {
    return authenticator;
  }

  /**
   * Set the authentication method to use for the HTTP request
   * 
   * @see ApacheRequestAuthenticator
   * @see ConfiguredUsernamePassword
   * @see MetadataUsernamePassword
   * @see ConfiguredAuthorizationHeader
   * @see MetadataAuthorizationHeader
   */
  public void setAuthenticator(HttpAuthenticator authenticator) {
    this.authenticator = Args.notNull(authenticator, "authenticator");
  }

  protected HttpMethod getMethod(AdaptrisMessage msg) {
    if (getMethod() != null) {
      log.warn("Configured using deprecated setMethod(), configure using #setMethodProvider() instead.");
      return getMethod();
    }
    RequestMethod m = getMethodProvider().getMethod(msg);
    log.trace("HTTP Request Method is : [{}]", m);
    return HttpMethod.valueOf(m.name());
  }

  protected HttpAuthenticator authenticator() {
    HttpAuthenticator authToUse = getAuthenticator() != null ? getAuthenticator() : new NoAuthentication();
    // If deprecated username/password are set and no authenticator is configured, transparently create a static authenticator
    if (authToUse instanceof NoAuthentication && !isEmpty(getUserName())) {
      authToUse = new ConfiguredUsernamePassword(getUserName(), getPassword());
    }
    return authToUse;
  }

  public void prepare() throws CoreException {
  }

}

package com.adaptris.core.http.apache;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
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
import com.adaptris.annotation.Removal;
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
import com.adaptris.util.TimeInterval;

/**
 * Abstract base class for all Apache HTTP producer classes.
 * 
 * @author lchan
 * 
 */
public abstract class HttpProducer extends RequestReplyProducerImp {

  protected static final long DEFAULT_TIMEOUT = -1;
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
  @Deprecated
  @Removal(version = "3.11.0")
  private Boolean allowRedirect;
  @AdvancedConfig
  @Deprecated
  @Removal(version = "3.11.0")
  private String httpProxy;
  @Valid
  private HttpAuthenticator authenticator;
  @Valid
  @AdvancedConfig
  @Deprecated
  @Removal(version = "3.11.0")
  private TimeInterval connectTimeout;
  @Valid
  @AdvancedConfig
  @Deprecated
  @Removal(version = "3.11.0")
  private TimeInterval readTimeout;
  @Valid
  @AdvancedConfig
  private HttpClientBuilderConfigurator clientConfig;

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
    return DEFAULT_TIMEOUT;
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
   * Specify whether to automatically handle redirection.
   * 
   * @param b true or false.
   * @deprecated since 3.8.0 Use a {@link HttpClientBuilderConfigurator} instead.
   */
  @Deprecated
  @Removal(version = "3.11.0", message = "Use HttpClientBuilderConfigurator instead")
  public void setAllowRedirect(Boolean b) {
    allowRedirect = b;
  }

  /**
   * Get the handle redirection flag.
   * 
   * @return true or false.
   * @deprecated since 3.8.0 Use a {@link HttpClientBuilderConfigurator} instead.
   */
  @Deprecated
  @Removal(version = "3.11.0", message = "Use HttpClientBuilderConfigurator instead")
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

  protected boolean ignoreServerResponseCode() {
    return BooleanUtils.toBooleanDefaultIfNull(getIgnoreServerResponseCode(), false);
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
    RequestMethod m = getMethodProvider().getMethod(msg);
    log.trace("HTTP Request Method is : [{}]", m);
    return HttpMethod.valueOf(m.name());
  }

  protected HttpAuthenticator authenticator() {
    return ObjectUtils.defaultIfNull(getAuthenticator(), new NoAuthentication());
  }

  protected HttpClientBuilderConfigurator clientConfig() {
    if (getClientConfig() == null && hasDeprecatedBuilderConfig()) {
      log.warn("Use of deprecated #allowRedirectory, #httpProxy, #readTimeout, #connectTimeout; use a {} instead",
          HttpClientBuilderConfigurator.class.getName());
      return new DefaultClientBuilder().withAllowRedirect(getAllowRedirect()).withConnectTimeout(getConnectTimeout())
          .withProxy(getHttpProxy()).withReadTimeout(getReadTimeout());
    }
    return getClientConfig();
  }

  protected boolean hasDeprecatedBuilderConfig() {
    return BooleanUtils.or(new boolean[]
    {
        getReadTimeout() != null, getAllowRedirect() != null, getConnectTimeout() != null, isNotBlank(getHttpProxy())
    });
  }

  @Override
  public void prepare() throws CoreException {
  }

  /**
   * 
   * @deprecated since 3.8.0 Use a {@link HttpClientBuilderConfigurator} instead via
   *             {@link #setClientConfig(HttpClientBuilderConfigurator)}.
   */
  @Deprecated
  @Removal(version = "3.11.0", message = "Use HttpClientBuilderConfigurator instead")
  public TimeInterval getConnectTimeout() {
    return connectTimeout;
  }

  /**
   * Set the connect timeout.
   * 
   * @param t the timeout.
   * @deprecated since 3.8.0 Use a {@link HttpClientBuilderConfigurator} instead via
   *             {@link #setClientConfig(HttpClientBuilderConfigurator)}.
   */
  @Deprecated
  @Removal(version = "3.11.0", message = "Use HttpClientBuilderConfigurator instead")
  public void setConnectTimeout(TimeInterval t) {
    this.connectTimeout = t;
  }

  /**
   * 
   * @deprecated since 3.8.0 Use a {@link HttpClientBuilderConfigurator} instead via
   *             {@link #setClientConfig(HttpClientBuilderConfigurator)}.
   */
  @Deprecated
  @Removal(version = "3.11.0", message = "Use HttpClientBuilderConfigurator instead")
  public TimeInterval getReadTimeout() {
    return readTimeout;
  }

  /**
   * Set the read timeout.
   * <p>
   * Note that any read timeout will be overridden by the timeout value passed in via the {{@link #request(AdaptrisMessage, long)}
   * method, provided it differs from {@link #defaultTimeout()}. Apache HTTP calls this the socket timeout in their documentation.
   * </p>
   * 
   * @param t the timeout.
   * @deprecated since 3.8.0 Use a {@link HttpClientBuilderConfigurator} instead via
   *             {@link #setClientConfig(HttpClientBuilderConfigurator)}.
   */
  @Deprecated
  @Removal(version = "3.11.0", message = "Use HttpClientBuilderConfigurator instead")
  public void setReadTimeout(TimeInterval t) {
    this.readTimeout = t;
  }

  /**
   * @return the httpProxy
   * @deprecated since 3.8.0 Use a {@link HttpClientBuilderConfigurator} instead via
   *             {@link #setClientConfig(HttpClientBuilderConfigurator)}.
   */
  @Deprecated
  @Removal(version = "3.11.0", message = "Use HttpClientBuilderConfigurator instead")
  public String getHttpProxy() {
    return httpProxy;
  }

  /**
   * Explicitly configure a proxy server.
   * 
   * @param proxy the httpProxy to generally {@code scheme://host:port} or more simply {@code host:port}
   * @deprecated since 3.8.0 Use a {@link HttpClientBuilderConfigurator} instead via
   *             {@link #setClientConfig(HttpClientBuilderConfigurator)}.
   */
  @Deprecated
  @Removal(version = "3.11.0", message = "Use HttpClientBuilderConfigurator instead")
  public void setHttpProxy(String proxy) {
    this.httpProxy = proxy;
  }

  public HttpClientBuilderConfigurator getClientConfig() {
    return clientConfig;
  }

  /**
   * Specify any custom {@code HttpClientBuilder} configuration.
   * 
   * @param httpClientCustomiser a {@link HttpClientBuilderConfigurator} instance.
   */
  public void setClientConfig(HttpClientBuilderConfigurator httpClientCustomiser) {
    this.clientConfig = httpClientCustomiser;
  }

}

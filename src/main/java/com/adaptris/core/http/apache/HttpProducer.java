package com.adaptris.core.http.apache;

import static com.adaptris.core.util.DestinationHelper.logWarningIfNotNull;
import static com.adaptris.core.util.DestinationHelper.mustHaveEither;
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
import com.adaptris.annotation.InputFieldHint;
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
import com.adaptris.core.util.DestinationHelper;
import com.adaptris.core.util.LoggingHelper;
import lombok.Getter;
import lombok.Setter;

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
  @Valid
  private HttpAuthenticator authenticator;
  @Valid
  @AdvancedConfig
  private HttpClientBuilderConfigurator clientConfig;
  /**
   * The ProduceDestination contains the url we will access.
   *
   */
  @Getter
  @Setter
  @Deprecated
  @Valid
  @Removal(version = "4.0.0", message = "Use 'url' instead")
  private ProduceDestination destination;

  /**
   * The URL endpoint to access.
   */
  @InputFieldHint(expression = true)
  @Getter
  @Setter
  // Needs to be @NotBlank when destination is removed.
  private String url;

  private transient boolean destWarning;

  public HttpProducer() {
    super();
    setContentTypeProvider(new ConfiguredContentTypeProvider());
    setResponseHeaderHandler(new DiscardResponseHeaders());
    setRequestHeaderProvider(new NoOpRequestHeaders());
    setMethodProvider(new ConfiguredRequestMethodProvider(RequestMethod.POST));
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
    contentTypeProvider = ctp;
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
    responseHeaderHandler = Args.notNull(handler, "ResponseHeaderHandler");
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
    requestHeaderProvider = Args.notNull(handler, "Request Header Handler");
  }

  public RequestMethodProvider getMethodProvider() {
    return methodProvider;
  }

  public void setMethodProvider(RequestMethodProvider p) {
    methodProvider = Args.notNull(p, "Method Provider");
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
    return getClientConfig();
  }

  @Override
  protected void doProduce(AdaptrisMessage msg, String endpoint) throws ProduceException {
    doRequest(msg, endpoint, defaultTimeout());
  }

  @Override
  public void prepare() throws CoreException {
    logWarningIfNotNull(destWarning, () -> destWarning = true, getDestination(),
        "{} uses destination, use 'url' instead", LoggingHelper.friendlyName(this));
    mustHaveEither(getUrl(), getDestination());
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
    clientConfig = httpClientCustomiser;
  }

  @Override
  public String endpoint(AdaptrisMessage msg) throws ProduceException {
    return DestinationHelper.resolveProduceDestination(getUrl(), getDestination(), msg);
  }

  public <T extends HttpProducer> T withURL(String s) {
    setUrl(s);
    return (T) this;
  }

}

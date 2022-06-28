package com.adaptris.core.http.apache;

import static com.adaptris.core.http.apache.ResponseHandlerFactory.OBJ_METADATA_PAYLOAD_MODIFIED;

import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.annotation.InputFieldDefault;
import com.adaptris.annotation.InputFieldHint;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.CoreConstants;
import com.adaptris.core.CoreException;
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
import com.adaptris.core.http.client.net.MetadataAuthorizationHeader;
import com.adaptris.core.util.MessageHelper;
import com.adaptris.interlok.util.Args;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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

/**
 * Abstract base class for all Apache HTTP producer classes.
 *
 */
@NoArgsConstructor
public abstract class HttpProducer extends RequestReplyProducerImp {

  private static final RequestMethodProvider DEFAULT_METHOD = new ConfiguredRequestMethodProvider(RequestMethod.POST);
  private static final ConfiguredContentTypeProvider DEFAULT_CONTENT = new ConfiguredContentTypeProvider("text/plain");

  protected static final long DEFAULT_TIMEOUT = -1;

  /**
   * Maps various methods supported by the Apache Http client.
   *
   */
  public enum HttpMethod {
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

  /**
   * The HTTP method.
   * <p>
   * The default is {@code POST}
   * </p>
   */
  @Valid
  @InputFieldDefault(value = "POST")
  @Getter
  @Setter
  private RequestMethodProvider methodProvider;

  /**
   * The Content-Type header associated with the HTTP operation.
   * <p>
   * The default is {@code text/plain} based on the default from
   * {@link ConfiguredContentTypeProvider}.
   * </p>
   *
   */
  @Valid
  @Getter
  @Setter
  @AdvancedConfig
  @InputFieldDefault(value = "text/plain")
  private ContentTypeProvider contentTypeProvider;

  /**
   * Specify how we handle headers from the HTTP response.
   *
   * <p>
   * If not explicitly configured then the default is {@link DiscardResponseHeaders}
   * </p>
   */
  @AdvancedConfig
  @Valid
  @Getter
  @Setter
  @InputFieldDefault(value = "discard response headers")
  private ResponseHeaderHandler<HttpResponse> responseHeaderHandler;

  /**
   * Any additional HTTP headers we wish to send with the request.
   * <p>
   * If not explicitly configured then the default is {@link NoOpRequestHeaders}
   * </p>
   *
   */
  @AdvancedConfig
  @Getter
  @Setter
  @Valid
  private RequestHeaderProvider<HttpRequestBase> requestHeaderProvider;

  /**
   * Control whether or not to ignore the server response code.
   * <p>
   * In some cases, you may wish to ignore any server response code (such as 500) as this may return
   * meaningful data that you wish to use. If that's the case, make sure this flag is true. It
   * defaults to false.
   * </p>
   * <p>
   * In all cases the metadata key {@link CoreConstants#HTTP_PRODUCER_RESPONSE_CODE} is populated
   * with the last server response.
   * </p>
   *
   */
  @AdvancedConfig
  @Getter
  @Setter
  @InputFieldDefault(value = "false")
  private Boolean ignoreServerResponseCode;
  /**
   * Set the authentication method to use for the HTTP request
   * <p>
   * If not explicitly configured then defaults to {@link NoAuthentication}.
   * </p>
   *
   * @see ApacheRequestAuthenticator
   * @see ConfiguredUsernamePassword
   * @see MetadataUsernamePassword
   * @see ConfiguredAuthorizationHeader
   * @see MetadataAuthorizationHeader
   */
  @Valid
  @Getter
  @Setter
  private HttpAuthenticator authenticator;
  /**
   * Customise the underlying Apache {@code HttpClientBuilder} before the request is made.
   *
   * <p>
   * If not explicitly configured will be a {@link DefaultClientBuilder} with its defaults.
   * </p>
   */
  @Valid
  @AdvancedConfig
  @Getter
  @Setter
  private HttpClientBuilderConfigurator clientConfig;

  /**
   * The URL endpoint to access.
   */
  @InputFieldHint(expression = true)
  @Getter
  @Setter
  @NotBlank
  private String url;

  /**
   *
   * @see com.adaptris.core.RequestReplyProducerImp#defaultTimeout()
   */
  @Override
  protected long defaultTimeout() {
    return DEFAULT_TIMEOUT;
  }


  protected boolean ignoreServerResponseCode() {
    return BooleanUtils.toBooleanDefaultIfNull(getIgnoreServerResponseCode(), false);
  }

  protected HttpMethod getMethod(AdaptrisMessage msg) {
    RequestMethod m = methodProvider().getMethod(msg);
    log.trace("HTTP Request Method is : [{}]", m);
    return HttpMethod.valueOf(m.name());
  }

  protected HttpAuthenticator authenticator() {
    return ObjectUtils.defaultIfNull(getAuthenticator(), new NoAuthentication());
  }

  @Override
  protected void doProduce(AdaptrisMessage msg, String endpoint) throws ProduceException {
    doRequest(msg, endpoint, defaultTimeout());
  }

  @Override
  public void prepare() throws CoreException {
    Args.notBlank(getUrl(), "url");
  }

  @Override
  public String endpoint(AdaptrisMessage msg) throws ProduceException {
    return msg.resolve(getUrl());
  }

  @SuppressWarnings("unchecked")
  public <T extends HttpProducer> T withURL(String s) {
    setUrl(s);
    return (T) this;
  }

  protected ResponseHeaderHandler<HttpResponse> responseHeaderHandler() {
    return ObjectUtils.defaultIfNull(getResponseHeaderHandler(), new DiscardResponseHeaders());
  }

  protected RequestHeaderProvider<HttpRequestBase> requestHeaderProvider() {
    return ObjectUtils.defaultIfNull(getRequestHeaderProvider(), new NoOpRequestHeaders());
  }

  protected RequestMethodProvider methodProvider() {
    return ObjectUtils.defaultIfNull(getMethodProvider(), DEFAULT_METHOD);
  }

  protected ContentTypeProvider contentTypeProvider() {
    return ObjectUtils.defaultIfNull(getContentTypeProvider(), DEFAULT_CONTENT);
  }

  /**
   * Ensures that if the reply hasn't got a new payload then we copy the request payload into the
   * response.
   *
   */
  protected void preserveRequestPayload(AdaptrisMessage request, AdaptrisMessage response)
      throws Exception {
    boolean responseModifiedPayload =
        (Boolean) response.getObjectHeaders().get(OBJ_METADATA_PAYLOAD_MODIFIED);
    if (!responseModifiedPayload) {
      MessageHelper.copyPayload(request, response);
    }
  }
}

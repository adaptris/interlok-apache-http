package com.adaptris.core.http.apache5;

import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.annotation.AutoPopulated;
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
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpHead;
import org.apache.hc.client5.http.classic.methods.HttpOptions;
import org.apache.hc.client5.http.classic.methods.HttpPatch;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpTrace;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.HttpResponse;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import static com.adaptris.core.http.apache5.ResponseHandlerFactory.OBJ_METADATA_PAYLOAD_MODIFIED;

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
      public HttpUriRequestBase create(String url) {
        return new HttpDelete(url);
      }
    },
    GET {
      @Override
      public HttpUriRequestBase create(String url) {
        return new HttpGet(url);
      }
    },
    HEAD {
      @Override
      public HttpUriRequestBase create(String url) {
        return new HttpHead(url);
      }
    },
    OPTIONS {
      @Override
      public HttpUriRequestBase create(String url) {
        return new HttpOptions(url);
      }
    },
    PATCH {
      @Override
      public HttpUriRequestBase create(String url) {
        return new HttpPatch(url);
      }
    },
    PUT {
      @Override
      public HttpUriRequestBase create(String url) {
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
      public HttpUriRequestBase create(String url) {
        return new HttpTrace(url);
      }
    };
    public abstract HttpUriRequestBase create(String url);
  }

  /**
   * The HTTP method.
   * <p>
   * The default is {@code POST}
   * </p>
   */
  @NotNull
  @AutoPopulated
  @Valid
  @NonNull
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
  @NotNull
  @Valid
  @AutoPopulated
  @AdvancedConfig
  @NonNull
  @Getter
  @Setter
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
  @NonNull
  @NotNull
  @AutoPopulated
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
  @Valid
  @NotNull
  @AutoPopulated
  @NonNull
  @Getter
  @Setter
  private RequestHeaderProvider<HttpUriRequestBase> requestHeaderProvider;

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
  @InputFieldDefault(value = "false")
  @Getter
  @Setter
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


  protected boolean ignoreServerResponseCode() {
    return BooleanUtils.toBooleanDefaultIfNull(getIgnoreServerResponseCode(), false);
  }

  protected HttpMethod getMethod(AdaptrisMessage msg) {
    RequestMethod m = getMethodProvider().getMethod(msg);
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

  public <T extends HttpProducer> T withURL(String s) {
    setUrl(s);
    return (T) this;
  }

  /**
   * Ensures that if the reply hasn't got a new payload then we copy the request payload into the
   * response.
   *
   */
  protected void preserveRequestPayload(AdaptrisMessage request, AdaptrisMessage response)
      throws Exception {
    boolean responseModifiedPayload =
        ((Boolean) response.getObjectHeaders().get(OBJ_METADATA_PAYLOAD_MODIFIED)).booleanValue();
    if (!responseModifiedPayload) {
      MessageHelper.copyPayload(request, response);
    }
  }
}

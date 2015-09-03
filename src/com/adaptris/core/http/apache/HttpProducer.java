package com.adaptris.core.http.apache;

import static com.adaptris.core.http.HttpConstants.DEFAULT_SOCKET_TIMEOUT;
import static org.apache.commons.lang.StringUtils.isEmpty;

import java.io.IOException;
import java.net.PasswordAuthentication;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.util.Args;
import org.hibernate.validator.constraints.NotBlank;

import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.annotation.AutoPopulated;
import com.adaptris.annotation.InputFieldHint;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageImp;
import com.adaptris.core.CoreConstants;
import com.adaptris.core.CoreException;
import com.adaptris.core.ProduceDestination;
import com.adaptris.core.ProduceException;
import com.adaptris.core.RequestReplyProducerImp;
import com.adaptris.security.password.Password;
import com.adaptris.util.license.License;
import com.adaptris.util.license.License.LicenseType;

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

  private String userName = null;
  @InputFieldHint(style = "PASSWORD")
  private String password = null;
  @NotBlank
  @AutoPopulated
  private HttpMethod method;

  @NotNull
  @Valid
  @AutoPopulated
  @AdvancedConfig
  private ContentTypeProvider contentTypeProvider;

  @AdvancedConfig
  @Valid
  @NotNull
  @AutoPopulated
  private ResponseHeaderHandler responseHandler;

  @AdvancedConfig
  @Valid
  @NotNull
  @AutoPopulated
  private RequestHeaderHandler requestHandler;

  @AdvancedConfig
  private Boolean ignoreServerResponseCode;
  @AdvancedConfig
  private Boolean allowRedirect;



  private transient String authString = null;
  private transient PasswordAuthentication passwordAuth;

  public HttpProducer() {
    super();
    setContentTypeProvider(new StaticContentTypeProvider());
    setResponseHandler(new DiscardResponseHeaders());
    setRequestHandler(new NoOpRequestHeaders());
    setMethod(HttpMethod.POST);
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
    try {
      if (!isEmpty(userName)) {
        passwordAuth = new PasswordAuthentication(userName, Password.decode(password).toCharArray());
      }
    }
    catch (Exception e) {
      throw new CoreException(e);
    }

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
   */
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
   */
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

  protected PasswordAuthentication getPasswordAuthentication() {
    return passwordAuth;
  }

  protected void copy(AdaptrisMessage src, AdaptrisMessage dest) throws IOException, CoreException {
    AdaptrisMessageImp.copyPayload(src, dest);
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
   * @see com.adaptris.core.AdaptrisComponent#isEnabled(License)
   */
  @Override
  public boolean isEnabled(License l) throws CoreException {
    return l.isEnabled(LicenseType.Basic);
  }


  public HttpMethod getMethod() {
    return method;
  }

  /**
   * Set the HTTP method to be used.
   * 
   * @param method the method; defaults to {@link HttpMethod#POST}
   */
  public void setMethod(HttpMethod method) {
    this.method = method;
  }


  public ResponseHeaderHandler getResponseHandler() {
    return responseHandler;
  }

  /**
   * Specify how we handle headers from the HTTP response.
   * 
   * @param handler the handler, default is a {@link DiscardResponseHeaders}.
   */
  public void setResponseHandler(ResponseHeaderHandler handler) {
    this.responseHandler = Args.notNull(handler, "ResponseHeaderHandler");
  }

  public RequestHeaderHandler getRequestHandler() {
    return requestHandler;
  }

  /**
   * Specify how we want to generate the initial set of HTTP Headers.
   * 
   * @param handler the handler, default is a {@link NoOpRequestHeaders}
   */
  public void setRequestHandler(RequestHeaderHandler handler) {
    this.requestHandler = Args.notNull(handler, "Request Header Handler");
  }


}

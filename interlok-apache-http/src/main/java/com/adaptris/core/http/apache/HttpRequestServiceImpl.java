/*
 * Copyright 2017 Adaptris Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.adaptris.core.http.apache;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.annotation.AutoPopulated;
import com.adaptris.annotation.InputFieldDefault;
import com.adaptris.annotation.InputFieldHint;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.CoreException;
import com.adaptris.core.NullConnection;
import com.adaptris.core.ServiceImp;
import com.adaptris.core.StandaloneProducer;
import com.adaptris.core.StandaloneRequestor;
import com.adaptris.core.http.RawContentTypeProvider;
import com.adaptris.core.http.auth.AdapterResourceAuthenticator;
import com.adaptris.core.http.auth.HttpAuthenticator;
import com.adaptris.core.http.auth.NoAuthentication;
import com.adaptris.core.http.client.ConfiguredRequestMethodProvider;
import com.adaptris.core.http.client.RequestHeaderProvider;
import com.adaptris.core.http.client.RequestMethodProvider.RequestMethod;
import com.adaptris.core.http.client.ResponseHeaderHandler;
import com.adaptris.core.http.client.net.NoRequestHeaders;
import java.net.Authenticator;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;

/**
 * Direct HTTP support as a service rather than wrapped via {@link StandaloneProducer} or {@link StandaloneRequestor}.
 *
 * <p>
 * Note that this service just wraps a {@link ApacheHttpProducer} instance but doesn't expose all the possible settings
 * available for the normal {@link ApacheHttpProducer}. If you need those features, than continue using the producer
 * wrapped as a {@link StandaloneProducer} or {@link StandaloneRequestor}.
 * </p>
 * <p>
 * String parameters in this service will use the {@link AdaptrisMessage#resolve(String)} which allows you to specify
 * metadata values as part of a constant string e.g. {@code setUrl("%message{http_url}")} will use the metadata value
 * associated with the key {@code http_url}.
 * </p>
 */
public abstract class HttpRequestServiceImpl extends ServiceImp {

  /**
   * The URL to target.
   * <p>The URL is resolved and runtime, and supports the expression notation {@code %message} etc.</p>
   */
  @Getter
  @Setter
  @NotBlank(message = "URL may not be blank")
  @InputFieldHint(expression = true)
  private String url;
  /**
   * The Content-Type for the request
   * <p>Defaults to {@code text/plain} if not explicitly specified, is resolved at runtime and supports the expression
   * notation {@code %message} etc.</p>
   */
  @AutoPopulated
  @InputFieldDefault(value = "text/plain")
  @InputFieldHint(expression = true)
  @Getter
  @Setter
  private String contentType;

  /**
   * The method to use with the HTTP Request.
   * <p>The method is resolved and runtime, and supports the expression notation {@code %message} etc, it defaults to
   * 'POST' if not explicitly configured.</p>
   */
  @AutoPopulated
  @InputFieldDefault(value = "POST")
  @InputFieldHint(expression = true, style = "com.adaptris.core.http.client.RequestMethodProvider.RequestMethod")
  @Getter
  @Setter
  private String method;

  /** How to handle HTTP response headers.
   *  <p>Defaults to {@link DiscardResponseHeaders} if not explicitly configured.</p>
   */
  @AdvancedConfig
  @Valid
  @InputFieldDefault(value = "discard-response-headers")
  @Getter
  @Setter
  private ResponseHeaderHandler<HttpResponse> responseHeaderHandler;


  /** How to supply HTTP request headers.
   *  <p>Defaults to {@link NoRequestHeaders} if not explicitly configured.</p>
   */
  @AdvancedConfig
  @Valid
  @InputFieldDefault(value = "no-request-headers")
  @Getter
  @Setter
  private RequestHeaderProvider<HttpRequestBase> requestHeaderProvider;

  /** The authentication for the request.
   *  <p>If not explicitly specified then defaults to {@link NoAuthentication}</p>
   */
  @Valid
  @AdvancedConfig
  @AutoPopulated
  @InputFieldDefault(value = "no-authentication")
  @Getter
  @Setter
  private HttpAuthenticator authenticator = new NoAuthentication();

  /** Any additional HTTP Client configuration required.
   *
   */
  @Valid
  @AdvancedConfig
  @Getter
  @Setter
  private HttpClientBuilderConfigurator clientConfig;

  public HttpRequestServiceImpl() {
    super();
    Authenticator.setDefault(AdapterResourceAuthenticator.getInstance());
  }

  @Override
  public void prepare() throws CoreException {
    if (isEmpty(url)) {
      throw new CoreException("Empty URL param");
    }
  }

  @Override
  protected void initService() throws CoreException {

  }

  @Override
  protected void closeService() {
  }

  protected ApacheHttpProducer buildProducer(AdaptrisMessage msg) {
    ApacheHttpProducer p = new ApacheHttpProducer();
    p.setMessageFactory(msg.getFactory());
    p.setClientConfig(clientConfig());
    p.setUrl(msg.resolve(getUrl()));
    p.setContentTypeProvider(new RawContentTypeProvider(msg.resolve(contentType())));
    p.setMethodProvider(
        new ConfiguredRequestMethodProvider(RequestMethod.valueOf(msg.resolve(method()).toUpperCase())));
    p.setAuthenticator(getAuthenticator());
    p.setRequestHeaderProvider(requestHeaderProvider());
    p.setResponseHeaderHandler(responseHeaderHandler());
    p.registerConnection(new NullConnection());
    return p;
  }

  protected HttpClientBuilderConfigurator clientConfig() {
    // If it's still null, it will get defaulted anyway by the underlying producer.
    // so we should be good.
    return getClientConfig();
  }

  protected ResponseHeaderHandler<HttpResponse> responseHeaderHandler() {
    return ObjectUtils.defaultIfNull(getResponseHeaderHandler(), new DiscardResponseHeaders());
  }

  protected RequestHeaderProvider<HttpRequestBase> requestHeaderProvider() {
    return ObjectUtils.defaultIfNull(getRequestHeaderProvider(), new NoOpRequestHeaders());
  }

  protected String contentType() {
    return StringUtils.defaultIfBlank(getContentType(), "text/plain");
  }

  protected String method() {
    return StringUtils.defaultIfBlank(getMethod(), RequestMethod.POST.name());
  }

}

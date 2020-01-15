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
import java.net.Authenticator;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.util.Args;
import org.hibernate.validator.constraints.NotBlank;
import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.annotation.AutoPopulated;
import com.adaptris.annotation.InputFieldDefault;
import com.adaptris.annotation.InputFieldHint;
import com.adaptris.annotation.Removal;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.ConfiguredProduceDestination;
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

/**
 * Direct HTTP support as a service rather than wrapped via {@link StandaloneProducer} or {@link StandaloneRequestor}.
 * 
 * <p>
 * Note that this service just wraps a {@link ApacheHttpProducer} instance but doesn't expose all the possible settings available
 * for the normal {@link ApacheHttpProducer}. If you need those features, than continue using the producer wrapped as a
 * {@link StandaloneProducer} or {@link StandaloneRequestor}.
 * </p>
 * <p>
 * String parameters in this service will use the {@link AdaptrisMessage#resolve(String)} which allows you to specify metadata
 * values as part of a constant string e.g. {@code setUrl("%message{http_url}")} will use the metadata value associated with the key
 * {@code http_url}.
 * </p>
 * 
 */
public abstract class HttpRequestServiceImpl extends ServiceImp {

  @NotBlank
  @InputFieldHint(expression = true)
  private String url;
  @NotBlank
  @AutoPopulated
  @InputFieldDefault(value = "text/plain")
  @InputFieldHint(expression = true)
  private String contentType;
  @NotBlank
  @AutoPopulated
  @InputFieldDefault(value = "POST")
  @InputFieldHint(expression = true, style = "com.adaptris.core.http.client.RequestMethodProvider.RequestMethod")
  private String method;

  @AdvancedConfig
  @Deprecated
  @Removal(version = "3.11.0")
  private String httpProxy;

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
  @Valid
  @AdvancedConfig
  @NotNull
  @AutoPopulated
  private HttpAuthenticator authenticator = new NoAuthentication();

  @Valid
  @AdvancedConfig
  private HttpClientBuilderConfigurator clientConfig;

  public HttpRequestServiceImpl() {
    super();
    Authenticator.setDefault(AdapterResourceAuthenticator.getInstance());
    setResponseHeaderHandler(new DiscardResponseHeaders());
    setRequestHeaderProvider(new NoOpRequestHeaders());
    setContentType("text/plain");
    setMethod("POST");
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
    p.setDestination(new ConfiguredProduceDestination(msg.resolve(getUrl())));
    p.setContentTypeProvider(new RawContentTypeProvider(msg.resolve(getContentType())));
    p.setMethodProvider(new ConfiguredRequestMethodProvider(RequestMethod.valueOf(msg.resolve(getMethod()).toUpperCase())));
    p.setAuthenticator(getAuthenticator());
    p.setRequestHeaderProvider(getRequestHeaderProvider());
    p.setResponseHeaderHandler(getResponseHeaderHandler());
    p.registerConnection(new NullConnection());
    return p;
  }

  protected HttpClientBuilderConfigurator clientConfig() {
    if (getClientConfig() == null && hasDeprecatedBuilderConfig()) {
      log.warn("Use of deprecated #httpProxy; use a {} instead",
          HttpClientBuilderConfigurator.class.getName());
      return new DefaultClientBuilder().withProxy(getHttpProxy());
    }
    // If it's still null, it will get defaulted anyway by the underlying producer.
    // so we should be good.
    return getClientConfig();
  }

  protected boolean hasDeprecatedBuilderConfig() {
    return !isEmpty(getHttpProxy());
  }

  /**
   * @return the responseHeaderHandler
   */
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
   * @param handler the handler, default is a {@link NoRequestHeaders}
   */
  public void setRequestHeaderProvider(RequestHeaderProvider<HttpRequestBase> handler) {
    this.requestHeaderProvider = Args.notNull(handler, "Request Header Provider");
  }


  /**
   * @return the url
   */
  public String getUrl() {
    return url;
  }

  /**
   * @param s the url to set; can be of the form {@code %message{key1}} to use the metadata value associated with {@code key1}
   */
  public void setUrl(String s) {
    this.url = s;
  }

  /**
   * @return the contentType
   */
  public String getContentType() {
    return contentType;
  }

  /**
   * @param ct the contentType to set; can be of the form {@code %message{key1}} to use the metadata value associated with
   *          {@code key1}
   */
  public void setContentType(String ct) {
    this.contentType = ct;
  }

  /**
   * @return the method
   */
  public String getMethod() {
    return method;
  }

  /**
   * @param m the method to set; can be of the form {@code %message{key1}} to use the metadata value associated with
   *          {@code key1}
   */
  public void setMethod(String m) {
    this.method = m;
  }

  /**
   * @return the authenticator
   */
  public HttpAuthenticator getAuthenticator() {
    return authenticator;
  }

  /**
   * @param auth the authenticator to set
   */
  public void setAuthenticator(HttpAuthenticator auth) {
    this.authenticator = auth;
  }

  /**
   * @return the httpProxy
   * @deprecated since 3.8.0 Use a {@link HttpClientBuilderConfigurator} via {@link #setClientConfig(HttpClientBuilderConfigurator)}
   *             instead.
   */
  @Deprecated
  @Removal(version = "3.11.0")
  public String getHttpProxy() {
    return httpProxy;
  }

  /**
   * Explicitly configure a proxy server.
   * 
   * @param proxy the httpProxy to generally {@code scheme://host:port} or more simply {@code host:port}
   * @deprecated since 3.8.0 Use a {@link HttpClientBuilderConfigurator} via {@link #setClientConfig(HttpClientBuilderConfigurator)}
   *             instead.
   */
  @Deprecated
  @Removal(version = "3.11.0")
  public void setHttpProxy(String proxy) {
    this.httpProxy = proxy;
  }

  public HttpClientBuilderConfigurator getClientConfig() {
    return clientConfig;
  }

  /**
   * Specify any custom {@code HttpClientBuilder} configuration.
   * 
   * @param clientConfig a {@link HttpClientBuilderConfigurator} instance.
   */
  public void setClientConfig(HttpClientBuilderConfigurator clientConfig) {
    this.clientConfig = clientConfig;
  }
}

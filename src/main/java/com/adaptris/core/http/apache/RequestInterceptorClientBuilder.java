/*
 * Copyright 2018 Adaptris Ltd.
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

import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.core.http.apache.request.RequestInterceptorBuilder;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * {@link HttpClientBuilderConfigurator} instance that allows additional {@code HttpRequestInterceptor} instances to be added to the
 * outgoing request.
 * 
 * @config request-interceptor-apache-http-client-builder
 */
@XStreamAlias("request-interceptor-apache-http-client-builder")
public class RequestInterceptorClientBuilder implements HttpClientBuilderConfigurator {

  @AdvancedConfig
  @Valid
  @XStreamImplicit
  private List<RequestInterceptorBuilder> requestInterceptors;

  public List<RequestInterceptorBuilder> getRequestInterceptors() {
    return requestInterceptors;
  }

  /**
   * Set any additional request interceptors that will be added to the {@link HttpClientBuilder}.
   * 
   * @param list
   */
  public void setRequestInterceptors(List<RequestInterceptorBuilder> list) {
    this.requestInterceptors = list;
  }

  public RequestInterceptorClientBuilder withInterceptors(RequestInterceptorBuilder... interceptors) {
    setRequestInterceptors(new ArrayList<>(Arrays.asList(interceptors)));
    return this;
  }

  protected List<RequestInterceptorBuilder> requestInterceptors() {
    return getRequestInterceptors() != null ? getRequestInterceptors() : Collections.emptyList();
  }

  @Override
  public HttpClientBuilder configure(HttpClientBuilder builder) throws Exception {
    for (RequestInterceptorBuilder b : requestInterceptors()) {
      builder.addRequestInterceptorLast(b.build());
    }
    return builder;
  }

}

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.validation.Valid;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.http.impl.client.HttpClientBuilder;

import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.core.http.apache.request.RequestInterceptorBuilder;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

/**
 * {@link HttpClientBuilderConfigurator} instance that allows additional {@code HttpRequestInterceptor} instances to be added to the
 * outgoing request.
 *
 * @config request-interceptor-apache-http-client-builder
 */
@XStreamAlias("request-interceptor-apache-http-client-builder")
@NoArgsConstructor
public class RequestInterceptorClientBuilder implements HttpClientBuilderConfigurator {

  /** Additional request interceptors that will be added to the {@link HttpClientBuilder}.
   *
   */
  @Getter
  @Setter
  @Valid
  @XStreamImplicit
  private List<RequestInterceptorBuilder> requestInterceptors;

  public RequestInterceptorClientBuilder withInterceptors(RequestInterceptorBuilder... interceptors) {
    setRequestInterceptors(new ArrayList<>(Arrays.asList(interceptors)));
    return this;
  }

  protected List<RequestInterceptorBuilder> requestInterceptors() {
    return ObjectUtils.defaultIfNull(getRequestInterceptors(), Collections.emptyList());
  }

  @Override
  public HttpClientBuilder configure(HttpClientBuilder builder) throws Exception {
    for (RequestInterceptorBuilder b : requestInterceptors()) {
      builder.addInterceptorLast(b.build());
    }
    return builder;
  }

}

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
package interlok.http.apache.async;

import com.adaptris.annotation.ComponentProfile;
import com.adaptris.core.http.apache.HttpClientBuilderConfigurator;
import com.adaptris.core.http.apache.request.RequestInterceptorBuilder;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.validation.Valid;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;

/**
 * {@link HttpClientBuilderConfigurator} instance that allows additional {@code HttpRequestInterceptor} instances to be added to the
 * outgoing request.
 *
 * @config apache-http-async-client-builder-with-request-interceptors
 */
@XStreamAlias("apache-http-async-client-builder-with-request-interceptors")
@NoArgsConstructor
@ComponentProfile(since = "4.5.0", summary = "Allows you to configure additional interceptors when building the HttpAsyncClient")
public class AsyncWithInterceptors implements HttpAsyncClientBuilderConfig {

  /** Additional request interceptors that will be added to the {@link HttpClientBuilder}.
   *
   */
  @Getter
  @Setter
  @Valid
  @XStreamImplicit
  private List<RequestInterceptorBuilder> requestInterceptors;

  public AsyncWithInterceptors withInterceptors(RequestInterceptorBuilder... interceptors) {
    return withInterceptors(new ArrayList<>(List.of(interceptors)));
  }

  public AsyncWithInterceptors withInterceptors(List<RequestInterceptorBuilder> builders) {
    setRequestInterceptors(builders);
    return this;
  }

  protected List<RequestInterceptorBuilder> requestInterceptors() {
    return ObjectUtils.defaultIfNull(getRequestInterceptors(), Collections.emptyList());
  }

  @Override
  public HttpAsyncClientBuilder configure(HttpAsyncClientBuilder builder) throws Exception {
    for (RequestInterceptorBuilder b : requestInterceptors()) {
      builder.addInterceptorLast(b.build());
    }
    return builder;
  }

}

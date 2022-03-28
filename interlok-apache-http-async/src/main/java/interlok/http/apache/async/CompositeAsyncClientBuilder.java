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
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;

/**
 * {@link HttpAsyncClientBuilderConfig} implementation that wraps a list of implementations.
 *
 * @config apache-http-composite-async-client-builder
 * @see AsyncWithCustomKeystores
 * @see DefaultAsyncClientBuilder
 * @see AsyncWithInterceptors
 */
@XStreamAlias("apache-http-composite-async-client-builder")
@NoArgsConstructor
@ComponentProfile(since = "4.5.0", summary = "Allows you to configure additional configuration when building the HttpAsyncClient")
public class CompositeAsyncClientBuilder implements HttpAsyncClientBuilderConfig {

  /** The list of builders that will be used in turn to configure the {@code HttpAsyncClientBuilder}.
   *
   */
  @XStreamImplicit
  @Valid
  @Getter
  @Setter
  private List<HttpAsyncClientBuilderConfig> builders;

  public <T extends CompositeAsyncClientBuilder> T withBuilders(HttpAsyncClientBuilderConfig... builders) {
    return withBuilders(new ArrayList<>(List.of(builders)));
  }

  @SuppressWarnings("unchecked")
  public <T extends CompositeAsyncClientBuilder> T withBuilders(List<HttpAsyncClientBuilderConfig> builders) {
    setBuilders(builders);
    return (T) this;
  }

  private List<HttpAsyncClientBuilderConfig> builders() {
    return ObjectUtils.defaultIfNull(getBuilders(), Collections.emptyList());
  }

  @Override
  public HttpAsyncClientBuilder configure(HttpAsyncClientBuilder builder) throws Exception {
    for (HttpAsyncClientBuilderConfig c : builders()) {
      c.configure(builder);
    }
    return builder;
  }
}

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
import java.util.List;

import javax.validation.constraints.NotNull;

import org.apache.http.impl.client.HttpClientBuilder;

import com.adaptris.core.util.Args;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

/**
 * {@link HttpClientBuilderConfigurator} implementation that wraps a list of implementations.
 * 
 * @config composite-apache-http-client-builder
 * @see {@link CustomTlsBuilder}
 * @see {@link DefaultClientBuilder}
 * @see {@link NoConnectionManagement}
 */
@XStreamAlias("composite-apache-http-client-builder")
public class CompositeClientBuilder implements HttpClientBuilderConfigurator {

  @NotNull
  @XStreamImplicit
  private List<HttpClientBuilderConfigurator> builders;

  public CompositeClientBuilder() {
    setBuilders(new ArrayList<>());
  }

  public List<HttpClientBuilderConfigurator> getBuilders() {
    return builders;
  }

  public void setBuilders(List<HttpClientBuilderConfigurator> list) {
    this.builders = Args.notNull(list, "builders");
  }

  public <T extends CompositeClientBuilder> T withBuilders(HttpClientBuilderConfigurator... builders) {
    this.builders = new ArrayList<>(Arrays.asList(builders));
    return (T) this;
  }

  @Override
  public HttpClientBuilder configure(HttpClientBuilder builder, long timeout) throws Exception {
    HttpClientBuilder result = builder;
    for (HttpClientBuilderConfigurator c : getBuilders()) {
      result = c.configure(result, timeout);
    }
    return result;
  }

  @Override
  public HttpClientBuilder configure(HttpClientBuilder builder) throws Exception {
    HttpClientBuilder result = builder;
    for (HttpClientBuilderConfigurator c : getBuilders()) {
      result = c.configure(result);
    }
    return result;
  }
}

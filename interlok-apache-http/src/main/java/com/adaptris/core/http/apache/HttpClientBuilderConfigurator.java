/*
 * Copyright 2015 Adaptris Ltd.
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

import org.apache.http.impl.client.HttpClientBuilder;

import com.adaptris.core.AdaptrisMessageProducer;

/**
 * Interface that allows {@link HttpClientBuilder} configuration.
 * 
 */
public interface HttpClientBuilderConfigurator {

  /**
   * Do any additional configuration.
   * <p>
   * This is provided as a default method, override as required.
   * </p>
   * 
   * @param builder the existing builder
   * @param timeout the timeout for read operations (provided from
   *          {@link AdaptrisMessageProducer#request(com.adaptris.core.AdaptrisMessage, long)}.
   * @return a reconfigured builder.
   */
  default HttpClientBuilder configure(HttpClientBuilder builder, long timeout) throws Exception {
    return configure(builder);
  }

  /**
   * Do any additional configuration.
   * 
   * @param builder the existing builder
   * @return a reconfigured builder.
   */
  HttpClientBuilder configure(HttpClientBuilder builder) throws Exception;

  static HttpClientBuilderConfigurator defaultIfNull(HttpClientBuilderConfigurator configured) {
    return configured != null ? configured : new DefaultClientBuilder();
  }
}

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
package com.adaptris.core.http.apache.request;

import lombok.NoArgsConstructor;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.RequestDate;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Adds a Date header via {@code RequestDate}.
 *
 * @config apache-http-add-date-to-request
 */
@XStreamAlias("apache-http-add-date-to-request")
@NoArgsConstructor
public class DateHeader implements RequestInterceptorBuilder {

  @Override
  public HttpRequestInterceptor build() {
    return new RequestDate();
  }

}

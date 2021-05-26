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
package com.adaptris.core.http.apache5.request;

import com.adaptris.annotation.AutoPopulated;
import com.adaptris.core.util.Args;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import org.apache.hc.core5.http.HttpRequestInterceptor;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Remove headers from the outgoing request.
 * <p>
 * This is included for completeness; you might, for instance, want to remove the {@code User-Agent} header to avoid being
 * identified as Apache-HTTP...
 * </p>
 *
 * @config apache-http-remove-headers
 */
@XStreamAlias("apache-http5-remove-headers")
public class RemoveHeaders implements RequestInterceptorBuilder {

  @XStreamImplicit(itemFieldName = "header")
  @NotNull
  @AutoPopulated
  private List<String> headers;

  public RemoveHeaders() {
    setHeaders(new ArrayList());
  }

  public RemoveHeaders(String... list) {
    this(new ArrayList<>(Arrays.asList(list)));
  }

  public RemoveHeaders(List<String> list) {
    this();
    setHeaders(list);
  }

  public List<String> getHeaders() {
    return headers;
  }

  public void setHeaders(List<String> list) {
    this.headers = Args.notNull(list, "headers");
  }

  @Override
  public HttpRequestInterceptor build() {
    return (request, entity, context) -> {
      for (String hdr : getHeaders()) {
        request.removeHeaders(hdr);
      }
    };
  }

}

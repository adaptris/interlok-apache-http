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

import com.adaptris.annotation.AutoPopulated;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.http.HttpRequestInterceptor;

/**
 * Remove headers from the outgoing request.
 * <p>
 * This is included for completeness; you might, for instance, want to remove the {@code User-Agent} header to avoid being
 * identified as Apache-HTTP...
 * </p>
 *
 * @config apache-http-remove-headers
 */
@XStreamAlias("apache-http-remove-headers")
@NoArgsConstructor
public class RemoveHeaders implements RequestInterceptorBuilder {

  @XStreamImplicit(itemFieldName = "header")
  @NotNull(message="list of headers may not be null, use an empty list")
  @AutoPopulated
  @Getter
  @Setter
  private List<String> headers = new ArrayList<>();

  public RemoveHeaders(String... list) {
    this(new ArrayList<>(List.of(list)));
  }

  public RemoveHeaders(List<String> list) {
    this();
    setHeaders(list);
  }

  @Override
  public HttpRequestInterceptor build() {
    return (request, context) -> {
      for (String hdr : getHeaders()) {
        request.removeHeaders(hdr);
      }
    };
  }

}

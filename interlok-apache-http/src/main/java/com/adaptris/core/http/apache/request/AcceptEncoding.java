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
import org.apache.http.client.protocol.RequestAcceptEncoding;

/**
 * Add an {@code Accept-Encoding} header to the outgoing request via {@link RequestAcceptEncoding}.
 *
 * @config apache-http-accept-encoding
 */
@XStreamAlias("apache-http-accept-encoding")
@NoArgsConstructor
public class AcceptEncoding implements RequestInterceptorBuilder {

  @XStreamImplicit(itemFieldName = "accept-encoding")
  @NotNull(message="acceptEncodings may not be null")
  @AutoPopulated
  @Getter
  @Setter
  private List<String> acceptEncodings = new ArrayList<>();


  public AcceptEncoding(String... list) {
    this(new ArrayList<>(List.of(list)));
  }

  public AcceptEncoding(List<String> list) {
    this();
    setAcceptEncodings(list);
  }

  @Override
  public HttpRequestInterceptor build() {
    return new RequestAcceptEncoding(getAcceptEncodings());
  }

}

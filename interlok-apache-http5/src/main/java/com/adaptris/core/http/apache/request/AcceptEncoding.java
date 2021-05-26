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
import com.adaptris.core.util.Args;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.HttpRequestInterceptor;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Add an {@code Accept-Encoding} header to the outgoing request via {@link HttpRequestInterceptor}.
 *
 * @config apache-http-accept-encoding
 */
@XStreamAlias("apache-http5-accept-encoding")
public class AcceptEncoding implements RequestInterceptorBuilder {

  @XStreamImplicit(itemFieldName = "accept-encoding")
  @NotNull
  @AutoPopulated
  private List<String> acceptEncodings;

  public AcceptEncoding() {
    setAcceptEncodings(new ArrayList());
  }

  public AcceptEncoding(String... list) {
    this(new ArrayList<>(Arrays.asList(list)));
  }

  public AcceptEncoding(List<String> list) {
    this();
    setAcceptEncodings(list);
  }

  public List<String> getAcceptEncodings() {
    return acceptEncodings;
  }

  public void setAcceptEncodings(List<String> l) {
    this.acceptEncodings = Args.notNull(l, "acceptEncodings");
  }

  @Override
  public HttpRequestInterceptor build() {
    /*
     * RequestAcceptEncoding appears to have been removed from v5, so
     * we'll just do what it did
     */
    return (request, entity, context) -> {
      final HttpClientContext clientContext = HttpClientContext.adapt(context);
      final RequestConfig requestConfig = clientContext.getRequestConfig();
      if (!request.containsHeader("Accept-Encoding") && requestConfig.isContentCompressionEnabled()) {
        if (acceptEncodings.isEmpty()) {
          request.addHeader("Accept-Encoding", "gzip,deflate");
        } else {
          request.addHeader("Accept-Encoding", StringUtils.join(acceptEncodings, ","));
        }
      }
    };
  }

}

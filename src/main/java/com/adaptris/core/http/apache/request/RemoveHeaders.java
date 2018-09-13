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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.Args;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

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
public class RemoveHeaders implements RequestInterceptorBuilder {

  @XStreamImplicit(itemFieldName = "header")
  private List<String> headers;

  public RemoveHeaders() {

  }

  public RemoveHeaders(String... list) {
    this(new ArrayList<String>(Arrays.asList(list)));
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
    return new HttpRequestInterceptor() {
      public void process(HttpRequest request, HttpContext context) {
        for (String hdr : headers) {
          request.removeHeaders(hdr);
        }
      }
    };
  }

}

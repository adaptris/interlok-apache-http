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

import org.apache.http.HttpRequestInterceptor;
import org.apache.http.client.protocol.RequestAcceptEncoding;
import org.apache.http.util.Args;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

/**
 * Add an {@code Accept-Encoding} header to the outgoing request via {@link RequestAcceptEncoding}.
 * 
 * @config apache-http-accept-encoding
 */
@XStreamAlias("apache-http-accept-encoding")
public class AcceptEncoding implements RequestInterceptorBuilder {

  @XStreamImplicit(itemFieldName = "accept-encoding")
  private List<String> acceptEncodings;

  public AcceptEncoding() {

  }

  public AcceptEncoding(String... list) {
    this(new ArrayList<String>(Arrays.asList(list)));
  }

  public AcceptEncoding(List<String> list) {
    this();
    setAcceptEncodings(list);
  }

  public List<String> getAcceptEncodings() {
    return acceptEncodings;
  }

  public void setAcceptEncodings(List<String> acceptEncodings) {
    this.acceptEncodings = Args.notNull(acceptEncodings, "acceptEncodings");
  }

  @Override
  public HttpRequestInterceptor build() {
    return new RequestAcceptEncoding(getAcceptEncodings());
  }

}

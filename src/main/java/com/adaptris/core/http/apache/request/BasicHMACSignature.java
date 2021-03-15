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

import java.util.List;
import javax.validation.constraints.NotBlank;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpContext;
import com.adaptris.core.util.Args;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Attempts to add an HMAC signature to the outgoing request.
 * <p>
 * Note that when using HMAC signatures the {@code 'StringToSign'} may be different for each server implementation. In our case we
 * are simply using <pre>
 * {@code HTTP-Verb + "\n" +
          Path + \n;
          HttpHeader:HeaderValue + "\n" +
          HttpHeaderN:HeaderValue + "\n"
 *
 * }
 * </pre> as the string to sign; HttpHeaders are configured via {@link #setHeaders(List)}. The resulting signature is added as as
 * header against the specified {@code targetHeader} in the form {@code identity:Signature}.
 * </p>
 * <p>
 * If needs be you can create your own custom implementation by extending {@link HMACSignatureImpl} directly.
 * </p>
 *
 * @config apache-http-basic-hmac-signature
 */
@XStreamAlias("apache-http-basic-hmac-signature")
public class BasicHMACSignature extends HMACSignatureImpl {
  @NotBlank
  private String identity;

  public String getIdentity() {
    return identity;
  }

  public void setIdentity(String identity) {
    this.identity = Args.notBlank(identity, "identity");
  }

  public <T extends BasicHMACSignature> T withIdentity(String s) {
    setIdentity(s);
    return (T) this;
  }

  @Override
  protected String getStringToSign(HttpRequest request, HttpContext context) {
    String stringToSign = request.getRequestLine().getMethod() + LF;
    stringToSign += request.getRequestLine().getUri() + LF;
    // iterate over all the headers that are configured...
    // If the headers don't exist, then we default to the blank string, because that's "more correct?"
    for (String h : getHeaders()) {
      Header header = request.getFirstHeader(h);
      if (header == null) {
        stringToSign += h + COLON + LF;
      } else {
        stringToSign += h + COLON + header.getValue() + LF;
      }
    }
    return stringToSign;
  }

  @Override
  protected String buildHeader(String hmacSignature) {
    Args.notBlank(getIdentity(), "identity");
    return getIdentity() + COLON + hmacSignature;
  }

}

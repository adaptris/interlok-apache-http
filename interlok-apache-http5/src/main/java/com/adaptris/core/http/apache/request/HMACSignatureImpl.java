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
import com.adaptris.annotation.InputFieldDefault;
import com.adaptris.annotation.InputFieldHint;
import com.adaptris.core.http.HttpConstants;
import com.adaptris.core.util.Args;
import com.adaptris.interlok.resolver.ExternalResolver;
import com.adaptris.security.exc.PasswordException;
import com.adaptris.security.password.Password;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.protocol.HttpContext;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * Base class for building a HMAC when doing HTTP requests.
 *
 */
public abstract class HMACSignatureImpl implements RequestInterceptorBuilder {
  protected static final String LF = "\n";
  protected static final String COLON = ":";

  /**
   * The encoding to use on the resulting signature.
   *
   */
  public static enum Encoding {
    /**
     * Turn each byte into its hex representation.
     *
     */
    HEX() {
      @Override
      public String encode(byte[] b) {
        return Hex.encodeHexString(b);
      }
    },
    /**
     * Turn it into a Base64 string.
     *
     */
    BASE64() {
      @Override
      public String encode(byte[] b) {
        return Base64.getEncoder().encodeToString(b);
      }
    };
    public abstract String encode(byte[] b);
  };

  /**
   * The algorithm to use when creating the message authentication code.
   *
   */
  public static enum Algorithm {
    HMAC_MD5() {
      @Override
      public byte[] digest(String key, String valueToDigest) {
        return HmacUtils.hmacMd5(key, valueToDigest);
      }
    },
    HMAC_SHA1() {
      @Override
      public byte[] digest(String key, String valueToDigest) {
        return HmacUtils.hmacSha1(key, valueToDigest);
      }
    },
    HMAC_SHA256() {
      @Override
      public byte[] digest(String key, String valueToDigest) {
        return HmacUtils.hmacSha256(key, valueToDigest);
      }
    },
    HMAC_SHA384() {
      @Override
      public byte[] digest(String key, String valueToDigest) {
        return HmacUtils.hmacSha384(key, valueToDigest);
      }
    },
    HMAC_SHA512() {
      @Override
      public byte[] digest(String key, String valueToDigest) {
        return HmacUtils.hmacSha256(key, valueToDigest);
      }
    };

    public abstract byte[] digest(String key, String valueToDigest);
  };

  @XStreamImplicit(itemFieldName = "header")
  @NotNull
  @AutoPopulated
  private List<String> headers;

  @InputFieldHint(style = "PASSWORD", external = true)
  @NotBlank
  private String secretKey;
  @InputFieldDefault(value = HttpConstants.AUTHORIZATION)
  private String targetHeader;
  @InputFieldDefault(value = "BASE64")
  @NotNull
  @AutoPopulated
  private Encoding encoding;
  @InputFieldDefault(value = "HMAC_SHA256")
  @NotNull
  @AutoPopulated
  private Algorithm hmacAlgorithm;

  protected HMACSignatureImpl() {
    setHmacAlgorithm(Algorithm.HMAC_SHA256);
    setEncoding(Encoding.BASE64);
    setHeaders(new ArrayList());
  }

  public List<String> getHeaders() {
    return headers;
  }

  public void setHeaders(List<String> list) {
    headers = Args.notNull(list, "headers");
  }

  public <T extends HMACSignatureImpl> T withHeaders(List<String> list) {
    setHeaders(list);
    return (T) this;
  }

  public <T extends HMACSignatureImpl> T withHeaders(String... list) {
    return withHeaders(new ArrayList<String>(Arrays.asList(list)));
  }

  public String getTargetHeader() {
    return targetHeader;
  }

  public void setTargetHeader(String targetHeader) {
    this.targetHeader = targetHeader;
  }

  public <T extends HMACSignatureImpl> T withTargetHeader(String s) {
    setTargetHeader(s);
    return (T) this;
  }

  protected String targetHeader() {
    return StringUtils.defaultIfBlank(getTargetHeader(), HttpConstants.AUTHORIZATION);
  }

  public Encoding getEncoding() {
    return encoding;
  }

  public void setEncoding(Encoding encoding) {
    this.encoding = Args.notNull(encoding, "encoding");
  }

  public <T extends HMACSignatureImpl> T withEncoding(Encoding s) {
    setEncoding(s);
    return (T) this;
  }

  public Algorithm getHmacAlgorithm() {
    return hmacAlgorithm;
  }

  public void setHmacAlgorithm(Algorithm algorithm) {
    hmacAlgorithm = Args.notNull(algorithm, "algorithm");
  }

  public <T extends HMACSignatureImpl> T withHmacAlgorithm(Algorithm s) {
    setHmacAlgorithm(s);
    return (T) this;
  }

  public String getSecretKey() {
    return secretKey;
  }

  public void setSecretKey(String key) {
    secretKey = Args.notBlank(key, "secretKey");
  }

  public <T extends HMACSignatureImpl> T withSecretKey(String s) {
    setSecretKey(s);
    return (T) this;
  }

  protected String secretKey() {
    try {
      Args.notBlank(getSecretKey(), "secretKey");
      return Password.decode(ExternalResolver.resolve(getSecretKey()));
    } catch (PasswordException e) {
      throw new RuntimeException(e);
    }
  }

  protected String buildHeader(HttpRequest request, HttpContext context) {
    String hmac = getEncoding().encode(getHmacAlgorithm().digest(secretKey(), getStringToSign(request, context)));
    return buildHeader(hmac);
  }

  protected abstract String buildHeader(String hmacSignature);

  protected abstract String getStringToSign(HttpRequest request, HttpContext context);

  @Override
  public HttpRequestInterceptor build() {
    return (request, entity, context) ->
        request.addHeader(targetHeader(), buildHeader(request, context));
  }

}

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
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;

/**
 * Base class for building a HMAC when doing HTTP requests.
 *
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class HMACSignatureImpl implements RequestInterceptorBuilder {
  protected static final String LF = "\n";
  protected static final String COLON = ":";

  /**
   * The encoding to use on the resulting signature.
   *
   */
  public enum Encoding {
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
  }

  /**
   * The algorithm to use when creating the message authentication code.
   *
   */
  public enum Algorithm {
    HMAC_MD5() {
      @Override
      public byte[] digest(String key, String valueToDigest) {
        return new HmacUtils(HmacAlgorithms.HMAC_MD5, key).hmac(valueToDigest);
      }
    },
    HMAC_SHA1() {
      @Override
      public byte[] digest(String key, String valueToDigest) {
        return new HmacUtils(HmacAlgorithms.HMAC_SHA_1, key).hmac(valueToDigest);
      }
    },
    HMAC_SHA256() {
      @Override
      public byte[] digest(String key, String valueToDigest) {
        return new HmacUtils(HmacAlgorithms.HMAC_SHA_256, key).hmac(valueToDigest);
      }
    },
    HMAC_SHA384() {
      @Override
      public byte[] digest(String key, String valueToDigest) {
        return new HmacUtils(HmacAlgorithms.HMAC_SHA_384, key).hmac(valueToDigest);
      }
    },
    HMAC_SHA512() {
      @Override
      public byte[] digest(String key, String valueToDigest) {
        return new HmacUtils(HmacAlgorithms.HMAC_SHA_256, key).hmac(valueToDigest);
      }
    };

    public abstract byte[] digest(String key, String valueToDigest);
  }

  /** The headers to use for HMAC Generation.
   *
   */
  @XStreamImplicit(itemFieldName = "header")
  @NotNull(message="Headers to use for the HMAC should not be null, an empty list is acceptable")
  @AutoPopulated
  @Setter
  @Getter
  private List<String> headers = new ArrayList<>();

  /** The secret key to use for HMAC generation.
   *
   */
  @InputFieldHint(style = "PASSWORD", external = true)
  @NotBlank(message="The secret key for the HMAC should not be blank")
  @Setter
  @Getter
  private String secretKey;

  /** The target header to attach the resulting HMAC to
   *
   */
  @InputFieldDefault(value = HttpConstants.AUTHORIZATION)
  @Setter
  @Getter
  private String targetHeader;

  /** The encoding used for the HMAC.
   *
   */
  @InputFieldDefault(value = "BASE64")
  @NotNull(message="Encoding for the HMAC may not be null")
  @Setter
  @Getter
  private Encoding encoding;

  /** The algorithm to use for the HMAC.
   *
   */
  @InputFieldDefault(value = "HMAC_SHA256")
  @NotNull
  @AutoPopulated
  @Setter
  @Getter
  private Algorithm hmacAlgorithm;

  @SuppressWarnings("unchecked")
  public <T extends HMACSignatureImpl> T withHeaders(List<String> list) {
    setHeaders(list);
    return (T) this;
  }

  public <T extends HMACSignatureImpl> T withHeaders(String... list) {
    return withHeaders(new ArrayList<>(List.of(list)));
  }

  @SuppressWarnings("unchecked")
  public <T extends HMACSignatureImpl> T withTargetHeader(String s) {
    setTargetHeader(s);
    return (T) this;
  }

  @SuppressWarnings("unchecked")
  public <T extends HMACSignatureImpl> T withEncoding(Encoding s) {
    setEncoding(s);
    return (T) this;
  }

  @SuppressWarnings("unchecked")
  public <T extends HMACSignatureImpl> T withHmacAlgorithm(Algorithm s) {
    setHmacAlgorithm(s);
    return (T) this;
  }

  @SuppressWarnings("unchecked")
  public <T extends HMACSignatureImpl> T withSecretKey(String s) {
    setSecretKey(s);
    return (T) this;
  }


  protected String targetHeader() {
    return StringUtils.defaultIfBlank(getTargetHeader(), HttpConstants.AUTHORIZATION);
  }

  protected Algorithm hmacAlgorithm() {
    return ObjectUtils.defaultIfNull(getHmacAlgorithm(), Algorithm.HMAC_SHA256);
  }

  protected Encoding encoding() {
    return ObjectUtils.defaultIfNull(getEncoding(), Encoding.BASE64);
  }

  @SneakyThrows(PasswordException.class)
  protected String secretKey() {
    return Password.decode(ExternalResolver.resolve(Args.notBlank(getSecretKey(), "secretKey")));
  }

  protected String buildHeader(HttpRequest request, HttpContext context) {
    String hmac = encoding().encode(hmacAlgorithm().digest(secretKey(), getStringToSign(request, context)));
    return buildHeader(hmac);
  }

  protected abstract String buildHeader(String hmacSignature);

  protected abstract String getStringToSign(HttpRequest request, HttpContext context);

  @Override
  public HttpRequestInterceptor build() {
    return (request, context) -> request.addHeader(targetHeader(), buildHeader(request, context));
  }

}
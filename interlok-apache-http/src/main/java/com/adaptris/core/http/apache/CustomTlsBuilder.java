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
package com.adaptris.core.http.apache;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.annotation.InputFieldDefault;
import com.adaptris.core.security.PrivateKeyPasswordProvider;
import com.adaptris.security.exc.AdaptrisSecurityException;
import com.adaptris.security.keystore.ConfiguredKeystore;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.validation.Valid;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;

/**
 * {@link HttpClientBuilderConfigurator} implementation that allows you to customise keystores etc.
 *
 * @config custom-tls-apache-http-client-builder
 */
@DisplayOrder(order =
    {
        "keystore", "privateKeyPassword", "truststore", "hostnameVerification", "tlsVersions", "cipherSuites"
    })
@XStreamAlias("custom-tls-apache-http-client-builder")
@NoArgsConstructor
public class CustomTlsBuilder implements HttpClientBuilderConfigurator {
  public enum HostnameVerification {
    /**
     * No Hostname Verification (dangerous).
     */
    NONE(new NoopHostnameVerifier()),
    /**
     * Standard Hostname verification
     */
    STANDARD(SSLConnectionSocketFactory.getDefaultHostnameVerifier());

    private final HostnameVerifier myVerifier;

    HostnameVerification(HostnameVerifier v) {
      myVerifier = v;
    }

    public HostnameVerifier getVerifier() {
      return myVerifier;
    }

  }

  /**
   * The list of tls versions that will be supported (comma separated)
   */
  @Getter
  @Setter
  @AdvancedConfig
  private String tlsVersions;
  /**
   * The cipher suites to support (comma separated)
   */
  @Getter
  @Setter
  @AdvancedConfig
  private String cipherSuites;
  /** How we want to verify the hostname.
   * <p>Defaults to {@link HostnameVerification#STANDARD} if not explicitly specified.</p>
   */
  @Getter
  @Setter
  @AdvancedConfig
  @InputFieldDefault(value = "STANDARD")
  private HostnameVerification hostnameVerification;
  /** The truststore used with TLS
   *
   */
  @Getter
  @Setter
  @Valid
  private ConfiguredKeystore truststore;
  /** The keystore used with TLS.
   *
   */
  @Getter
  @Setter
  @Valid
  private ConfiguredKeystore keystore;
  /** The private key password.
   *
   */
  @Getter
  @Setter
  @Valid
  private PrivateKeyPasswordProvider privateKeyPassword;
  /** Whether or not to trust self signed certificates.
   *  <p>Defaults to false if not explicitly configured.</p>
   */
  @InputFieldDefault(value = "false")
  @Getter
  @Setter
  private Boolean trustSelfSigned;

  private static String[] asArray(String s) {
    if (isEmpty(s)) {
      return null;
    }
    return s.split("\\s*,\\s*", -1);
  }

  @Override
  public HttpClientBuilder configure(HttpClientBuilder builder) throws Exception {
    SSLContext sslcontext = configure(SSLContexts.custom()).build();
    SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext, asArray(getTlsVersions()),
        asArray(getCipherSuites()), hostnameVerification().getVerifier());
    builder.setSSLSocketFactory(sslsf);
    return builder;
  }

  private SSLContextBuilder configure(SSLContextBuilder builder)
      throws GeneralSecurityException, AdaptrisSecurityException, IOException {
    if (getKeystore() != null) {
      KeyStore actual = getKeystore().asKeystoreProxy().getKeystore();
      if (getPrivateKeyPassword() == null) {
        throw new AdaptrisSecurityException("Keystore configured; no key password");
      }
      builder.loadKeyMaterial(actual, getPrivateKeyPassword().retrievePrivateKeyPassword());
    }
    if (getTruststore() != null) {
      KeyStore actual = getTruststore().asKeystoreProxy().getKeystore();
      builder.loadTrustMaterial(actual, trustStrategy());
    }
    return builder;
  }

  @SuppressWarnings("unchecked")
  public <T extends CustomTlsBuilder> T withTlsVersions(String s) {
    setTlsVersions(s);
    return (T) this;
  }

  @SuppressWarnings("unchecked")
  public <T extends CustomTlsBuilder> T withCipherSuites(String s) {
    setCipherSuites(s);
    return (T) this;
  }

  @SuppressWarnings("unchecked")
  public <T extends CustomTlsBuilder> T withHostnameVerification(HostnameVerification s) {
    setHostnameVerification(s);
    return (T) this;
  }

  protected HostnameVerification hostnameVerification() {
    return getHostnameVerification() != null ? getHostnameVerification() : HostnameVerification.STANDARD;
  }

  protected boolean trustSelfSigned() {
    return BooleanUtils.toBooleanDefaultIfNull(getTrustSelfSigned(), false);
  }

  @SuppressWarnings("unchecked")
  public <T extends CustomTlsBuilder> T withTrustSelfSigned(Boolean s) {
    setTrustSelfSigned(s);
    return (T) this;
  }

  protected TrustStrategy trustStrategy() {
    return trustSelfSigned() ? TrustSelfSignedStrategy.INSTANCE : null;
  }

  @SuppressWarnings("unchecked")
  public <T extends CustomTlsBuilder> T withPrivateKeyPassword(PrivateKeyPasswordProvider s) {
    setPrivateKeyPassword(s);
    return (T) this;
  }

  @SuppressWarnings("unchecked")
  public <T extends CustomTlsBuilder> T withTrustStore(ConfiguredKeystore s) {
    setTruststore(s);
    return (T) this;
  }

  @SuppressWarnings("unchecked")
  public <T extends CustomTlsBuilder> T withKeystore(ConfiguredKeystore s) {
    setKeystore(s);
    return (T) this;
  }
}

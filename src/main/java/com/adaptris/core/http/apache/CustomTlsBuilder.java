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

import static org.apache.commons.lang.StringUtils.isEmpty;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.validation.Valid;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;

import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.core.security.PrivateKeyPasswordProvider;
import com.adaptris.security.exc.AdaptrisSecurityException;
import com.adaptris.security.keystore.ConfiguredKeystore;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * {@link HttpClientBuilderConfigurator} implementation that allows you to customise keystores etc.
 * 
 * @config custom-tls-apache-http-client-builder
 *
 */
@DisplayOrder(order =
{
    "keystore", "privateKeyPassword", "truststore", "hostnameVerification", "tlsVersions", "cipherSuites"
})
@XStreamAlias("custom-tls-apache-http-client-builder")
public class CustomTlsBuilder implements HttpClientBuilderConfigurator {
  public static enum HostnameVerification {
    /**
     * No Hostname Verification (dangerous).
     * 
     */
    NONE(new NoopHostnameVerifier()),
    /**
     * Standard Hostname verification
     * 
     */
    STANDARD(SSLConnectionSocketFactory.getDefaultHostnameVerifier());

    private HostnameVerifier myVerifier;

    private HostnameVerification(HostnameVerifier v) {
      myVerifier = v;
    }

    public HostnameVerifier getVerifier() {
      return myVerifier;
    }

  }

  @AdvancedConfig
  private String tlsVersions;
  @AdvancedConfig
  private String cipherSuites;
  @AdvancedConfig
  private HostnameVerification hostnameVerification;
  @Valid
  private ConfiguredKeystore truststore;
  @Valid
  private ConfiguredKeystore keystore;
  @Valid
  private PrivateKeyPasswordProvider privateKeyPassword;
  private Boolean trustSelfSigned;

  @Override
  public HttpClientBuilder configure(HttpClientBuilder builder) throws Exception {
    HttpClientBuilder result = builder;
    SSLContext sslcontext = configure(SSLContexts.custom()).build();
    SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext, asArray(getTlsVersions()),
        asArray(getCipherSuites()), hostnameVerification().getVerifier());
    result.setSSLSocketFactory(sslsf);
    return result;
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

  private static String[] asArray(String s) {
    if (isEmpty(s)) {
      return null;
    }
    return s.split("\\s*,\\s*", -1);
  }

  public String getTlsVersions() {
    return tlsVersions;
  }

  /**
   * 
   * Set the list of tls versions that will be supported (comma separated)
   * 
   * @param tls the tls versions to support; default is null
   */
  public void setTlsVersions(String tls) {
    this.tlsVersions = tls;
  }

  public <T extends CustomTlsBuilder> T withTlsVersions(String s) {
    setTlsVersions(s);
    return (T) this;
  }

  /**
   * @return the cipherSuites
   */
  public String getCipherSuites() {
    return cipherSuites;
  }

  /**
   * Set the cipher suites to support.
   * 
   * @param ciphers the cipherSuites to support; default is null
   */
  public void setCipherSuites(String ciphers) {
    this.cipherSuites = ciphers;
  }

  public <T extends CustomTlsBuilder> T withCipherSuites(String s) {
    setCipherSuites(s);
    return (T) this;
  }

  /**
   * @return the hostnameVerification
   */
  public HostnameVerification getHostnameVerification() {
    return hostnameVerification;
  }

  /**
   * @param hostnameVerification the hostnameVerification to set
   */
  public void setHostnameVerification(HostnameVerification hostnameVerification) {
    this.hostnameVerification = hostnameVerification;
  }

  public <T extends CustomTlsBuilder> T withHostnameVerification(HostnameVerification s) {
    setHostnameVerification(s);
    return (T) this;
  }

  protected HostnameVerification hostnameVerification() {
    return getHostnameVerification() != null ? getHostnameVerification() : HostnameVerification.STANDARD;
  }

  public Boolean getTrustSelfSigned() {
    return trustSelfSigned;
  }

  /**
   * Do we trust self-signed certificates or not.
   * 
   * @param b if set to true the {@code TrustSelfSignedStrategy.INSTANCE} is used; default null/false.
   */
  public void setTrustSelfSigned(Boolean b) {
    this.trustSelfSigned = b;
  }

  protected boolean trustSelfSigned() {
    return BooleanUtils.toBooleanDefaultIfNull(getTrustSelfSigned(), false);
  }

  public <T extends CustomTlsBuilder> T withTrustSelfSigned(Boolean s) {
    setTrustSelfSigned(s);
    return (T) this;
  }

  protected TrustStrategy trustStrategy() {
    return trustSelfSigned() ? TrustSelfSignedStrategy.INSTANCE : null;
  }

  public PrivateKeyPasswordProvider getPrivateKeyPassword() {
    return privateKeyPassword;
  }

  public void setPrivateKeyPassword(PrivateKeyPasswordProvider pk) {
    this.privateKeyPassword = pk;
  }

  public <T extends CustomTlsBuilder> T withPrivateKeyPassword(PrivateKeyPasswordProvider s) {
    setPrivateKeyPassword(s);
    return (T) this;
  }

  public ConfiguredKeystore getTruststore() {
    return truststore;
  }

  /**
   * Set the trustore to be used.
   * 
   */
  public void setTruststore(ConfiguredKeystore truststore) {
    this.truststore = truststore;
  }

  public <T extends CustomTlsBuilder> T withTrustStore(ConfiguredKeystore s) {
    setTruststore(s);
    return (T) this;
  }

  public ConfiguredKeystore getKeystore() {
    return keystore;
  }

  /**
   * Set the keystore to be used.
   * 
   */
  public void setKeystore(ConfiguredKeystore keystore) {
    this.keystore = keystore;
  }

  public <T extends CustomTlsBuilder> T withKeystore(ConfiguredKeystore s) {
    setKeystore(s);
    return (T) this;
  }
}

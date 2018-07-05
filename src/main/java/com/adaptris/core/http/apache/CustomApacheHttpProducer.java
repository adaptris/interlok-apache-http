package com.adaptris.core.http.apache;

import static org.apache.commons.lang.StringUtils.isEmpty;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.validation.Valid;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;

import com.adaptris.annotation.AdapterComponent;
import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.core.NullConnection;
import com.adaptris.core.ProduceDestination;
import com.adaptris.core.security.PrivateKeyPasswordProvider;
import com.adaptris.security.exc.AdaptrisSecurityException;
import com.adaptris.security.keystore.ConfiguredKeystore;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Apache HTTP client producer with customised TLS/SSL settings.
 * 
 * @config custom-tls-apache-http-producer
 */
@XStreamAlias("custom-tls-apache-http-producer")
@AdapterComponent
@ComponentProfile(summary = "Apache HTTP Client with custom TLS/SSL parameters", tag = "producer,http,https", metadata =
{
    "adphttpresponse"

}, recommended =
{
    NullConnection.class
}, author = "Adaptris Ltd")
@DisplayOrder(order = {"username", "password", "authenticator", "httpProxy", "allowRedirect", "ignoreServerResponseCode",
    "methodProvider", "contentTypeProvider", "requestHeaderProvider", "responseHeaderHandler", "responseHandlerFactory", "keystore",
    "privateKeyPassword", "truststore", "hostnameVerification", "tlsVersions", "cipherSuites"
})
public class CustomApacheHttpProducer extends ApacheHttpProducer {

  public enum HostnameVerification {
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

  public CustomApacheHttpProducer() {
    super();
  }

  public CustomApacheHttpProducer(ProduceDestination d) {
    super(d);
  }


  protected HttpClientBuilder customise(HttpClientBuilder builder) throws Exception {
    HttpClientBuilder result = super.customise(builder);
    SSLContext sslcontext = configure(SSLContexts.custom()).build();
    SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext, asArray(getTlsVersions()),
        asArray(getCipherSuites()),
        hostnameVerification().getVerifier());
    result.setSSLSocketFactory(sslsf);
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

  HostnameVerification hostnameVerification() {
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

  boolean trustSelfSigned() {
    return getTrustSelfSigned() != null ? getTrustSelfSigned().booleanValue() : false;
  }

  TrustStrategy trustStrategy() {
    return trustSelfSigned() ? TrustSelfSignedStrategy.INSTANCE : null;
  }

  public PrivateKeyPasswordProvider getPrivateKeyPassword() {
    return privateKeyPassword;
  }

  public void setPrivateKeyPassword(PrivateKeyPasswordProvider pk) {
    this.privateKeyPassword = pk;
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
}

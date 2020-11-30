package com.adaptris.core.http.apache;

import javax.validation.Valid;
import org.apache.http.impl.client.HttpClientBuilder;
import com.adaptris.annotation.AdapterComponent;
import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.validation.constraints.ConfigDeprecated;
import com.adaptris.core.CoreException;
import com.adaptris.core.NullConnection;
import com.adaptris.core.http.apache.CustomTlsBuilder.HostnameVerification;
import com.adaptris.core.security.PrivateKeyPasswordProvider;
import com.adaptris.security.keystore.ConfiguredKeystore;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Apache HTTP client producer with customised TLS/SSL settings.
 *
 * @config custom-tls-apache-http-producer
 * @deprecated since 3.8.0 you can achieve the same thing with a {@link CustomTlsBuilder} with {@link ApacheHttpProducer}.
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
@DisplayOrder(order = {"url", "authenticator",
    "ignoreServerResponseCode",
    "methodProvider", "contentTypeProvider", "requestHeaderProvider", "responseHeaderHandler", "responseHandlerFactory", "keystore",
    "privateKeyPassword", "truststore", "hostnameVerification", "tlsVersions", "cipherSuites", "clientConfig"
})
@Deprecated
@ConfigDeprecated(removalVersion = "3.12.0", groups = Deprecated.class)
public class CustomApacheHttpProducer extends ApacheHttpProducer {

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

  private boolean warningLogged = false;

  public CustomApacheHttpProducer() {
    super();
  }

  @Override
  public void prepare() throws CoreException {
    if (!warningLogged) {
      log.warn("{} is deprecated, use {} with {} instead", this.getClass().getSimpleName(),
          ApacheHttpProducer.class.getName(), CustomTlsBuilder.class.getName());
      warningLogged = true;
    }
    super.prepare();
  }

  @Override
  protected HttpClientBuilder customise(HttpClientBuilder builder) throws Exception {
    return new CustomTlsBuilder().withCipherSuites(getCipherSuites()).withHostnameVerification(getHostnameVerification()).withKeystore(getKeystore())
        .withPrivateKeyPassword(getPrivateKeyPassword()).withTlsVersions(getTlsVersions()).withTrustSelfSigned(getTrustSelfSigned())
        .withTrustStore(getTruststore()).configure(builder);
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
    tlsVersions = tls;
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
    cipherSuites = ciphers;
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


  public Boolean getTrustSelfSigned() {
    return trustSelfSigned;
  }

  /**
   * Do we trust self-signed certificates or not.
   *
   * @param b if set to true the {@code TrustSelfSignedStrategy.INSTANCE} is used; default null/false.
   */
  public void setTrustSelfSigned(Boolean b) {
    trustSelfSigned = b;
  }

  public PrivateKeyPasswordProvider getPrivateKeyPassword() {
    return privateKeyPassword;
  }

  public void setPrivateKeyPassword(PrivateKeyPasswordProvider pk) {
    privateKeyPassword = pk;
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

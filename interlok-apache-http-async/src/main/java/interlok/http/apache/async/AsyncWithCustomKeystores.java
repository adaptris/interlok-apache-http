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
package interlok.http.apache.async;

import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.annotation.InputFieldDefault;
import com.adaptris.core.http.apache.HttpClientBuilderConfigurator;
import com.adaptris.core.security.PrivateKeyPasswordProvider;
import com.adaptris.security.exc.AdaptrisSecurityException;
import com.adaptris.security.keystore.ConfiguredKeystore;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Optional;
import javax.net.ssl.SSLContext;
import javax.validation.Valid;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;

/**
 * {@link HttpClientBuilderConfigurator} implementation that allows you to customise keystores etc.
 *
 * @config apache-http-async-client-builder-with-custom-keystores
 */
@DisplayOrder(order = {"truststore", "keystore", "privateKeyPassword"})
@XStreamAlias("apache-http-async-client-builder-with-custom-keystores")
@ComponentProfile(since = "4.5.0", summary = "Allows you to configure custom keystores when building the HttpAsyncClient")
@NoArgsConstructor
public class AsyncWithCustomKeystores implements HttpAsyncClientBuilderConfig {

  /**
   * The truststore used with TLS
   */
  @Getter
  @Setter
  @Valid
  private ConfiguredKeystore truststore;
  /**
   * The keystore used with TLS.
   */
  @Getter
  @Setter
  @Valid
  private ConfiguredKeystore keystore;
  /**
   * The private key password.
   */
  @Getter
  @Setter
  @Valid
  private PrivateKeyPasswordProvider privateKeyPassword;
  /**
   * Whether or not to trust self signed certificates.
   * <p>Defaults to false if not explicitly configured.</p>
   */
  @InputFieldDefault(value = "false")
  @Getter
  @Setter
  private Boolean trustSelfSigned;

  @Override
  public HttpAsyncClientBuilder configure(HttpAsyncClientBuilder builder) throws Exception {
    SSLContext sslcontext = configure(SSLContexts.custom()).build();
    return builder.setSSLContext(sslcontext);
  }

  private SSLContextBuilder configure(SSLContextBuilder builder)
      throws GeneralSecurityException, AdaptrisSecurityException, IOException {
    if (getKeystore() != null) {
      KeyStore actual = getKeystore().asKeystoreProxy().getKeystore();
      builder.loadKeyMaterial(actual, Optional.of(getPrivateKeyPassword()).get().retrievePrivateKeyPassword());
    }
    if (getTruststore() != null) {
      KeyStore actual = getTruststore().asKeystoreProxy().getKeystore();
      builder.loadTrustMaterial(actual, trustStrategy());
    }
    return builder;
  }

  private boolean trustSelfSigned() {
    return BooleanUtils.toBooleanDefaultIfNull(getTrustSelfSigned(), false);
  }

  @SuppressWarnings("unchecked")
  public <T extends AsyncWithCustomKeystores> T withTrustSelfSigned(Boolean s) {
    setTrustSelfSigned(s);
    return (T) this;
  }

  protected TrustStrategy trustStrategy() {
    return trustSelfSigned() ? TrustSelfSignedStrategy.INSTANCE : null;
  }

  @SuppressWarnings("unchecked")
  public <T extends AsyncWithCustomKeystores> T withPrivateKeyPassword(PrivateKeyPasswordProvider s) {
    setPrivateKeyPassword(s);
    return (T) this;
  }

  @SuppressWarnings("unchecked")
  public <T extends AsyncWithCustomKeystores> T withTrustStore(ConfiguredKeystore s) {
    setTruststore(s);
    return (T) this;
  }

  @SuppressWarnings("unchecked")
  public <T extends AsyncWithCustomKeystores> T withKeystore(ConfiguredKeystore s) {
    setKeystore(s);
    return (T) this;
  }
}

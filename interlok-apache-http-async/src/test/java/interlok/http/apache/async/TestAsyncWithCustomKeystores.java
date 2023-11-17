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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.junit.jupiter.api.Test;

import com.adaptris.core.security.ConfiguredPrivateKeyPasswordProvider;
import com.adaptris.interlok.junit.scaffolding.BaseCase;
import com.adaptris.security.keystore.ConfiguredUrl;

public class TestAsyncWithCustomKeystores extends BaseCase {

  protected static final String KEY_PASSWORD = "keystore.password";
  protected static final String KEY_KEYSTORE_URL = "keystore.url";
  protected static final String KEY_TRUSTSTORE_URL = "trust.url";
  protected static final String KEY_TRUSTSTORE_PASSWORD = "trust.password";

  @Test
  public void testBuilder() throws Exception {
    HttpAsyncClientBuilder httpClientBuilder = HttpAsyncClientBuilder.create();
    AsyncWithCustomKeystores builder = new AsyncWithCustomKeystores();
    assertEquals(httpClientBuilder, builder.configure(httpClientBuilder));
  }

  @Test
  public void testBuilder_WithTrustStore() throws Exception {
    String truststoreURL = PROPERTIES.getProperty(KEY_TRUSTSTORE_URL);
    String truststorePassword = PROPERTIES.getProperty(KEY_TRUSTSTORE_PASSWORD);

    AsyncWithCustomKeystores http = new AsyncWithCustomKeystores().withTrustSelfSigned(true)
        .withTrustStore(new ConfiguredUrl(truststoreURL, truststorePassword));
    assertNotNull(http.configure(HttpAsyncClientBuilder.create()));
  }

  @Test
  public void testBuilder_WithKeystore() throws Exception {
    String keystorePassword = PROPERTIES.getProperty(KEY_PASSWORD);
    String keystoreURL = PROPERTIES.getProperty(KEY_KEYSTORE_URL);

    AsyncWithCustomKeystores http = new AsyncWithCustomKeystores()
        .withPrivateKeyPassword(new ConfiguredPrivateKeyPasswordProvider(keystorePassword))
        .withKeystore(new ConfiguredUrl(keystoreURL, keystorePassword));
    assertNotNull(http.configure(HttpAsyncClientBuilder.create()));
  }

  @Test
  public void testBuilder_WithKeystores() throws Exception {
    String keystorePassword = PROPERTIES.getProperty(KEY_PASSWORD);
    String keystoreURL = PROPERTIES.getProperty(KEY_KEYSTORE_URL);

    String truststoreURL = PROPERTIES.getProperty(KEY_TRUSTSTORE_URL);
    String truststorePassword = PROPERTIES.getProperty(KEY_TRUSTSTORE_PASSWORD);

    AsyncWithCustomKeystores http = new AsyncWithCustomKeystores()
        .withPrivateKeyPassword(new ConfiguredPrivateKeyPasswordProvider(keystorePassword))
        .withTrustStore(new ConfiguredUrl(truststoreURL, truststorePassword))
        .withKeystore(new ConfiguredUrl(keystoreURL, keystorePassword));
    assertNotNull(http.configure(HttpAsyncClientBuilder.create()));
  }

  @Test
  public void testBuilder_WithKeystores_NoPassword() throws Exception {
    String keystorePassword = PROPERTIES.getProperty(KEY_PASSWORD);
    String keystoreURL = PROPERTIES.getProperty(KEY_KEYSTORE_URL);

    String truststoreURL = PROPERTIES.getProperty(KEY_TRUSTSTORE_URL);
    String truststorePassword = PROPERTIES.getProperty(KEY_TRUSTSTORE_PASSWORD);

    // Generated from Optional.getPrivateKeyPassword
    assertThrows(NullPointerException.class, () -> {
      AsyncWithCustomKeystores http = new AsyncWithCustomKeystores().withTrustStore(new ConfiguredUrl(truststoreURL, truststorePassword))
          .withKeystore(new ConfiguredUrl(keystoreURL, keystorePassword));
      http.configure(HttpAsyncClientBuilder.create());
    });
  }

}

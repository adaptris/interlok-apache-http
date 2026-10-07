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
package com.adaptris.core.http.apache5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import com.adaptris.core.security.ConfiguredPrivateKeyPasswordProvider;
import com.adaptris.interlok.junit.scaffolding.BaseCase;
import com.adaptris.security.exc.AdaptrisSecurityException;
import com.adaptris.security.keystore.ConfiguredUrl;

class CustomTlsBuilderTest extends BaseCase {

  protected static final String KEY_PASSWORD = "jetty.keystore.password";
  private static final String KEYSTORE_URL = Objects
      .requireNonNull(CustomTlsBuilderTest.class.getResource("/interlok.jks"), "Missing test keystore")
      .toExternalForm() + "?keystoreType=JKS";

  @Test
  void testSetTrustSelfSigned(){
    CustomTlsBuilder p = new CustomTlsBuilder();
    assertFalse(p.trustSelfSigned());
    p.setTrustSelfSigned(Boolean.TRUE);
    assertTrue(p.trustSelfSigned());
    p.setTrustSelfSigned(null);
    assertFalse(p.trustSelfSigned());
  }

  @Test
  void testSetHostnameVerification() {
    CustomTlsBuilder p = new CustomTlsBuilder();
    assertEquals(CustomTlsBuilder.HostnameVerification.STANDARD, p.hostnameVerification());

    p.setHostnameVerification(CustomTlsBuilder.HostnameVerification.NONE);
    assertEquals(CustomTlsBuilder.HostnameVerification.NONE, p.hostnameVerification());
    p.setHostnameVerification(null);
    assertEquals(CustomTlsBuilder.HostnameVerification.STANDARD, p.hostnameVerification());
  }

  @Test
  void testBuilder_WithKeystores() throws Exception {
    String keystorePassword = PROPERTIES.getProperty(KEY_PASSWORD);
    CustomTlsBuilder http = new CustomTlsBuilder();
    http.setHostnameVerification(CustomTlsBuilder.HostnameVerification.NONE);
    http.withPrivateKeyPassword(new ConfiguredPrivateKeyPasswordProvider(keystorePassword));
    http.setTrustSelfSigned(true);
    http.setTruststore(new ConfiguredUrl(KEYSTORE_URL, keystorePassword));
    http.setKeystore(new ConfiguredUrl(KEYSTORE_URL, keystorePassword));
    assertNotNull(http.configure(HttpClients.custom(), 10));
  }

  @Test
  void testBuilder_WithKeystores_NoPassword() {
    String keystorePassword = PROPERTIES.getProperty(KEY_PASSWORD);
    CustomTlsBuilder http = new CustomTlsBuilder();
    http.setHostnameVerification(CustomTlsBuilder.HostnameVerification.NONE);
    http.setTrustSelfSigned(true);
    http.withTrustStore(new ConfiguredUrl(KEYSTORE_URL, keystorePassword));
    http.withKeystore(new ConfiguredUrl(KEYSTORE_URL, keystorePassword));
    assertThrows(AdaptrisSecurityException.class, () -> http.configure(HttpClients.custom(), 10));
  }

  @Test
  void testTrustSelfSigned() {
    CustomTlsBuilder http = new CustomTlsBuilder();
    http.withTrustSelfSigned(false);
    assertNull(http.trustStrategy());
    http.withTrustSelfSigned(true);
    assertNotNull(http.trustStrategy());
  }

  @Test
  void testBuilder_WithTls() throws Exception {
    CustomTlsBuilder http = new CustomTlsBuilder().withTlsVersions("SSLv3,TLSv1.1");
    assertNotNull(http.configure(HttpClients.custom()));
  }

  @Test
  void testBuilder_WithCipherSuites() throws Exception {
    CustomTlsBuilder http = new CustomTlsBuilder()
        .withCipherSuites("TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA3841,TLS_RSA_WITH_AES_256_CBC_SHA256");
    assertNotNull(http.configure(HttpClients.custom()));
  }

}

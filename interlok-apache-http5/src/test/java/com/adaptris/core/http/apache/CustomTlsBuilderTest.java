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

import com.adaptris.core.http.apache.CustomTlsBuilder.HostnameVerification;
import com.adaptris.core.security.ConfiguredPrivateKeyPasswordProvider;
import com.adaptris.interlok.junit.scaffolding.BaseCase;
import com.adaptris.security.keystore.ConfiguredUrl;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CustomTlsBuilderTest extends BaseCase {

  protected static final String KEY_KEYSTORE = "jetty.keystore.material";
  protected static final String KEY_PASSWORD = "jetty.keystore.password";
  protected static final String KEY_KEYSTORE_TYPE = "jetty.keystore.type";
  protected static final String KEY_KEYSTORE_URL = "jetty.keystore.url";
  protected static final String KEY_TRUSTSTORE_URL = "jetty.trust.url";
  protected static final String KEY_TRUSTSTORE_PASSWORD = "jetty.trust.password";

  @Test
  public void testSetTrustSelfSigned() throws Exception {
    CustomTlsBuilder p = new CustomTlsBuilder();
    assertFalse(p.trustSelfSigned());
    p.setTrustSelfSigned(Boolean.TRUE);
    assertTrue(p.trustSelfSigned());
    p.setTrustSelfSigned(null);
    assertFalse(p.trustSelfSigned());
  }

  @Test
  public void testSetHostnameVerification() throws Exception {
    CustomTlsBuilder p = new CustomTlsBuilder();
    assertEquals(HostnameVerification.STANDARD, p.hostnameVerification());

    p.setHostnameVerification(HostnameVerification.NONE);
    assertEquals(HostnameVerification.NONE, p.hostnameVerification());
    p.setHostnameVerification(null);
    assertEquals(HostnameVerification.STANDARD, p.hostnameVerification());
  }

  @Test
  public void testBuilder_WithKeystores() throws Exception {
    String keystore = PROPERTIES.getProperty(KEY_KEYSTORE);
    String keystorePassword = PROPERTIES.getProperty(KEY_PASSWORD);
    String keystoreType = PROPERTIES.getProperty(KEY_KEYSTORE_TYPE);
    String keystoreURL = PROPERTIES.getProperty(KEY_KEYSTORE_URL);

    String truststoreURL = PROPERTIES.getProperty(KEY_TRUSTSTORE_URL);
    String truststorePassword = PROPERTIES.getProperty(KEY_TRUSTSTORE_PASSWORD);
    CustomTlsBuilder http = new CustomTlsBuilder();
    http.setHostnameVerification(HostnameVerification.NONE);
    http.withPrivateKeyPassword(new ConfiguredPrivateKeyPasswordProvider(keystorePassword));
    http.setTrustSelfSigned(true);
    http.setTruststore(new ConfiguredUrl(truststoreURL, truststorePassword));
    http.setKeystore(new ConfiguredUrl(keystoreURL, keystorePassword));
    assertNotNull(http.configure(HttpClients.custom(), 10));
  }

  @Test
  public void testBuilder_WithKeystores_NoPassword() throws Exception {
    String keystore = PROPERTIES.getProperty(KEY_KEYSTORE);
    String keystorePassword = PROPERTIES.getProperty(KEY_PASSWORD);
    String keystoreType = PROPERTIES.getProperty(KEY_KEYSTORE_TYPE);
    String keystoreURL = PROPERTIES.getProperty(KEY_KEYSTORE_URL);

    String truststoreURL = PROPERTIES.getProperty(KEY_TRUSTSTORE_URL);
    String truststorePassword = PROPERTIES.getProperty(KEY_TRUSTSTORE_PASSWORD);
    CustomTlsBuilder http = new CustomTlsBuilder();
    http.setHostnameVerification(HostnameVerification.NONE);
    http.setTrustSelfSigned(true);
    http.withTrustStore(new ConfiguredUrl(truststoreURL, truststorePassword));
    http.withKeystore(new ConfiguredUrl(keystoreURL, keystorePassword));
    try {
      http.configure(HttpClients.custom(), 10);
      fail();
    } catch (Exception expected) {

    }
  }

  @Test
  public void testTrustSelfSigned() throws Exception {
    CustomTlsBuilder http = new CustomTlsBuilder();
    http.withTrustSelfSigned(false);
    assertNull(http.trustStrategy());
    http.withTrustSelfSigned(true);
    assertNotNull(http.trustStrategy());
  }

  @Test
  public void testBuilder_WithTls() throws Exception {
    CustomTlsBuilder http = new CustomTlsBuilder().withTlsVersions("SSLv3,TLSv1.1");
    assertNotNull(http.configure(HttpClients.custom()));
  }

  @Test
  public void testBuilder_WithCipherSuites() throws Exception {
    CustomTlsBuilder http = new CustomTlsBuilder().withCipherSuites(
        "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA3841,TLS_RSA_WITH_AES_256_CBC_SHA256");
    assertNotNull(http.configure(HttpClients.custom()));
  }

}

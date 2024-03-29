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
package com.adaptris.core.http.apache5.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.core5.http.protocol.HttpDateGenerator;
import org.junit.jupiter.api.Test;

import com.adaptris.core.http.HttpConstants;
import com.adaptris.core.http.apache5.request.HMACSignatureImpl.Algorithm;
import com.adaptris.core.http.apache5.request.HMACSignatureImpl.Encoding;

public class BasicHmacSignatureTest {

  @Test
  public void testEnum_Encoding() {
    for (Encoding e : HMACSignatureImpl.Encoding.values()) {
      assertNotNull(e.encode("Hello World".getBytes()));
    }
  }

  @Test
  public void testEnum_HmacAlgorithm() {
    for (Algorithm e : HMACSignatureImpl.Algorithm.values()) {
      assertNotNull(e.digest("hello", "world"));
    }
  }

  @Test
  public void testAlgorithm() {
    BasicHMACSignature hmac = new BasicHMACSignature();
    assertNotNull(hmac.getHmacAlgorithm());
    assertEquals(HMACSignatureImpl.Algorithm.HMAC_SHA256, hmac.getHmacAlgorithm());
    hmac.withHmacAlgorithm(Algorithm.HMAC_MD5);
    assertEquals(HMACSignatureImpl.Algorithm.HMAC_MD5, hmac.getHmacAlgorithm());
  }

  @Test
  public void testEncoding() {
    BasicHMACSignature hmac = new BasicHMACSignature();
    assertNotNull(hmac.getEncoding());
    assertEquals(HMACSignatureImpl.Encoding.BASE64, hmac.getEncoding());
    hmac.withEncoding(HMACSignatureImpl.Encoding.HEX);
    assertEquals(HMACSignatureImpl.Encoding.HEX, hmac.getEncoding());
  }

  @Test
  public void testIdentity() {
    BasicHMACSignature hmac = new BasicHMACSignature().withIdentity("xx");
    assertEquals("xx", hmac.getIdentity());
  }

  @Test
  public void testTargetHeader() {
    BasicHMACSignature hmac = new BasicHMACSignature();
    assertNull(hmac.getTargetHeader());
    assertEquals(HttpConstants.AUTHORIZATION, hmac.targetHeader());
    hmac.withTargetHeader("hmac");
    assertEquals("hmac", hmac.targetHeader());
  }

  @Test
  public void testSecretKey() {
    BasicHMACSignature hmac = new BasicHMACSignature();
    assertNull(hmac.getSecretKey());
    hmac.withSecretKey("XXX");
    assertEquals("XXX", hmac.secretKey());
    hmac.withSecretKey("PW:WillNotDecode");
    assertThrows(RuntimeException.class, () -> hmac.secretKey());
  }

  @Test
  public void testHeaders() {
    BasicHMACSignature hmac = new BasicHMACSignature();
    assertEquals(0, hmac.getHeaders().size());
    hmac.withHeaders("Content-Type", "Date", "X-Content-Encoding");
    assertEquals(3, hmac.getHeaders().size());
  }

  @Test
  public void testBuild() {
    BasicHMACSignature hmac = new BasicHMACSignature().withIdentity("MyAccessKey").withEncoding(Encoding.BASE64)
        .withHmacAlgorithm(Algorithm.HMAC_SHA512).withSecretKey("MySecretKey").withHeaders("Date", "Content-Type").withTargetHeader("hmac");
    assertNotNull(hmac.build());
  }

  @Test
  public void testStringToSign() {
    BasicHMACSignature hmac = new BasicHMACSignature();
    hmac.withHeaders("Content-Type", "Date", "X-Content-Encoding");
    HttpGet get = new HttpGet("http://localhost:8080/index.html");
    String currentDate = HttpDateGenerator.INSTANCE.getCurrentDate();
    get.addHeader("Content-Type", "text/xml");
    get.addHeader("Date", currentDate);
    String stringToSign = hmac.getStringToSign(get, null);
    assertTrue(stringToSign.contains(currentDate));
    assertTrue(stringToSign.contains("text/xml"));
    // no x-content-encoding...
    assertTrue(stringToSign.contains("X-Content-Encoding:\n"));
  }

  @Test
  public void testBuildHeader() {
    BasicHMACSignature hmac = new BasicHMACSignature().withIdentity("MyAccessKey").withEncoding(Encoding.BASE64)
        .withHmacAlgorithm(Algorithm.HMAC_SHA512).withSecretKey("MySecretKey").withHeaders("Date", "Content-Type").withTargetHeader("hmac");
    HttpGet get = new HttpGet("http://localhost:8080/index.html");
    String header = hmac.buildHeader(get, null);
    assertTrue(header.startsWith("MyAccessKey:"));
    System.out.println(header);
  }

}

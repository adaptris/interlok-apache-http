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

import com.adaptris.core.http.apache.request.HMACSignatureImpl.Algorithm;
import com.adaptris.core.http.apache.request.HMACSignatureImpl.Encoding;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.protocol.HttpContext;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class MagentoHMACSignatureTest {

  private static MagentoHMACSignature hmac;
  private static HttpContext context;
  private static HttpPost post;

  @BeforeClass
  public static void setUp() throws Exception {
    hmac = new MagentoHMACSignature().withIdentity("My AccessKey").withOauthVersion("0.1")
      .withOauthConsumerKey("My ConsumerKey").withOauthVerifier("My Verifier").withEncoding(Encoding.BASE64)
      .withHmacAlgorithm(Algorithm.HMAC_SHA1).withSecretKey("My SecretKey").withTargetHeader("Authorization");

    context = new HttpContext() {
      Map<String, Object> context = new HashMap<>();
      @Override
      public Object getAttribute(String id) {
        return context.get(id);
      }

      @Override
      public void setAttribute(String id, Object obj) {
        context.put(id, obj);
      }

      @Override
      public Object removeAttribute(String id) {
        return null;
      }
    };
    context.setAttribute("http.target_host", "http://localhost:8080");

    post = new HttpPost("/oauth/token");
    post.addHeader("oauth_token", "My Token");
    post.addHeader("oauth_signature", "My Signature");
  }

  @Test
  public void testBuild() {
    assertNotNull(hmac.build());
  }

  @Test
  public void testStringToSign() {
    String stringToSign = hmac.getStringToSign(post, context);

    assertTrue(stringToSign.startsWith("POST&http%3A%2F%2Flocalhost%3A8080%2Foauth%2Ftoken"));
    assertTrue(stringToSign.contains("&oauth_consumer_key=My+ConsumerKey"));
    assertTrue(stringToSign.contains("&oauth_verifier=My+Verifier"));
    assertTrue(stringToSign.contains("&oauth_signature=My+Signature"));
    assertTrue(stringToSign.contains("&oauth_token=My+Token"));
    assertTrue(stringToSign.contains("&oauth_version=0.1"));
    assertTrue(stringToSign.contains("&oauth_signature_method=HMAC-SHA1"));
    assertTrue(stringToSign.contains("&oauth_nonce="));
    assertTrue(stringToSign.contains("&oauth_timestamp="));
  }

  @Test
  public void testBuildHeader() {
    String header = hmac.buildHeader(post, context);

    assertTrue(header.startsWith(hmac.getIdentity()));
    assertTrue(header.contains("oauth_consumer_key=\"My ConsumerKey\""));
    assertTrue(header.contains("oauth_verifier=\"My Verifier\""));
    assertTrue(header.contains("oauth_signature=\"My Signature\""));
    assertTrue(header.contains("oauth_token=\"My Token\""));
    assertTrue(header.contains("oauth_version=\"0.1\""));
    assertTrue(header.contains("oauth_signature_method=\"HMAC-SHA1\""));
    assertTrue(header.contains("oauth_nonce=\""));
    assertTrue(header.contains("oauth_timestamp=\""));
    assertEquals(8, StringUtils.countMatches(header, ","));
    System.out.println(header);
  }
}

package com.adaptris.core.http.apache5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.junit.jupiter.api.Test;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageFactory;
import com.adaptris.util.KeyValuePair;
import com.adaptris.util.KeyValuePairSet;

public class ConfiguredRequestHeadersTest extends RequestHeadersCase {

  @Test
  public void testSetHandlers() throws Exception {
    ConfiguredRequestHeaders headers = new ConfiguredRequestHeaders();
    assertNotNull(headers.getHeaders());
    assertEquals(0, headers.getHeaders().size());
    String name = getName();
    headers.getHeaders().add(new KeyValuePair(name, name));
    assertEquals(1, headers.getHeaders().size());
    headers.setHeaders(new KeyValuePairSet());
    assertEquals(0, headers.getHeaders().size());
  }

  @Test
  public void testAddHeaders() throws Exception {
    HttpUriRequestBase httpOperation = new HttpPost("http://localhost:8080/anywhere");
    ConfiguredRequestHeaders headers = new ConfiguredRequestHeaders();
    String name = getName();
    headers.withHeaders(new KeyValuePair(name, name));
    AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage("");
    httpOperation = headers.addHeaders(msg, httpOperation);
    assertTrue(contains(httpOperation, name, name));
  }

}

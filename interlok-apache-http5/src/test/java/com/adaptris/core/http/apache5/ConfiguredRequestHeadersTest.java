package com.adaptris.core.http.apache5;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageFactory;
import com.adaptris.util.KeyValuePair;
import com.adaptris.util.KeyValuePairSet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ConfiguredRequestHeadersTest extends RequestHeadersCase {

  @Before
  public void setUp() throws Exception {}

  @After
  public void tearDown() throws Exception {}

  @Test
  public void testSetHandlers() throws Exception {
    ConfiguredRequestHeaders headers = new ConfiguredRequestHeaders();
    assertNotNull(headers.getHeaders());
    assertEquals(0, headers.getHeaders().size());
    String name = testName.getMethodName();
    headers.getHeaders().add(new KeyValuePair(name, name));
    assertEquals(1, headers.getHeaders().size());
    headers.setHeaders(new KeyValuePairSet());
    assertEquals(0, headers.getHeaders().size());
  }

  @Test
  public void testAddHeaders() throws Exception {
    HttpUriRequestBase httpOperation = new HttpPost("http://localhost:8080/anywhere");
    ConfiguredRequestHeaders headers = new ConfiguredRequestHeaders();
    String name = testName.getMethodName();
    headers.withHeaders(new KeyValuePair(name, name));
    AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage("");
    httpOperation = headers.addHeaders(msg, httpOperation);
    assertTrue(contains(httpOperation, name, name));
  }



}

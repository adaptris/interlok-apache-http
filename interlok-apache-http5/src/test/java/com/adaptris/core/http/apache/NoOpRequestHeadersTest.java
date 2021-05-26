package com.adaptris.core.http.apache;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageFactory;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class NoOpRequestHeadersTest extends RequestHeadersCase {
  @Before
  public void setUp() throws Exception {}

  @After
  public void tearDown() throws Exception {}

  @Test
  public void testAddHeaders() throws Exception {
    HttpUriRequestBase httpOperation = new HttpPost("http://localhost:8080/anywhere");
    NoOpRequestHeaders headers = new NoOpRequestHeaders();
    AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage("");
    String name = testName.getMethodName();
    msg.addMetadata(name, name);
    httpOperation = headers.addHeaders(msg, httpOperation);
    assertFalse(contains(httpOperation, name, name));
  }

}

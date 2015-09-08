package com.adaptris.core.http.apache;

import static org.junit.Assert.assertFalse;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.adaptris.core.AdaptrisMessageFactory;

public class DiscardResponseHeadersTest extends ResponseHeadersCase {

  @Before
  public void setUp() throws Exception {}

  @After
  public void tearDown() throws Exception {}

  @Test
  public void testHandle() {
    DiscardResponseHeaders handler = new DiscardResponseHeaders();    
    HttpRequestBase httpOperation = new HttpPost("http://localhost:8080/anywhere");
    String header = testName.getMethodName();
    httpOperation.addHeader(header, header);
    assertFalse(contains(handler.handle(httpOperation.getAllHeaders(), AdaptrisMessageFactory.getDefaultInstance().newMessage()),
        header, header));
  }

}

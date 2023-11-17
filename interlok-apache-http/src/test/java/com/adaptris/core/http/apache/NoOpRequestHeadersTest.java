package com.adaptris.core.http.apache;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.junit.jupiter.api.Test;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageFactory;

public class NoOpRequestHeadersTest extends RequestHeadersCase {

  @Test
  public void testAddHeaders() throws Exception {
    HttpRequestBase httpOperation = new HttpPost("http://localhost:8080/anywhere");
    NoOpRequestHeaders headers = new NoOpRequestHeaders();
    AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage("");
    String name = getName();
    msg.addMetadata(name, name);
    httpOperation = headers.addHeaders(msg, httpOperation);
    assertFalse(contains(httpOperation, name, name));
  }

}

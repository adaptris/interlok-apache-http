package com.adaptris.core.http.apache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHeader;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageFactory;

public class ResponseHeadersAsObjectMetadataTest extends ResponseHeadersCase {

  @Test
  public void testHandle_Response() {
    String name = getName();
    BasicHeader header = new BasicHeader(name, name);
    HttpResponse response = Mockito.mock(HttpResponse.class);
    Mockito.when(response.getAllHeaders()).thenReturn(new Header[] { header });
    AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage();
    ResponseHeadersAsObjectMetadata handler = new ResponseHeadersAsObjectMetadata();

    handler.handle(response, msg);
    assertEquals(0, msg.getMetadata().size());
    assertEquals(1, msg.getObjectHeaders().size());
    assertFalse(msg.headersContainsKey(name));
    assertTrue(msg.getObjectHeaders().containsKey(name));
    assertEquals(BasicHeader.class, msg.getObjectHeaders().get(name).getClass());
  }

  @Test
  public void testHandle_ResponsePrefix() {
    String name = getName();
    BasicHeader header = new BasicHeader(name, name);
    HttpResponse response = Mockito.mock(HttpResponse.class);
    Mockito.when(response.getAllHeaders()).thenReturn(new Header[] { header });
    AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage();
    ResponseHeadersAsObjectMetadata handler = new ResponseHeadersAsObjectMetadata("Header_");

    String expectedName = "Header_" + name;
    handler.handle(response, msg);
    assertEquals(0, msg.getMetadata().size());
    assertEquals(1, msg.getObjectHeaders().size());
    assertFalse(msg.headersContainsKey(expectedName));
    assertFalse(msg.getObjectHeaders().containsKey(name));
    assertTrue(msg.getObjectHeaders().containsKey(expectedName));
    assertEquals(BasicHeader.class, msg.getObjectHeaders().get(expectedName).getClass());
  }

  @Test
  public void testHandle_EmptyHeaders() {
    String name = getName();
    BasicHeader header = new BasicHeader(name, name);
    HttpResponse response = Mockito.mock(HttpResponse.class);
    Mockito.when(response.getAllHeaders()).thenReturn(new Header[0]);
    AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage();
    ResponseHeadersAsObjectMetadata handler = new ResponseHeadersAsObjectMetadata();
    handler.handle(response, msg);

    assertEquals(0, msg.getObjectHeaders().size());

  }

  @Test
  public void testHandle_NullHeaders() {
    String name = getName();
    BasicHeader header = new BasicHeader(name, name);
    HttpResponse response = Mockito.mock(HttpResponse.class);
    Mockito.when(response.getAllHeaders()).thenReturn(null);
    AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage();
    ResponseHeadersAsObjectMetadata handler = new ResponseHeadersAsObjectMetadata();
    handler.handle(response, msg);

    assertEquals(0, msg.getObjectHeaders().size());

  }

}

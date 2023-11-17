package com.adaptris.core.http.apache5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.message.BasicHeader;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageFactory;

public class ResponseHeadersAsMetadataTest extends ResponseHeadersCase {

  @Test
  public void testHandle_Response() {
    String name = getName();
    BasicHeader header = new BasicHeader(name, name);
    HttpResponse response = Mockito.mock(HttpResponse.class);
    Mockito.when(response.getHeaders()).thenReturn(new Header[] { header });
    AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage();
    ResponseHeadersAsMetadata handler = new ResponseHeadersAsMetadata();

    handler.handle(response, msg);
    assertEquals(1, msg.getMetadata().size());
    assertTrue(msg.headersContainsKey(name));
    assertEquals(name, msg.getMetadataValue(name));
  }

  @Test
  public void testHandle_ResponsePrefix() {
    String name = getName();
    BasicHeader header = new BasicHeader(name, name);
    HttpResponse response = Mockito.mock(HttpResponse.class);
    Mockito.when(response.getHeaders()).thenReturn(new Header[] { header });
    AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage();
    ResponseHeadersAsMetadata handler = new ResponseHeadersAsMetadata("Header_");

    String expectedName = "Header_" + name;
    handler.handle(response, msg);
    assertEquals(1, msg.getMetadata().size());
    assertTrue(msg.headersContainsKey(expectedName));
    assertFalse(msg.headersContainsKey(name));
    assertEquals(name, msg.getMetadataValue(expectedName));
  }

  @Test
  public void testHandle_EmptyHeaders() {
    String name = getName();
    BasicHeader header = new BasicHeader(name, name);
    HttpResponse response = Mockito.mock(HttpResponse.class);
    Mockito.when(response.getHeaders()).thenReturn(new Header[0]);
    AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage();
    ResponseHeadersAsMetadata handler = new ResponseHeadersAsMetadata();
    handler.handle(response, msg);

    assertEquals(0, msg.getMetadata().size());

  }

  @Test
  public void testHandle_NullHeaders() {
    String name = getName();
    BasicHeader header = new BasicHeader(name, name);
    HttpResponse response = Mockito.mock(HttpResponse.class);
    Mockito.when(response.getHeaders()).thenReturn(null);
    AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage();
    ResponseHeadersAsMetadata handler = new ResponseHeadersAsMetadata();
    handler.handle(response, msg);

    assertEquals(0, msg.getMetadata().size());

  }

}

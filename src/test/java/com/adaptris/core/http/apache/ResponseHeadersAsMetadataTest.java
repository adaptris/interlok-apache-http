package com.adaptris.core.http.apache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHeader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageFactory;

@SuppressWarnings("deprecation")
public class ResponseHeadersAsMetadataTest extends ResponseHeadersCase {

  @Before
  public void setUp() throws Exception {}

  @After
  public void tearDown() throws Exception {}
  @Test
  public void testHandle_Response() {
    String name = testName.getMethodName();
    BasicHeader header = new BasicHeader(name, name);
    HttpResponse response = Mockito.mock(HttpResponse.class);
    Mockito.when(response.getAllHeaders()).thenReturn(new Header[] {header});
    AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage();
    ResponseHeadersAsMetadata handler = new ResponseHeadersAsMetadata();

    handler.handle(response, msg);
    assertEquals(1, msg.getMetadata().size());
    assertTrue(msg.containsKey(name));
    assertEquals(name, msg.getMetadataValue(name));
  }

  @Test
  public void testHandle_ResponsePrefix() {
    String name = testName.getMethodName();
    BasicHeader header = new BasicHeader(name, name);
    HttpResponse response = Mockito.mock(HttpResponse.class);
    Mockito.when(response.getAllHeaders()).thenReturn(new Header[] {header});
    AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage();
    ResponseHeadersAsMetadata handler = new ResponseHeadersAsMetadata("Header_");

    String expectedName = "Header_" + name;
    handler.handle(response, msg);
    assertEquals(1, msg.getMetadata().size());
    assertTrue(msg.containsKey(expectedName));
    assertFalse(msg.containsKey(name));
    assertEquals(name, msg.getMetadataValue(expectedName));
  }


  @Test
  public void testHandle_EmptyHeaders() {
    String name = testName.getMethodName();
    BasicHeader header = new BasicHeader(name, name);
    HttpResponse response = Mockito.mock(HttpResponse.class);
    Mockito.when(response.getAllHeaders()).thenReturn(new Header[0]);
    AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage();
    ResponseHeadersAsMetadata handler = new ResponseHeadersAsMetadata();
    handler.handle(response, msg);

    assertEquals(0, msg.getMetadata().size());

  }

  @Test
  public void testHandle_NullHeaders() {
    String name = testName.getMethodName();
    BasicHeader header = new BasicHeader(name, name);
    HttpResponse response = Mockito.mock(HttpResponse.class);
    Mockito.when(response.getAllHeaders()).thenReturn(null);
    AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage();
    ResponseHeadersAsMetadata handler = new ResponseHeadersAsMetadata();
    handler.handle(response, msg);

    assertEquals(0, msg.getMetadata().size());

  }


}

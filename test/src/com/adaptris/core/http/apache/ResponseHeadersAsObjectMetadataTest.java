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
public class ResponseHeadersAsObjectMetadataTest extends ResponseHeadersCase {

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
    ResponseHeadersAsObjectMetadata handler = new ResponseHeadersAsObjectMetadata();

    handler.handle(response, msg);
    assertEquals(0, msg.getMetadata().size());
    assertEquals(1, msg.getObjectMetadata().size());
    assertFalse(msg.containsKey(name));
    assertTrue(msg.getObjectMetadata().containsKey(name));
    assertEquals(BasicHeader.class, msg.getObjectMetadata().get(name).getClass());
  }

  @Test
  public void testHandle_ResponsePrefix() {
    String name = testName.getMethodName();
    BasicHeader header = new BasicHeader(name, name);
    HttpResponse response = Mockito.mock(HttpResponse.class);
    Mockito.when(response.getAllHeaders()).thenReturn(new Header[] {header});
    AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage();
    ResponseHeadersAsObjectMetadata handler = new ResponseHeadersAsObjectMetadata("Header_");

    String expectedName = "Header_" + name;
    handler.handle(response, msg);
    assertEquals(0, msg.getMetadata().size());
    assertEquals(1, msg.getObjectMetadata().size());
    assertFalse(msg.containsKey(expectedName));
    assertFalse(msg.getObjectMetadata().containsKey(name));
    assertTrue(msg.getObjectMetadata().containsKey(expectedName));
    assertEquals(BasicHeader.class, msg.getObjectMetadata().get(expectedName).getClass());
  }


  @Test
  public void testHandle_EmptyHeaders() {
    String name = testName.getMethodName();
    BasicHeader header = new BasicHeader(name, name);
    HttpResponse response = Mockito.mock(HttpResponse.class);
    Mockito.when(response.getAllHeaders()).thenReturn(new Header[0]);
    AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage();
    ResponseHeadersAsObjectMetadata handler = new ResponseHeadersAsObjectMetadata();
    handler.handle(response, msg);

    assertEquals(0, msg.getObjectMetadata().size());

  }

  @Test
  public void testHandle_NullHeaders() {
    String name = testName.getMethodName();
    BasicHeader header = new BasicHeader(name, name);
    HttpResponse response = Mockito.mock(HttpResponse.class);
    Mockito.when(response.getAllHeaders()).thenReturn(null);
    AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage();
    ResponseHeadersAsObjectMetadata handler = new ResponseHeadersAsObjectMetadata();
    handler.handle(response, msg);

    assertEquals(0, msg.getObjectMetadata().size());

  }


}

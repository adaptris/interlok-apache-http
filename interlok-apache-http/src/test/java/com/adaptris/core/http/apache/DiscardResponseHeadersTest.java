package com.adaptris.core.http.apache;

import static org.junit.Assert.assertEquals;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHeader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageFactory;

public class DiscardResponseHeadersTest extends ResponseHeadersCase {

  @Before
  public void setUp() throws Exception {}

  @After
  public void tearDown() throws Exception {}

  @Test
  public void testHandle_Response() {
    String name = testName.getMethodName();
    BasicHeader header = new BasicHeader(name, name);
    HttpResponse response = Mockito.mock(HttpResponse.class);
    Mockito.when(response.getAllHeaders()).thenReturn(new Header[] { header });
    AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage();
    DiscardResponseHeaders handler = new DiscardResponseHeaders();
    
    handler.handle(response,  msg);
    assertEquals(0, msg.getMetadata().size());
  }


}

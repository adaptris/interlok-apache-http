package com.adaptris.core.http.apache;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageFactory;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.message.BasicHeader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CompositeResponseHeadersTest extends ResponseHeadersCase {

  @Before
  public void setUp() throws Exception {}

  @After
  public void tearDown() throws Exception {}
  @Test
  @SuppressWarnings("deprecation")
  public void testHandle_Response() {
    String name = testName.getMethodName();
    BasicHeader header = new BasicHeader(name, name);
    HttpResponse response = Mockito.mock(HttpResponse.class);
    Mockito.when(response.getHeaders()).thenReturn(new Header[] {header});
    AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage();
    CompositeResponseHeaderHandler handler =
        new CompositeResponseHeaderHandler(new ResponseHeadersAsMetadata(), new ResponseHeadersAsObjectMetadata());

    handler.handle(response, msg);

    assertEquals(1, msg.getMetadata().size());
    assertTrue(msg.headersContainsKey(name));
    assertEquals(name, msg.getMetadataValue(name));

    assertEquals(1, msg.getObjectMetadata().size());
    assertTrue(msg.getObjectMetadata().containsKey(name));
    assertEquals(BasicHeader.class, msg.getObjectMetadata().get(name).getClass());

  }

}

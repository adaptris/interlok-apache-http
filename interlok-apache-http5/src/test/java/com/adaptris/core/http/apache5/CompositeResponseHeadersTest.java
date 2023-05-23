package com.adaptris.core.http.apache5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.message.BasicHeader;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageFactory;

public class CompositeResponseHeadersTest extends ResponseHeadersCase {

  @Test
  public void testHandle_Response() {
    String name = getName();
    BasicHeader header = new BasicHeader(name, name);
    HttpResponse response = Mockito.mock(HttpResponse.class);
    Mockito.when(response.getHeaders()).thenReturn(new Header[] { header });
    AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage();
    CompositeResponseHeaderHandler handler = new CompositeResponseHeaderHandler(new ResponseHeadersAsMetadata(),
        new ResponseHeadersAsObjectMetadata());

    handler.handle(response, msg);

    assertEquals(1, msg.getMetadata().size());
    assertTrue(msg.headersContainsKey(name));
    assertEquals(name, msg.getMetadataValue(name));

    assertEquals(1, msg.getObjectHeaders().size());
    assertTrue(msg.getObjectHeaders().containsKey(name));
    assertEquals(BasicHeader.class, msg.getObjectHeaders().get(name).getClass());

  }

}

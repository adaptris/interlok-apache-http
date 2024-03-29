package com.adaptris.core.http.apache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHeader;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageFactory;

public class CompositeResponseHeadersTest extends ResponseHeadersCase {

  @Test
  @SuppressWarnings("deprecation")
  public void testHandle_Response() {
    String name = getName();
    BasicHeader header = new BasicHeader(name, name);
    HttpResponse response = Mockito.mock(HttpResponse.class);
    Mockito.when(response.getAllHeaders()).thenReturn(new Header[] { header });
    AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage();
    CompositeResponseHeaderHandler handler = new CompositeResponseHeaderHandler(new ResponseHeadersAsMetadata(),
        new ResponseHeadersAsObjectMetadata());

    handler.handle(response, msg);

    assertEquals(1, msg.getMetadata().size());
    assertTrue(msg.headersContainsKey(name));
    assertEquals(name, msg.getMetadataValue(name));

    assertEquals(1, msg.getObjectMetadata().size());
    assertTrue(msg.getObjectMetadata().containsKey(name));
    assertEquals(BasicHeader.class, msg.getObjectMetadata().get(name).getClass());

  }

}

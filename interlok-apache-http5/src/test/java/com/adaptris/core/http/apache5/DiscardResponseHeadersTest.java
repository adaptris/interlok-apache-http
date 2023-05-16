package com.adaptris.core.http.apache5;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.message.BasicHeader;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageFactory;

public class DiscardResponseHeadersTest extends ResponseHeadersCase {

  @Test
  public void testHandle_Response() {
    String name = getName();
    BasicHeader header = new BasicHeader(name, name);
    HttpResponse response = Mockito.mock(HttpResponse.class);
    Mockito.when(response.getHeaders()).thenReturn(new Header[] { header });
    AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage();
    DiscardResponseHeaders handler = new DiscardResponseHeaders();

    handler.handle(response, msg);
    assertEquals(0, msg.getMetadata().size());
  }

}

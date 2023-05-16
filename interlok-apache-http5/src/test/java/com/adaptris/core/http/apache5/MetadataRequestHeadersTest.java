package com.adaptris.core.http.apache5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.junit.jupiter.api.Test;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageFactory;
import com.adaptris.core.metadata.NoOpMetadataFilter;
import com.adaptris.core.metadata.RegexMetadataFilter;

public class MetadataRequestHeadersTest extends RequestHeadersCase {

  @Test
  public void testFilter() throws Exception {
    MetadataRequestHeaders headers = new MetadataRequestHeaders();
    assertNull(headers.getFilter());
    headers.setFilter(new NoOpMetadataFilter());
    assertEquals(NoOpMetadataFilter.class, headers.getFilter().getClass());
    try {
      headers.setFilter(null);
      fail();
    } catch (IllegalArgumentException expected) {

    }
    assertEquals(NoOpMetadataFilter.class, headers.getFilter().getClass());
  }

  @Test
  public void testAddHeaders() throws Exception {
    HttpUriRequestBase httpOperation = new HttpPost("http://localhost:8080/anywhere");
    MetadataRequestHeaders headers = new MetadataRequestHeaders();
    headers.setFilter(new RegexMetadataFilter());
    AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage("");
    String name = getName();
    msg.addMetadata(name, name);
    httpOperation = headers.addHeaders(msg, httpOperation);
    assertTrue(contains(httpOperation, name, name));
  }

}

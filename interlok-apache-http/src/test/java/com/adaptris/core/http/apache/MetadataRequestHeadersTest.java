package com.adaptris.core.http.apache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.junit.jupiter.api.Test;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageFactory;
import com.adaptris.core.metadata.NoOpMetadataFilter;
import com.adaptris.core.metadata.RegexMetadataFilter;
import com.adaptris.core.metadata.RemoveAllMetadataFilter;

public class MetadataRequestHeadersTest extends RequestHeadersCase {

  @Test
  public void testFilter() throws Exception {
    MetadataRequestHeaders headers = new MetadataRequestHeaders();
    assertNull(headers.getFilter());
    assertEquals(RemoveAllMetadataFilter.class, headers.filter().getClass());
    headers.setFilter(new NoOpMetadataFilter());
    assertEquals(NoOpMetadataFilter.class, headers.getFilter().getClass());
  }

  @Test
  public void testAddHeaders() throws Exception {
    HttpRequestBase httpOperation = new HttpPost("http://localhost:8080/anywhere");
    MetadataRequestHeaders headers = new MetadataRequestHeaders();
    headers.setFilter(new RegexMetadataFilter());
    AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage("");
    String name = getName();
    msg.addMetadata(name, name);
    httpOperation = headers.addHeaders(msg, httpOperation);
    assertTrue(contains(httpOperation, name, name));
  }

}

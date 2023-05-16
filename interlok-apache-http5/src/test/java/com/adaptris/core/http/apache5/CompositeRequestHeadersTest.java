package com.adaptris.core.http.apache5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.junit.jupiter.api.Test;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageFactory;
import com.adaptris.core.http.client.RequestHeaderProvider;
import com.adaptris.core.metadata.RegexMetadataFilter;

public class CompositeRequestHeadersTest extends RequestHeadersCase {

  @Test
  public void testSetHandlers() throws Exception {
    CompositeRequestHeaders headers = new CompositeRequestHeaders();
    assertNotNull(headers.getProviders());
    assertEquals(0, headers.getProviders().size());
    headers.addHandler(new NoOpRequestHeaders());
    assertEquals(1, headers.getProviders().size());
    headers.setProviders(new ArrayList<RequestHeaderProvider<HttpUriRequestBase>>());
    assertEquals(0, headers.getProviders().size());
  }

  @Test
  public void testAddHeaders() throws Exception {
    HttpUriRequestBase httpOperation = new HttpPost("http://localhost:8080/anywhere");
    CompositeRequestHeaders headers = new CompositeRequestHeaders();
    MetadataRequestHeaders meta = new MetadataRequestHeaders();
    meta.setFilter(new RegexMetadataFilter());
    AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage("");
    String name = getName();
    headers.addHandler(meta);
    msg.addMetadata(name, name);
    httpOperation = headers.addHeaders(msg, httpOperation);
    assertTrue(contains(httpOperation, name, name));
  }

}

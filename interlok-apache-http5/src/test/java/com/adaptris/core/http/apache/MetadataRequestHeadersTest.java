package com.adaptris.core.http.apache;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageFactory;
import com.adaptris.core.metadata.NoOpMetadataFilter;
import com.adaptris.core.metadata.RegexMetadataFilter;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MetadataRequestHeadersTest extends RequestHeadersCase {

  @Before
  public void setUp() throws Exception {}

  @After
  public void tearDown() throws Exception {}

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
    String name = testName.getMethodName();
    msg.addMetadata(name, name);
    httpOperation = headers.addHeaders(msg, httpOperation);
    assertTrue(contains(httpOperation, name, name));
  }



}

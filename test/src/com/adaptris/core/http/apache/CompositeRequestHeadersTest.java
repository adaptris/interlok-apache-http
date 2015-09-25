package com.adaptris.core.http.apache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageFactory;
import com.adaptris.core.http.client.RequestHeaderProvider;
import com.adaptris.core.metadata.RegexMetadataFilter;

public class CompositeRequestHeadersTest extends RequestHeadersCase {

  @Before
  public void setUp() throws Exception {}

  @After
  public void tearDown() throws Exception {}

  @Test
  public void testSetHandlers() throws Exception {
    CompositeRequestHeaders headers = new CompositeRequestHeaders();
    assertNotNull(headers.getProviders());
    assertEquals(0, headers.getProviders().size());
    headers.addHandler(new NoOpRequestHeaders());
    assertEquals(1, headers.getProviders().size());
    headers.setProviders(new ArrayList<RequestHeaderProvider<HttpRequestBase>>());
    assertEquals(0, headers.getProviders().size());
  }

  @Test
  public void testAddHeaders() throws Exception {
    HttpRequestBase httpOperation = new HttpPost("http://localhost:8080/anywhere");
    CompositeRequestHeaders headers = new CompositeRequestHeaders();
    MetadataRequestHeaders meta = new MetadataRequestHeaders();
    meta.setFilter(new RegexMetadataFilter());
    AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage("");
    String name = testName.getMethodName();
    headers.addHandler(meta);
    msg.addMetadata(name, name);
    httpOperation = headers.addHeaders(msg, httpOperation);
    assertTrue(contains(httpOperation, name, name));
  }



}

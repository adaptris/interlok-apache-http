package com.adaptris.core.http.apache;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.adaptris.core.AdaptrisMessageFactory;

public class ResponseHeadersAsMetadataTest extends ResponseHeadersCase {

  @Before
  public void setUp() throws Exception {}

  @After
  public void tearDown() throws Exception {}
  @Test
  public void testHandle_NoPrefix() {
    ResponseHeadersAsMetadata handler = new ResponseHeadersAsMetadata();
    HttpRequestBase httpOperation = new HttpPost("http://localhost:8080/anywhere");
    String header = testName.getMethodName();
    httpOperation.addHeader(header, header);
    assertTrue(contains(handler.handle(httpOperation.getAllHeaders(), AdaptrisMessageFactory.getDefaultInstance().newMessage()),
        header, header));
  }

  @Test
  public void testHandle_Prefix() {
    String header = testName.getMethodName();
    ResponseHeadersAsMetadata handler = new ResponseHeadersAsMetadata();
    handler.setMetadataPrefix(header + "_");
    HttpRequestBase httpOperation = new HttpPost("http://localhost:8080/anywhere");
    httpOperation.addHeader(header, header);
    assertFalse(contains(handler.handle(httpOperation.getAllHeaders(), AdaptrisMessageFactory.getDefaultInstance().newMessage()),
        header, header));
    assertTrue(contains(handler.handle(httpOperation.getAllHeaders(), AdaptrisMessageFactory.getDefaultInstance().newMessage()),
        handler.getMetadataPrefix() + header, header));
  }


}

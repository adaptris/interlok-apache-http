package com.adaptris.core.http.apache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.apache.http.entity.ContentType;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.CoreException;
import com.adaptris.core.DefaultMessageFactory;

@SuppressWarnings("deprecation")
public class MetadataContentTypeProviderTest {
  @Rule
  public TestName testName = new TestName();

  @Before
  public void setUp() throws Exception {}

  @After
  public void tearDown() throws Exception {}

  @Test
  public void testGetContentType() throws Exception {
    MetadataContentTypeProvider provider = new MetadataContentTypeProvider(testName.getMethodName());

    AdaptrisMessage msg = new DefaultMessageFactory().newMessage("");
    msg.addMetadata(testName.getMethodName(), "text/complicated");

    ContentType contentType = ContentType.parse(provider.getContentType(msg));
    assertEquals("text/complicated", contentType.getMimeType());
    assertNull(contentType.getCharset());
  }

  @Test
  public void testGetContentType_MetadataKeyNonExistent() throws Exception {
    MetadataContentTypeProvider provider = new MetadataContentTypeProvider(testName.getMethodName());

    AdaptrisMessage msg = new DefaultMessageFactory().newMessage("");
    ContentType contentType = ContentType.parse(provider.getContentType(msg));
    assertEquals("text/plain", contentType.getMimeType());
  }

  @Test
  public void testGetContentType_NullMetadataKey() throws Exception {
    MetadataContentTypeProvider provider = new MetadataContentTypeProvider();

    AdaptrisMessage msg = new DefaultMessageFactory().newMessage("");
    msg.addMetadata(testName.getMethodName(), "text/complicated");
    try {
      ContentType contentType = ContentType.parse(provider.getContentType(msg));
      fail();
    } catch (CoreException expected) {

    }
  }


}
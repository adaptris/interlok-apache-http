package com.adaptris.core.http.apache5;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageFactory;
import com.adaptris.core.http.ConfiguredContentTypeProvider;

public class AdaptrisMessageEntityTest {

  private static final byte[] PAYLOAD = "Hello World".getBytes(Charset.defaultCharset());

  @Test
  public void testGetters() throws Exception {
    ConfiguredContentTypeProvider ct = new ConfiguredContentTypeProvider("text/plain");
    AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage(PAYLOAD);
    AdaptrisMessageEntity entity = new AdaptrisMessageEntity(msg, ct);
    assertTrue(entity.isRepeatable());
    assertTrue(entity.isStreaming());
    assertTrue(entity.getContentLength() > 0);
    assertNotNull(entity.getContentType());
  }

  @Test
  public void testGetContent() throws Exception {
    ConfiguredContentTypeProvider ct = new ConfiguredContentTypeProvider("text/plain");
    AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage(PAYLOAD);
    AdaptrisMessageEntity entity = new AdaptrisMessageEntity(msg, ct);
    try (InputStream in = entity.getContent(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      IOUtils.copy(in, out);
      assertArrayEquals(PAYLOAD, out.toByteArray());
    }
  }

  @Test
  public void testWriteTo() throws Exception {
    ConfiguredContentTypeProvider ct = new ConfiguredContentTypeProvider("text/plain");
    AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage(PAYLOAD);
    AdaptrisMessageEntity entity = new AdaptrisMessageEntity(msg, ct);
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      entity.writeTo(out);
      assertArrayEquals(PAYLOAD, out.toByteArray());
    }
  }

}

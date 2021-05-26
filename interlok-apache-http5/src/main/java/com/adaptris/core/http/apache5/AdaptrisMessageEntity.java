package com.adaptris.core.http.apache5;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.CoreException;
import com.adaptris.core.http.ContentTypeProvider;
import org.apache.commons.io.IOUtils;
import org.apache.hc.core5.http.io.entity.AbstractHttpEntity;
import org.apache.hc.core5.util.Args;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A streamed, repeatable entity that obtains its content from the associated
 * {@link AdaptrisMessage#getInputStream()}.
 * 
 * @see AbstractHttpEntity
 */
public final class AdaptrisMessageEntity extends AbstractHttpEntity {

  private transient final AdaptrisMessage msg;

  public AdaptrisMessageEntity(AdaptrisMessage msg, ContentTypeProvider contentType) throws CoreException {
    super(Args.notNull(contentType, "Content Type").getContentType(msg), null, false);
    this.msg = Args.notNull(msg, "AdaptrisMessage");
  }


  @Override
  public boolean isRepeatable() {
    return true;
  }

  @Override
  public long getContentLength() {
    return msg.getSize();
  }

  @Override
  public InputStream getContent() throws IOException {
    return msg.getInputStream();
  }

  @Override
  public void writeTo(final OutputStream output) throws IOException {
    Args.notNull(output, "OutputStream");
    try (InputStream in = msg.getInputStream()) {
      IOUtils.copy(in, output);
    } finally {
      output.flush();
    }
  }

  @Override
  public boolean isStreaming() {
    return true;
  }

  @Override
  public void close() throws IOException
  {

  }
}

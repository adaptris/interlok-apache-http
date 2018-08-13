package com.adaptris.core.http.apache;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.util.Args;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.CoreException;
import com.adaptris.core.http.ContentTypeProvider;

/**
 * A streamed, repeatable entity that obtains its content from the associated
 * {@link AdaptrisMessage#getInputStream()}.
 * 
 * @see AbstractHttpEntity
 */
public final class AdaptrisMessageEntity extends AbstractHttpEntity {

  private transient final AdaptrisMessage msg;

  public AdaptrisMessageEntity(AdaptrisMessage msg, ContentTypeProvider contentType) throws CoreException {
    super();
    this.msg = Args.notNull(msg, "AdaptrisMessage");
    setContentType(Args.notNull(contentType, "Content Type").getContentType(msg));
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
}

package com.adaptris.core.http.apache;
import org.apache.http.entity.ContentType;

import com.adaptris.core.AdaptrisMessage;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Provides a static content type.
 * <p>
 * Note that the content type character set if derived from
 * {@link AdaptrisMessage#getCharEncoding()} so configuring a mime type of {@code text/xml} when the
 * message has a char encoding of {@code UTF-8} will return {@code text/xml; charset="UTF-8"}
 * </p>
 * 
 * @config apache-http-static-content-type-provider
 */
@XStreamAlias("apache-http-static-content-type-provider")
public class StaticContentTypeProvider implements ContentTypeProvider {

  private String mimeType;

  public StaticContentTypeProvider() {
    setMimeType(ContentType.TEXT_PLAIN.getMimeType());
  }

  @Override
  public ContentType getContentType(AdaptrisMessage msg) {
    return ContentType.create(getMimeType(), msg.getCharEncoding());
  }


  public String getMimeType() {
    return mimeType;
  }

  /**
   * Set the base content type.
   * 
   * @param contentType the base content type; defaults to text/plain
   */
  public void setMimeType(String contentType) {
    this.mimeType = contentType;
  }

}

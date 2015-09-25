package com.adaptris.core.http.apache;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * @deprecated since 3.0.6 use {@link com.adaptris.core.http.ConfiguredContentTypeProvider} instead.
 */
@XStreamAlias("apache-http-static-content-type-provider")
@Deprecated
public class StaticContentTypeProvider extends com.adaptris.core.http.ConfiguredContentTypeProvider {
  private static transient boolean warningLogged;
  private transient Logger log = LoggerFactory.getLogger(this.getClass());

  public StaticContentTypeProvider() {
    if (!warningLogged) {
      log.warn("[{}] is deprecated, use [{}] instead", this.getClass().getSimpleName(),
          com.adaptris.core.http.ConfiguredContentTypeProvider.class.getName());
      warningLogged = true;
    }
    setMimeType(ContentType.TEXT_PLAIN.getMimeType());
  }

}

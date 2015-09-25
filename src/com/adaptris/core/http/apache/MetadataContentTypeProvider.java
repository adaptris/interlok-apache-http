package com.adaptris.core.http.apache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Provides a content type derived from metadata.
 * <p>
 * Note that the content type charset will be derived from {@link AdaptrisMessage#getCharEncoding()}
 * so configuring a mime type of {@code text/xml} when the message has a char encoding of
 * {@code UTF-8} will return {@code text/xml; charset="UTF-8"}. Also no validation is done of the
 * metadata value forming the mime-type. it is passed straight through to
 * {@link ContentType#create(String, String)}
 * </p>
 * @config apache-http-metadata-content-type-provider
 * @deprecated since 3.0.6 use {@link com.adaptris.core.http.MetadataContentTypeProvider} instead.
 */
@XStreamAlias("apache-http-metadata-content-type-provider")
@Deprecated
public class MetadataContentTypeProvider extends com.adaptris.core.http.MetadataContentTypeProvider {
  private static transient boolean warningLogged;
  private transient Logger log = LoggerFactory.getLogger(this.getClass());

  public MetadataContentTypeProvider() {
    if (!warningLogged) {
      log.warn("[{}] is deprecated, use [{}] instead", this.getClass().getSimpleName(),
          com.adaptris.core.http.MetadataContentTypeProvider.class.getName());
      warningLogged = true;
    }
  }

  public MetadataContentTypeProvider(String key) {
    this();
    setMetadataKey(key);
  }

}

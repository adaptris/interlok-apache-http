package com.adaptris.core.http.apache;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.http.client.ResponseHeaderHandler;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpResponse;

/**
 * Concrete implementation of {@link ResponseHeaderHandler} which adds all the HTTP headers from the
 * response as object metadata to the {@link AdaptrisMessage}.
 * 
 * <p>The underlying {@link Header} object that will be added to object metadata and keyed by {@link Header#getName}. and will
 * include header fields where the name is {@code null};
 * </p>
 * @config apache-http-response-headers-as-object-metadata
 * @author lchan
 * 
 */
@XStreamAlias("apache-http-response-headers-as-object-metadata")
public class ResponseHeadersAsObjectMetadata extends ResponseHeadersAsMetadata {

  public ResponseHeadersAsObjectMetadata() {

  }

  public ResponseHeadersAsObjectMetadata(String prefix) {
    this();
    setMetadataPrefix(prefix);
  }

  @Override
  public AdaptrisMessage handle(HttpResponse src, AdaptrisMessage msg) {
    Header[] headers = src.getHeaders();
    if (notNull(headers)) {
      log.trace("Processing {} headers from response", headers.length);
      for (Header h : headers) {
        String metadataKey = generateKey(h.getName());
        log.trace("Adding {}: {}", metadataKey, h);
        msg.getObjectMetadata().put(metadataKey, h);
      }
    }
    return msg;
  }
}

package com.adaptris.core.http.apache;

import static org.apache.commons.lang.StringUtils.defaultIfEmpty;

import org.apache.http.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adaptris.core.AdaptrisMessage;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Concrete implementation of {@link ResponseHeaderHandler} which adds all the HTTP headers from
 * the response as metadata to the {@link AdaptrisMessage}.
 * 
 * @author lchan
 * 
 */
@XStreamAlias("apache-http-response-headers-as-metadata")
public class ResponseHeadersAsMetadata implements ResponseHeaderHandler {

  private Logger log = LoggerFactory.getLogger(this.getClass());

  private String metadataPrefix;

  public ResponseHeadersAsMetadata() {

  }

  public ResponseHeadersAsMetadata(String prefix) {
    this();
    setMetadataPrefix(prefix);
  }

  @Override
  public AdaptrisMessage handle(Header[] headers, AdaptrisMessage msg) {
    for (Header h : headers) {
      String metadataKey = generateKey(h.getName());
      log.trace("Adding {}: {}", metadataKey, h.getValue());
      msg.addMetadata(metadataKey, h.getValue());
    }
    return msg;
  }

  private String generateKey(String header) {
    return defaultIfEmpty(getMetadataPrefix(), "") + header;
  }

  public String getMetadataPrefix() {
    return metadataPrefix;
  }

  public void setMetadataPrefix(String metadataPrefix) {
    this.metadataPrefix = metadataPrefix;
  }
}

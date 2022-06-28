package com.adaptris.core.http.apache;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.http.client.ResponseHeaderHandler;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Concrete implementation of {@link ResponseHeaderHandler} which adds all the HTTP headers from the
 * response as metadata to the {@link AdaptrisMessage}.
 *
 * @config apache-http-response-headers-as-metadata
 * @author lchan
 *
 */
@XStreamAlias("apache-http-response-headers-as-metadata")
@NoArgsConstructor
public class ResponseHeadersAsMetadata implements ResponseHeaderHandler<HttpResponse> {

  protected transient Logger log = LoggerFactory.getLogger(this.getClass());

  /** The metadata prefix to add to any metadata recorded from the HTTP response headers.
   */
  @Getter
  @Setter
  private String metadataPrefix;

  public ResponseHeadersAsMetadata(String prefix) {
    this();
    setMetadataPrefix(prefix);
  }

  @Override
  public AdaptrisMessage handle(HttpResponse response, AdaptrisMessage msg) {
    Header[] headers = response.getAllHeaders();
    if (notNull(headers)) {
      log.trace("Processing {} headers from response", headers.length);
      for (Header h : headers) {
        String metadataKey = generateKey(h.getName());
        log.trace("Adding {}: {}", metadataKey, h.getValue());
        msg.addMetadata(metadataKey, h.getValue());
      }
    }
    return msg;
  }

  protected String generateKey(String header) {
    return defaultIfEmpty(getMetadataPrefix(), "") + header;
  }

  protected static boolean notNull(Object[] o) {
    return ArrayUtils.isNotEmpty(o);
  }
}

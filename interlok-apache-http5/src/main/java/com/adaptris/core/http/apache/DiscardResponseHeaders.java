package com.adaptris.core.http.apache;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.http.client.ResponseHeaderHandler;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.apache.hc.core5.http.HttpResponse;

/**
 * {@link ResponseHeaderHandler} implementation that discards the headers from the HTTP response.
 *
 * @author lchan
 * @config apache-http-discard-response-headers
 */
@XStreamAlias("apache-http5-discard-response-headers")
public class DiscardResponseHeaders implements ResponseHeaderHandler<HttpResponse> {

  @Override
  public AdaptrisMessage handle(HttpResponse src, AdaptrisMessage msg) {
    return msg;
  }

}

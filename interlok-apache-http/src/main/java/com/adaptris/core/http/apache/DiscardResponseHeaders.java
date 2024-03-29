package com.adaptris.core.http.apache;

import lombok.NoArgsConstructor;
import org.apache.http.HttpResponse;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.http.client.ResponseHeaderHandler;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * {@link ResponseHeaderHandler} implementation that discards the headers from the HTTP response.
 *
 * @config apache-http-discard-response-headers
 */
@XStreamAlias("apache-http-discard-response-headers")
@NoArgsConstructor
public class DiscardResponseHeaders implements ResponseHeaderHandler<HttpResponse> {

  @Override
  public AdaptrisMessage handle(HttpResponse src, AdaptrisMessage msg) {
    return msg;
  }

}

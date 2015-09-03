package com.adaptris.core.http.apache;

import org.apache.http.Header;

import com.adaptris.core.AdaptrisMessage;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * {@link ResponseHeaderHandler} implementation that discards the headers from the HTTP response.
 * 
 * @author lchan
 * @config apache-http-discard-response-headers
 */
@XStreamAlias("apache-http-discard-response-headers")
public class DiscardResponseHeaders implements ResponseHeaderHandler {

  @Override
  public AdaptrisMessage handle(Header[] headers, AdaptrisMessage msg) {
    return msg;
  }

}

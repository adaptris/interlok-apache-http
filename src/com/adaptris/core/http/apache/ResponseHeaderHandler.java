package com.adaptris.core.http.apache;

import org.apache.http.Header;
import org.apache.http.HttpResponse;

import com.adaptris.core.AdaptrisMessage;

/**
 * Interface to handle the headers from the HTTP response.
 * 
 * 
 */
public interface ResponseHeaderHandler {

  /**
   * Do something with the response headers
   * 
   * @param headers the headers from {@link HttpResponse#getAllHeaders()}
   * @param msg the AdaptrisMessage.
   * @return the modified message.
   */
  AdaptrisMessage handle(Header[] headers, AdaptrisMessage msg);

}

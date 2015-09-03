package com.adaptris.core.http.apache;

import org.apache.http.client.methods.HttpRequestBase;

import com.adaptris.core.AdaptrisMessage;

/**
 * Interface to generate http request headers.
 * 
 * 
 */
public interface RequestHeaderHandler {

  /**
   * Apply any additional headers required.
   * 
   * @param msg the {@link AdaptrisMessage} to source the headers from
   * @param target the {@link HttpRequestBase}
   * @return the modified HttpRequestBase object.
   */
  HttpRequestBase addHeaders(AdaptrisMessage msg, HttpRequestBase target);


}

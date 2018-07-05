package com.adaptris.core.http.apache;

import org.apache.http.client.methods.HttpRequestBase;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.http.client.RequestHeaderProvider;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Implementation of {@link RequestHeaderProvider} that adds no additional headers
 * 
 * @config apache-http-no-request-headers
 * @author lchan
 * 
 */
@XStreamAlias("apache-http-no-request-headers")
public class NoOpRequestHeaders implements RequestHeaderProvider<HttpRequestBase> {


  @Override
  public HttpRequestBase addHeaders(AdaptrisMessage msg, HttpRequestBase target) {
    return target;
  }

}

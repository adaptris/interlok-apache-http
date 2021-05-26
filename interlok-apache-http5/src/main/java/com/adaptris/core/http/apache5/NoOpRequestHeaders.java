package com.adaptris.core.http.apache5;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.http.client.RequestHeaderProvider;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

/**
 * Implementation of {@link RequestHeaderProvider} that adds no additional headers
 *
 * @config apache-http-no-request-headers
 * @author lchan
 *
 */
@XStreamAlias("apache-http5-no-request-headers")
public class NoOpRequestHeaders implements RequestHeaderProvider<HttpUriRequestBase> {


  @Override
  public HttpUriRequestBase addHeaders(AdaptrisMessage msg, HttpUriRequestBase target) {
    return target;
  }

}

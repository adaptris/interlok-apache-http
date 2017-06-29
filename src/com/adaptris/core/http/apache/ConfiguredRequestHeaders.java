package com.adaptris.core.http.apache;

import javax.validation.constraints.NotNull;

import org.apache.http.client.methods.HttpRequestBase;

import com.adaptris.annotation.AutoPopulated;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.http.client.RequestHeaderProvider;
import com.adaptris.core.util.Args;
import com.adaptris.util.KeyValuePair;
import com.adaptris.util.KeyValuePairSet;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Implementation of {@link RequestHeaderProvider} that applies static configured values as headers.
 * 
 * @config apache-http-configured-request-headers
 * 
 */
@XStreamAlias("apache-http-configured-request-headers")
public class ConfiguredRequestHeaders implements RequestHeaderProvider<HttpRequestBase> {
  @NotNull
  @AutoPopulated
  private KeyValuePairSet headers;

  public ConfiguredRequestHeaders() {
    headers = new KeyValuePairSet();
  }


  @Override
  public HttpRequestBase addHeaders(AdaptrisMessage msg, HttpRequestBase target) {
    for (KeyValuePair k : getHeaders()) {
      target.addHeader(k.getKey(), k.getValue());
    }
    return target;
  }


  public KeyValuePairSet getHeaders() {
    return headers;
  }

  public void setHeaders(KeyValuePairSet headers) {
    this.headers = Args.notNull(headers, "headers");
  }
}

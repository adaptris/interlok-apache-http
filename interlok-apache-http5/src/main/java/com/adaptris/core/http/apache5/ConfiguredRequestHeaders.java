package com.adaptris.core.http.apache5;

import com.adaptris.annotation.AutoPopulated;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.http.client.RequestHeaderProvider;
import com.adaptris.core.util.Args;
import com.adaptris.util.KeyValuePair;
import com.adaptris.util.KeyValuePairSet;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

import javax.validation.constraints.NotNull;

/**
 * Implementation of {@link RequestHeaderProvider} that applies static configured values as headers.
 *
 * @config apache-http-configured-request-headers
 *
 */
@XStreamAlias("apache-http5-configured-request-headers")
public class ConfiguredRequestHeaders implements RequestHeaderProvider<HttpUriRequestBase> {
  @NotNull
  @AutoPopulated
  private KeyValuePairSet headers;

  public ConfiguredRequestHeaders() {
    headers = new KeyValuePairSet();
  }


  @Override
  public HttpUriRequestBase addHeaders(AdaptrisMessage msg, HttpUriRequestBase target) {
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

  public ConfiguredRequestHeaders withHeaders(KeyValuePair... keyValuePairs) {
    for (KeyValuePair k : keyValuePairs) {
      getHeaders().add(k);
    }
    return this;
  }
}

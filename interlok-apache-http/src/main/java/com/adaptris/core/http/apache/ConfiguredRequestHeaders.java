package com.adaptris.core.http.apache;

import com.adaptris.annotation.AutoPopulated;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.http.client.RequestHeaderProvider;
import com.adaptris.util.KeyValuePair;
import com.adaptris.util.KeyValuePairSet;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.http.client.methods.HttpRequestBase;

/**
 * Implementation of {@link RequestHeaderProvider} that applies static configured values as headers.
 *
 * @config apache-http-configured-request-headers
 *
 */
@XStreamAlias("apache-http-configured-request-headers")
@NoArgsConstructor
public class ConfiguredRequestHeaders implements RequestHeaderProvider<HttpRequestBase> {

  /** The list of headers to add to the request.
   *
   */
  @Getter
  @Setter
  @NotNull(message="Use an empty set, not null for headers")
  @AutoPopulated
  private KeyValuePairSet headers = new KeyValuePairSet();

  @Override
  public HttpRequestBase addHeaders(AdaptrisMessage msg, HttpRequestBase target) {
    for (KeyValuePair k : getHeaders()) {
      target.addHeader(k.getKey(), k.getValue());
    }
    return target;
  }

  public ConfiguredRequestHeaders withHeaders(KeyValuePair... keyValuePairs) {
    for (KeyValuePair k : keyValuePairs) {
      getHeaders().add(k);
    }
    return this;
  }
}

package com.adaptris.core.http.apache;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import org.apache.http.client.methods.HttpRequestBase;

import com.adaptris.annotation.AutoPopulated;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.http.client.RequestHeaderProvider;
import com.adaptris.core.util.Args;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

/**
 * Implementation of {@link RequestHeaderProvider} that uses its own configured handlers to add headers.
 *
 * <p>
 * This implementation is primarily so that you can mix and match both static and metadata driven headers; the order in which you
 * configure them determines what is actually present as headers.
 * </p>
 *
 * @config apache-http-composite-request-headers
 *
 */
@XStreamAlias("apache-http-composite-request-headers")
@NoArgsConstructor
public class CompositeRequestHeaders implements RequestHeaderProvider<HttpRequestBase> {

  /** The list of {@link RequestHeaderProvider}s to apply to the HTTP Request.
   *
   */
  @Valid
  @Getter
  @Setter
  @XStreamImplicit
  @NotNull(message="Use an empty list of RequestHeaderProviders, not null")
  @NonNull
  @AutoPopulated
  private List<RequestHeaderProvider<HttpRequestBase>> providers = new ArrayList<>();

  @Override
  public HttpRequestBase addHeaders(AdaptrisMessage msg, HttpRequestBase target) {
    HttpRequestBase http = target;
    for (RequestHeaderProvider<HttpRequestBase> h : getProviders()) {
      http = h.addHeaders(msg, http);
    }
    return http;
  }

  public void addHandler(RequestHeaderProvider<HttpRequestBase> handler) {
    getProviders().add(Args.notNull(handler, "Request Header Provider"));
  }
}

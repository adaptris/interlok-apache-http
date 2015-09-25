package com.adaptris.core.http.apache;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.apache.http.client.methods.HttpRequestBase;

import com.adaptris.annotation.AutoPopulated;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.http.client.RequestHeaderProvider;
import com.adaptris.core.util.Args;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

/**
 * Implementation of {@link RequestHeaderHandler} that uses its own configured handlers to add headers.
 * 
 * <p>This implementation is primarily so that you can mix and match both static and metadata driven headers; the order in which
 * you configure them determines what is actually present as headers.
 * </p>
 * @config apache-http-composite-request-headers
 * 
 */
@XStreamAlias("apache-http-composite-request-headers")
public class CompositeRequestHeaders implements RequestHeaderProvider<HttpRequestBase> {
  @XStreamImplicit
  @NotNull
  @AutoPopulated
  private List<RequestHeaderProvider<HttpRequestBase>> providers;

  public CompositeRequestHeaders() {
    providers = new ArrayList<>();
  }


  @Override
  public HttpRequestBase addHeaders(AdaptrisMessage msg, HttpRequestBase target) {
    HttpRequestBase http = target;
    for (RequestHeaderProvider<HttpRequestBase> h : getProviders()) {
      http = h.addHeaders(msg, http);
    }
    return http;
  }


  public List<RequestHeaderProvider<HttpRequestBase>> getProviders() {
    return providers;
  }


  public void setProviders(List<RequestHeaderProvider<HttpRequestBase>> handlers) {
    this.providers = Args.notNull(handlers, "Request Header Providers");
  }

  public void addHandler(RequestHeaderProvider<HttpRequestBase> handler) {
    getProviders().add(Args.notNull(handler, "Request Header Provider"));
  }
}

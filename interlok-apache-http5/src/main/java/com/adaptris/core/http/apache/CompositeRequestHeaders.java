package com.adaptris.core.http.apache;

import com.adaptris.annotation.AutoPopulated;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.http.client.RequestHeaderProvider;
import com.adaptris.core.util.Args;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

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
public class CompositeRequestHeaders implements RequestHeaderProvider<HttpUriRequestBase> {
  @XStreamImplicit
  @NotNull
  @AutoPopulated
  private List<RequestHeaderProvider<HttpUriRequestBase>> providers;

  public CompositeRequestHeaders() {
    providers = new ArrayList<>();
  }


  @Override
  public HttpUriRequestBase addHeaders(AdaptrisMessage msg, HttpUriRequestBase target) {
    HttpUriRequestBase http = target;
    for (RequestHeaderProvider<HttpUriRequestBase> h : getProviders()) {
      http = h.addHeaders(msg, http);
    }
    return http;
  }


  public List<RequestHeaderProvider<HttpUriRequestBase>> getProviders() {
    return providers;
  }


  public void setProviders(List<RequestHeaderProvider<HttpUriRequestBase>> handlers) {
    this.providers = Args.notNull(handlers, "Request Header Providers");
  }

  public void addHandler(RequestHeaderProvider<HttpUriRequestBase> handler) {
    getProviders().add(Args.notNull(handler, "Request Header Provider"));
  }
}

package com.adaptris.core.http.apache;

import com.adaptris.annotation.AutoPopulated;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.http.client.ResponseHeaderHandler;
import com.adaptris.core.util.Args;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import org.apache.hc.core5.http.HttpResponse;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of {@link ResponseHeaderHandler} that uses nested handlers to extract headers from a {@link
 * HttpResponse}.
 * 
 * <p>This implementation is primarily so that you can mix and matchhow you capture response headers; If you wanted to use both
 * {@link ResponseHeadersAsMetadata} and {@link ResponseHeadersAsObjectMetadata} then you can.
 * </p>
 * @config apache-http-composite-request-headers
 * 
 */
@XStreamAlias("apache-http-composite-response-header-handler")
public class CompositeResponseHeaderHandler implements ResponseHeaderHandler<HttpResponse> {
  @XStreamImplicit
  @NotNull
  @AutoPopulated
  private List<ResponseHeaderHandler<HttpResponse>> handlers;

  public CompositeResponseHeaderHandler() {
    setHandlers(new ArrayList<ResponseHeaderHandler<HttpResponse>>());
  }

  public CompositeResponseHeaderHandler(ResponseHeaderHandler<HttpResponse>... handlers) {
    this();
    for (ResponseHeaderHandler<HttpResponse> h : handlers) {
      addHandler(h);
    }
  }

  public List<ResponseHeaderHandler<HttpResponse>> getHandlers() {
    return handlers;
  }

  public void setHandlers(List<ResponseHeaderHandler<HttpResponse>> handlers) {
    this.handlers = Args.notNull(handlers, "Response Header Handlers");
  }

  public void addHandler(ResponseHeaderHandler<HttpResponse> handler) {
    getHandlers().add(Args.notNull(handler, "Response Handler"));
  }

  @Override
  public AdaptrisMessage handle(HttpResponse src, AdaptrisMessage msg) {
    AdaptrisMessage target = msg;
    for (ResponseHeaderHandler<HttpResponse> h : getHandlers()) {
      target = h.handle(src, target);
    }
    return target;
  }
}

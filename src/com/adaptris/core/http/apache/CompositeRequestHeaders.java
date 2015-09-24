package com.adaptris.core.http.apache;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.apache.http.client.methods.HttpRequestBase;

import com.adaptris.annotation.AutoPopulated;
import com.adaptris.core.AdaptrisMessage;
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
public class CompositeRequestHeaders implements RequestHeaderHandler {
  @XStreamImplicit
  @NotNull
  @AutoPopulated
  private List<RequestHeaderHandler> handlers;

  public CompositeRequestHeaders() {
    handlers = new ArrayList<>();
  }


  @Override
  public HttpRequestBase addHeaders(AdaptrisMessage msg, HttpRequestBase target) {
    HttpRequestBase http = target;
    for (RequestHeaderHandler h : getHandlers()) {
      http = h.addHeaders(msg, http);
    }
    return http;
  }


  public List<RequestHeaderHandler> getHandlers() {
    return handlers;
  }


  public void setHandlers(List<RequestHeaderHandler> handlers) {
    this.handlers = Args.notNull(handlers, "Request Header Handlers");
  }

  public void addHandler(RequestHeaderHandler handler) {
    getHandlers().add(Args.notNull(handler, "Request Header Handler"));
  }
}

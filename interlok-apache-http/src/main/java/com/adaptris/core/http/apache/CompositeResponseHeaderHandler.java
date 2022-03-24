package com.adaptris.core.http.apache;

import com.adaptris.annotation.AutoPopulated;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.http.client.ResponseHeaderHandler;
import com.adaptris.core.util.Args;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import org.apache.http.HttpResponse;

/**
 * Implementation of {@link ResponseHeaderHandler} that uses nested handlers to extract headers from a {@link
 * HttpResponse}.
 *
 * <p>This implementation is primarily so that you can mix and match how you capture response headers; If you wanted to use both
 * {@link ResponseHeadersAsMetadata} and {@link ResponseHeadersAsObjectMetadata} then you can.
 * </p>
 * @config apache-http-composite-request-headers
 *
 */
@XStreamAlias("apache-http-composite-response-header-handler")
@NoArgsConstructor
public class CompositeResponseHeaderHandler implements ResponseHeaderHandler<HttpResponse> {

  /** The list of {@link ResponseHeaderHandler} objects that will be used to process
   *  HTTP Response headers.
   */
  @XStreamImplicit
  @NotNull(message="Use an empty list of ResponseHeaderHandlers, not null")
  @NonNull
  @AutoPopulated
  @Valid
  @Getter
  @Setter
  private List<ResponseHeaderHandler<HttpResponse>> handlers = new ArrayList<>();

  @SafeVarargs
  public CompositeResponseHeaderHandler(ResponseHeaderHandler<HttpResponse>... handlers) {
    this();
    for (ResponseHeaderHandler<HttpResponse> h : handlers) {
      addHandler(h);
    }
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

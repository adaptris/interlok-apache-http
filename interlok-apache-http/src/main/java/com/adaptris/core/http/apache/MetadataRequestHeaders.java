package com.adaptris.core.http.apache;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.http.client.methods.HttpRequestBase;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.MetadataCollection;
import com.adaptris.core.MetadataElement;
import com.adaptris.core.http.client.RequestHeaderProvider;
import com.adaptris.core.metadata.MetadataFilter;
import com.adaptris.core.util.Args;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Implementation of {@link RequestHeaderProvider} that applies {@link AdaptrisMessage} metadata as headers using a
 * {@link MetadataFilter}.
 * 
 * @config apache-http-metadata-request-headers
 * 
 */
@XStreamAlias("apache-http-metadata-request-headers")
public class MetadataRequestHeaders implements RequestHeaderProvider<HttpRequestBase> {
  @NotNull
  @Valid
  private MetadataFilter filter;

  public MetadataRequestHeaders() {
  }

  public MetadataRequestHeaders(MetadataFilter mf) {
    this();
    setFilter(mf);
  }

  @Override
  public HttpRequestBase addHeaders(AdaptrisMessage msg, HttpRequestBase target) {
    MetadataCollection metadataSubset = getFilter().filter(msg);
    for (MetadataElement me : metadataSubset) {
      target.addHeader(me.getKey(), me.getValue());
    }
    return target;
  }

  public MetadataFilter getFilter() {
    return filter;
  }

  public void setFilter(MetadataFilter filter) {
    this.filter = Args.notNull(filter, "metadata filter");
  }

}

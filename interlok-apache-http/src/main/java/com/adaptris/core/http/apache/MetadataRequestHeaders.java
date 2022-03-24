package com.adaptris.core.http.apache;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.MetadataCollection;
import com.adaptris.core.MetadataElement;
import com.adaptris.core.http.client.RequestHeaderProvider;
import com.adaptris.core.metadata.MetadataFilter;
import com.adaptris.core.metadata.RemoveAllMetadataFilter;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import javax.validation.Valid;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.http.client.methods.HttpRequestBase;

/**
 * Implementation of {@link RequestHeaderProvider} that applies {@link AdaptrisMessage} metadata as headers using a
 * {@link MetadataFilter}.
 *
 * @config apache-http-metadata-request-headers
 *
 */
@XStreamAlias("apache-http-metadata-request-headers")
@NoArgsConstructor
public class MetadataRequestHeaders implements RequestHeaderProvider<HttpRequestBase> {

  /** Apply a filter to the metadata before adding metadata as HTTP headers.
   *  <p>If not explicitly configured, then defaults to {@link RemoveAllMetadataFilter}</p>
   */
  @Getter
  @Setter
  @Valid
  private MetadataFilter filter;

  public MetadataRequestHeaders(MetadataFilter mf) {
    this();
    setFilter(mf);
  }

  @Override
  public HttpRequestBase addHeaders(AdaptrisMessage msg, HttpRequestBase target) {
    MetadataCollection metadataSubset = filter().filter(msg);
    for (MetadataElement me : metadataSubset) {
      target.addHeader(me.getKey(), me.getValue());
    }
    return target;
  }

  protected MetadataFilter filter() {
    return ObjectUtils.defaultIfNull(getFilter(), new RemoveAllMetadataFilter());
  }
}

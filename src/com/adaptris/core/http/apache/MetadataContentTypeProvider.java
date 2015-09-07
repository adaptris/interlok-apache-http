package com.adaptris.core.http.apache;

import static org.apache.commons.lang.StringUtils.isEmpty;

import org.apache.http.entity.ContentType;
import org.hibernate.validator.constraints.NotBlank;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.CoreException;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Provides a content type derived from metadata.
 * <p>
 * Note that the content type charset will be derived from {@link AdaptrisMessage#getCharEncoding()}
 * so configuring a mime type of {@code text/xml} when the message has a char encoding of
 * {@code UTF-8} will return {@code text/xml; charset="UTF-8"}. Also no validation is done of the
 * metadata value forming the mime-type. it is passed straight through to
 * {@link ContentType#create(String, String)}
 * </p>
 * 
 * @config apache-http-metadata-content-type-provider
 */
@XStreamAlias("apache-http-metadata-content-type-provider")
public class MetadataContentTypeProvider implements ContentTypeProvider {

  @NotBlank
  private String metadataKey;

  public MetadataContentTypeProvider() {
  }

  public MetadataContentTypeProvider(String key) {
    this();
    setMetadataKey(key);
  }
  @Override
  public ContentType getContentType(AdaptrisMessage msg) throws CoreException {
    validate(msg);
    return ContentType.create(msg.getMetadataValue(getMetadataKey()), msg.getCharEncoding());
  }

  private void validate(AdaptrisMessage msg) throws CoreException {
    if (isEmpty(getMetadataKey())) {
      throw new CoreException("metadata key is blank");
    }
    if (!msg.containsKey(getMetadataKey())) {
      throw new CoreException(getMetadataKey() + " not found in message");
    }
    return;
  }


  public String getMetadataKey() {
    return metadataKey;
  }

  /**
   * Set the metadata item containing content type.
   * 
   * @param key the key containing the base content type
   */
  public void setMetadataKey(String key) {
    this.metadataKey = key;
  }

}

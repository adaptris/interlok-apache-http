/*
 * Copyright 2015 Adaptris Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.adaptris.core.http.apache;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.HttpRequestBase;
import org.hibernate.validator.constraints.NotBlank;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.CoreException;
import com.adaptris.core.http.HttpConstants;
import com.adaptris.core.http.auth.ResourceTargetMatcher;
import com.adaptris.core.util.Args;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Build an {@link HttpConstants#AUTHORIZATION} header from metadata.
 * 
 * @config apache-http-metadata-authorization-header
 * 
 */
@XStreamAlias("apache-http-metadata-authorization-header")
public class MetadataAuthorizationHeader implements ApacheRequestAuthenticator {

  @NotBlank
  private String metadataKey;
  
  private transient String headerValue;
  
  public MetadataAuthorizationHeader() {

  }

  public MetadataAuthorizationHeader(String key) {
    this();
    setMetadataKey(key);
  }

  @Override
  public void setup(String target, AdaptrisMessage msg, ResourceTargetMatcher m) throws CoreException {
    headerValue = msg.getMetadataValue(getMetadataKey());
  }

  @Override
  public void close() {
  }

  public String getMetadataKey() {
    return metadataKey;
  }

  /**
   * The metadata key to retrieve the value for the Authorization header from
   */
  public void setMetadataKey(String metadataKey) {
    this.metadataKey = Args.notBlank(metadataKey, "metadataKey");
  }

  @Override
  public void configure(HttpRequestBase req) {
    if (!StringUtils.isBlank(headerValue)) {
      req.addHeader(HttpConstants.AUTHORIZATION, headerValue);
    }
  }
}

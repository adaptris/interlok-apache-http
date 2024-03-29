/*
 * Copyright 2017 Adaptris Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.adaptris.core.http.apache;

import com.adaptris.annotation.AdapterComponent;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.CoreException;
import com.adaptris.core.DynamicPollingTemplate;
import com.adaptris.core.ServiceException;
import com.adaptris.core.StandaloneProducer;
import com.adaptris.core.StandaloneRequestor;
import com.adaptris.core.util.ExceptionHelper;
import com.adaptris.core.util.LifecycleHelper;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.NoArgsConstructor;

/**
 * Direct HTTP support as a service rather wrapped via {@link StandaloneProducer} or {@link StandaloneRequestor}.
 *
 * <p>
 * Note that this service just wraps a {@link ApacheHttpProducer} instance but doesn't expose all the possible settings available
 * for the normal {@link ApacheHttpProducer}. If you need those features, than continue using the producer wrapped as a
 * {@link StandaloneProducer} or {@link StandaloneRequestor}.
 * </p>
 * <p>
 * String parameters in this service will use the {@link AdaptrisMessage#resolve(String)} which allows you to specify metadata
 * values as part of a constant string e.g. {@code setUrl("%message{http_url}")} will use the metadata value associated with the key
 * {@code http_url}.
 * </p>
 *
 * @config apache-http-request-service
 */
@XStreamAlias("apache-http-request-service")
@AdapterComponent
@ComponentProfile(summary = "Make a HTTP(s) request to a remote server using the Apache HTTP Client", tag = "service,http,https", metadata =
{
    "adphttpresponse"

}, author = "Adaptris Ltd")
@DisplayOrder(order = {"url", "method", "contentType", "authenticator", "requestHeaderProvider", "responseHeaderHandler"})
@NoArgsConstructor
public class HttpRequestService extends HttpRequestServiceImpl implements DynamicPollingTemplate.TemplateProvider {

  public HttpRequestService(String url) {
    this();
    setUrl(url);
  }

  @Override
  public void doService(AdaptrisMessage msg) throws ServiceException {
    ApacheHttpProducer p = buildProducer(msg);
    try {
      LifecycleHelper.initAndStart(p);
      p.request(msg);
    }
    catch (CoreException e) {
      throw ExceptionHelper.wrapServiceException(e);
    }
    finally {
      LifecycleHelper.stopAndClose(p);
    }
  }

}

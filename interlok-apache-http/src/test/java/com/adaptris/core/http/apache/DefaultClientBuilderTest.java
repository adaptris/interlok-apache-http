/*
 * Copyright 2018 Adaptris Ltd.
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

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.Test;

import com.adaptris.util.TimeInterval;

public class DefaultClientBuilderTest {

  @Test
  public void testHttpProxy() throws Exception {
    DefaultClientBuilder p = new DefaultClientBuilder().withProxy("http://localhost:1234");
    assertNotNull(p.configure(HttpClients.custom(), 10));
    p.withProxy(":");
    assertNotNull(p.configure(HttpClients.custom(), 10));
  }

  @Test
  public void testAllowRedirect() throws Exception {
    DefaultClientBuilder p = new DefaultClientBuilder().withAllowRedirect(true);
    assertNotNull(p.configure(HttpClients.custom(), 10));
    assertNotNull(p.withAllowRedirect(false).configure(HttpClients.custom(), 10));
  }

  @Test
  public void testConnectTimeout() throws Exception {
    DefaultClientBuilder p = new DefaultClientBuilder().withConnectTimeout(new TimeInterval());
    assertNotNull(p.configure(HttpClients.custom(), 10));
    p.withConnectTimeout(null);
    assertNotNull(p.configure(HttpClients.custom(), 10));
  }

  @Test
  public void testReadTimeout() throws Exception {
    DefaultClientBuilder p = new DefaultClientBuilder().withReadTimeout(new TimeInterval());
    assertNotNull(p.configure(HttpClients.custom(), 10));
    p.withReadTimeout(null);
    assertNotNull(p.configure(HttpClients.custom(), 10));
  }

}

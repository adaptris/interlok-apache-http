/*
 * Copyright 2015 Adaptris Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.adaptris.core.http.apache5;

import static com.adaptris.core.http.apache5.JettyHelper.JETTY_USER_REALM;
import static com.adaptris.core.http.apache5.JettyHelper.METADATA_KEY_CONTENT_TYPE;
import static com.adaptris.core.http.apache5.JettyHelper.URL_TO_POST_TO;
import static com.adaptris.core.http.apache5.JettyHelper.createAndStartChannel;
import static com.adaptris.core.http.apache5.JettyHelper.createChannel;
import static com.adaptris.core.http.apache5.JettyHelper.createConnection;
import static com.adaptris.core.http.apache5.JettyHelper.createConsumer;
import static com.adaptris.core.http.apache5.JettyHelper.createURL;
import static com.adaptris.core.http.apache5.JettyHelper.createWorkflow;
import static com.adaptris.core.http.apache5.JettyHelper.stopAndRelease;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageFactory;
import com.adaptris.core.Channel;
import com.adaptris.core.CoreConstants;
import com.adaptris.core.CoreException;
import com.adaptris.core.DefaultMessageFactory;
import com.adaptris.core.ServiceException;
import com.adaptris.core.ServiceList;
import com.adaptris.core.StandaloneProducer;
import com.adaptris.core.http.HttpConstants;
import com.adaptris.core.http.auth.ConfiguredUsernamePassword;
import com.adaptris.core.http.auth.HttpAuthenticator;
import com.adaptris.core.http.jetty.ConfigurableSecurityHandler;
import com.adaptris.core.http.jetty.HashLoginServiceFactory;
import com.adaptris.core.http.jetty.HttpConnection;
import com.adaptris.core.http.jetty.JettyMessageConsumer;
import com.adaptris.core.http.jetty.SecurityConstraint;
import com.adaptris.core.http.jetty.StandardResponseProducer;
import com.adaptris.core.http.server.HttpStatusProvider.HttpStatus;
import com.adaptris.core.metadata.RegexMetadataFilter;
import com.adaptris.core.services.WaitService;
import com.adaptris.core.services.metadata.PayloadFromTemplateService;
import com.adaptris.core.stubs.MockMessageProducer;
import com.adaptris.core.util.LifecycleHelper;
import com.adaptris.interlok.junit.scaffolding.services.ExampleServiceCase;
import com.adaptris.util.KeyValuePair;
import com.adaptris.util.TimeInterval;

public class HttpRequestServiceTest extends ExampleServiceCase {

  private static final String TEXT = "ABCDEFG";

  @Test
  public void testService_init() throws Exception {
    HttpRequestService service = new HttpRequestService();
    try {
      LifecycleHelper.prepare(service);
      LifecycleHelper.init(service);
      fail();
    } catch (CoreException expected) {

    }
    service.setUrl("http://localhost");
    LifecycleHelper.prepare(service);
    LifecycleHelper.init(service);
    LifecycleHelper.stop(service);
  }

  @Test
  public void testService() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    HttpConnection jc = createConnection();
    JettyMessageConsumer mc = createConsumer(URL_TO_POST_TO);
    Channel c = createChannel(jc, createWorkflow(mc, mock, new ServiceList(new PayloadFromTemplateService().withTemplate(TEXT),
        new StandaloneProducer(new StandardResponseProducer(HttpStatus.OK_200)))));

    HttpRequestService service = new HttpRequestService(createURL(c));
    service.setClientConfig(new NoConnectionManagement());
    service.setMethod("GET");
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage("Hello World");
    try {
      start(c);
      execute(service, msg);
      waitForMessages(mock, 1);
    } finally {
      stop(c);
    }
    assertEquals(1, mock.messageCount());
    AdaptrisMessage m2 = mock.getMessages().get(0);
    assertEquals("GET", m2.getMetadataValue(CoreConstants.HTTP_METHOD));
    assertEquals(TEXT, msg.getContent());
  }

  @Test
  public void testService_WithContentTypeMetadata() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    Channel c = createAndStartChannel(mock);
    HttpRequestService service = new HttpRequestService(createURL(c));
    service.setContentType("%message{" + METADATA_KEY_CONTENT_TYPE + "}");
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    msg.addMetadata(METADATA_KEY_CONTENT_TYPE, "text/complicated");
    try {
      c.requestStart();
      execute(service, msg);
      waitForMessages(mock, 1);
    } finally {
      stopAndRelease(c);
    }
    assertEquals(1, mock.messageCount());
    AdaptrisMessage m2 = mock.getMessages().get(0);
    assertTrue(m2.headersContainsKey("Content-Type"));
    assertEquals("text/complicated", m2.getMetadataValue("Content-Type"));
  }

  @Test
  public void testService_MetadataRequestHeaders() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    Channel c = createAndStartChannel(mock);
    HttpRequestService service = new HttpRequestService(createURL(c));
    service.setRequestHeaderProvider(new MetadataRequestHeaders(new RegexMetadataFilter()));
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    msg.addMetadata(getName(), getName());
    try {
      c.requestStart();
      execute(service, msg);
      waitForMessages(mock, 1);
    } finally {
      stopAndRelease(c);
    }
    assertEquals(1, mock.messageCount());
    AdaptrisMessage m2 = mock.getMessages().get(0);
    assertTrue(m2.headersContainsKey(getName()));
    assertEquals(getName(), m2.getMetadataValue(getName()));
  }

  @Test
  public void testService_WithMetadataMethod() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    HttpConnection jc = createConnection();
    JettyMessageConsumer mc = createConsumer(URL_TO_POST_TO);
    Channel c = createChannel(jc, createWorkflow(mc, mock, new ServiceList(new PayloadFromTemplateService().withTemplate(TEXT),
        new StandaloneProducer(new StandardResponseProducer(HttpStatus.OK_200)))));

    HttpRequestService service = new HttpRequestService(createURL(c));
    service.setMethod("%message{httpMethod}");
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage();
    msg.addMetadata("httpMethod", "get");
    try {
      start(c);
      execute(service, msg);
      waitForMessages(mock, 1);
    } finally {
      stopAndRelease(c);
    }
    assertEquals(1, mock.messageCount());
    AdaptrisMessage m2 = mock.getMessages().get(0);
    assertEquals("GET", m2.getMetadataValue(CoreConstants.HTTP_METHOD));
    assertEquals(TEXT, msg.getContent());
  }

  @Test
  public void testRequest_GetMethod_ZeroBytes() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    HttpConnection jc = createConnection();
    JettyMessageConsumer mc = createConsumer(URL_TO_POST_TO);
    Channel c = createChannel(jc, createWorkflow(mc, mock, new ServiceList(new PayloadFromTemplateService().withTemplate(TEXT),
        new StandaloneProducer(new StandardResponseProducer(HttpStatus.OK_200)))));

    HttpRequestService service = new HttpRequestService(createURL(c));
    service.setMethod("GET");
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage();
    try {
      start(c);
      execute(service, msg);
      waitForMessages(mock, 1);
    } finally {
      stopAndRelease(c);
    }
    assertEquals(1, mock.messageCount());
    AdaptrisMessage m2 = mock.getMessages().get(0);
    assertEquals("GET", m2.getMetadataValue(CoreConstants.HTTP_METHOD));
    assertEquals(TEXT, msg.getContent());
  }

  @Test
  public void testRequest_PostMethod_ZeroBytes() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    HttpConnection jc = createConnection();
    JettyMessageConsumer mc = createConsumer(URL_TO_POST_TO);
    Channel c = createChannel(jc, createWorkflow(mc, mock, new ServiceList(new PayloadFromTemplateService().withTemplate(TEXT),
        new StandaloneProducer(new StandardResponseProducer(HttpStatus.OK_200)))));

    HttpRequestService service = new HttpRequestService(createURL(c));
    service.setMethod("POST");
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage();
    try {
      start(c);
      execute(service, msg);
      waitForMessages(mock, 1);
    } finally {
      stopAndRelease(c);
    }
    assertEquals(1, mock.messageCount());
    AdaptrisMessage m2 = mock.getMessages().get(0);
    assertEquals("POST", m2.getMetadataValue(CoreConstants.HTTP_METHOD));
    assertEquals(TEXT, msg.getContent());
  }

  @Test
  public void testRequest_EmptyReply() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    HttpConnection jc = createConnection();
    JettyMessageConsumer mc = createConsumer(URL_TO_POST_TO);
    StandardResponseProducer responder = new StandardResponseProducer(HttpStatus.OK_200);
    responder.setSendPayload(false);
    Channel c = createChannel(jc, createWorkflow(mc, mock, new ServiceList(new StandaloneProducer(responder))));

    HttpRequestService service = new HttpRequestService(createURL(c));
    service.setMethod("POST");

    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    try {
      start(c);
      execute(service, msg);
      waitForMessages(mock, 1);
    } finally {
      stopAndRelease(c);
    }
    assertEquals(1, mock.messageCount());
    AdaptrisMessage m2 = mock.getMessages().get(0);
    assertEquals("POST", m2.getMetadataValue(CoreConstants.HTTP_METHOD));
    assertEquals(0, msg.getSize());
  }

  @Test
  public void testRequest_MetadataResponseHeaders() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    Channel c = createAndStartChannel(mock);
    HttpRequestService service = new HttpRequestService(createURL(c));
    service.setContentType("%message{" + METADATA_KEY_CONTENT_TYPE + "}");
    service.setResponseHeaderHandler(new ResponseHeadersAsMetadata(""));

    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    msg.addMetadata(METADATA_KEY_CONTENT_TYPE, "text/complicated");
    assertFalse(msg.headersContainsKey("Server"));
    try {
      c.requestStart();
      execute(service, msg);
      waitForMessages(mock, 1);
    } finally {
      stopAndRelease(c);
    }
    assertEquals(1, mock.messageCount());
    AdaptrisMessage m2 = mock.getMessages().get(0);
    assertTrue(m2.headersContainsKey("Content-Type"));
    assertEquals("text/complicated", m2.getMetadataValue("Content-Type"));
    assertTrue(msg.headersContainsKey("Server"));
  }

  // INTERLOK-2682
  @Test
  public void testRequest_ReplyOverwritesExisting() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    Channel c = createAndStartChannel(mock);
    HttpRequestService service = new HttpRequestService(createURL(c));

    service.setContentType("text/plain");
    service.setResponseHeaderHandler(new ResponseHeadersAsMetadata(""));
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    msg.addMetadata("Server", "Hello World");
    try {
      LifecycleHelper.initAndStart(c);
      execute(service, msg);
      waitForMessages(mock, 1);
    } finally {
      stopAndRelease(c);
    }
    assertEquals(1, mock.messageCount());
    assertTrue(msg.headersContainsKey("Server"));
    assertNotEquals("Hello World", msg.getMetadataValue("Server"));
  }

  @Test
  public void testRequest_ObjectMetadataResponseHeaders() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    Channel c = createAndStartChannel(mock);

    HttpRequestService service = new HttpRequestService(createURL(c));
    service.setContentType("%message{" + METADATA_KEY_CONTENT_TYPE + "}");
    service.setResponseHeaderHandler(new ResponseHeadersAsObjectMetadata());

    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    msg.addMetadata(METADATA_KEY_CONTENT_TYPE, "text/complicated");
    assertFalse(msg.headersContainsKey("Server"));
    try {
      c.requestStart();
      execute(service, msg);
      waitForMessages(mock, 1);
    } finally {
      stopAndRelease(c);
    }
    assertFalse(msg.headersContainsKey("Server"));
    assertTrue(msg.getObjectHeaders().containsKey("Server"));
  }

  @Test
  public void testRequest_GetMethod_NonZeroBytes() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    HttpConnection jc = createConnection();
    JettyMessageConsumer mc = createConsumer(URL_TO_POST_TO);
    Channel c = createChannel(jc, createWorkflow(mc, mock, new ServiceList(new PayloadFromTemplateService().withTemplate(TEXT),
        new StandaloneProducer(new StandardResponseProducer(HttpStatus.OK_200)))));

    HttpRequestService service = new HttpRequestService(createURL(c));
    service.setMethod("GET");
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage("Hello World");
    try {
      start(c);
      execute(service, msg);
      waitForMessages(mock, 1);
    } finally {
      stop(c);
    }
    assertEquals(1, mock.messageCount());
    AdaptrisMessage m2 = mock.getMessages().get(0);
    assertEquals("GET", m2.getMetadataValue(CoreConstants.HTTP_METHOD));
    assertEquals(TEXT, msg.getContent());
  }

  @Test
  public void testRequest_GetMethod_NonZeroBytes_WithErrorResponse() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    HttpConnection jc = createConnection();
    JettyMessageConsumer mc = createConsumer(URL_TO_POST_TO);

    Channel c = createChannel(jc, createWorkflow(mc, mock, new ServiceList(new PayloadFromTemplateService().withTemplate(TEXT),
        new StandaloneProducer(new StandardResponseProducer(HttpStatus.UNAUTHORIZED_401)))));
    HttpRequestService service = new HttpRequestService(createURL(c));
    service.setMethod("GET");

    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    try {
      start(c);
      execute(service, msg);
      fail();
    } catch (ServiceException expect) {

    } finally {
      stop(c);
    }
  }

  protected static HttpAuthenticator buildAuthenticator(String username, String password) {
    ConfiguredUsernamePassword auth = new ConfiguredUsernamePassword();
    auth.setUsername(username);
    auth.setPassword(password);
    return auth;
  }

  @Test
  public void testProduce_WithUsernamePassword() throws Exception {
    String threadName = Thread.currentThread().getName();
    Thread.currentThread().setName(getName());

    ConfigurableSecurityHandler csh = new ConfigurableSecurityHandler();
    HashLoginServiceFactory hsl = new HashLoginServiceFactory("InterlokJetty", PROPERTIES.getProperty(JETTY_USER_REALM));
    csh.setLoginService(hsl);
    SecurityConstraint securityConstraint = new SecurityConstraint();
    securityConstraint.setMustAuthenticate(true);
    securityConstraint.setRoles("user");
    csh.setSecurityConstraints(Arrays.asList(securityConstraint));

    HttpConnection jc = createConnection();
    jc.setSecurityHandler(csh);
    MockMessageProducer mockProducer = new MockMessageProducer();
    JettyMessageConsumer consumer = JettyHelper.createConsumer(URL_TO_POST_TO);
    Channel channel = JettyHelper.createChannel(jc, consumer, mockProducer);

    HttpAuthenticator auth = buildAuthenticator("user", "password");

    HttpRequestService service = new HttpRequestService(createURL(channel));
    service.setMethod("POST");
    service.setAuthenticator(auth);
    AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage(TEXT);
    try {
      start(channel);
      execute(service, msg);
      waitForMessages(mockProducer, 1);
      assertEquals(TEXT, mockProducer.getMessages().get(0).getContent());
    } finally {
      stopAndRelease(channel);
      Thread.currentThread().setName(threadName);
    }
  }

  @Test
  public void testRequest_ExpectHeader() throws Exception {
    String threadName = Thread.currentThread().getName();
    Thread.currentThread().setName(getName());

    MockMessageProducer mock = new MockMessageProducer();
    HttpConnection jc = createConnection();
    JettyMessageConsumer mc = createConsumer(URL_TO_POST_TO);
    mc.setSendProcessingInterval(new TimeInterval(100L, TimeUnit.MILLISECONDS));
    ServiceList services = new ServiceList();
    services.add(new PayloadFromTemplateService().withTemplate(TEXT));
    services.add(new WaitService(new TimeInterval(2L, TimeUnit.SECONDS)));
    services.add(new StandaloneProducer(new StandardResponseProducer(HttpStatus.OK_200)));

    Channel c = createChannel(jc, createWorkflow(mc, mock, services));

    HttpRequestService service = new HttpRequestService(createURL(c));
    service.setMethod("GET");
    service.setRequestHeaderProvider(new ConfiguredRequestHeaders().withHeaders(new KeyValuePair(HttpConstants.EXPECT, "102-Processing")));
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage("Hello World");

    try {
      start(c);
      start(service);
      service.doService(msg);
      assertEquals(TEXT, msg.getContent());
    } finally {
      stopAndRelease(c);
      stop(service);
      Thread.currentThread().setName(threadName);
    }
  }

  @Override
  protected Object retrieveObjectForSampleConfig() {
    HttpRequestService service = new HttpRequestService("http://myhost.com/url/to/post/to");
    service.setAuthenticator(buildAuthenticator("username", "password"));
    return service;
  }

}

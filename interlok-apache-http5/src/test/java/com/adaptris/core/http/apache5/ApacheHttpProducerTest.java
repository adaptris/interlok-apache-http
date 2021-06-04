package com.adaptris.core.http.apache5;

import static com.adaptris.core.http.apache5.JettyHelper.JETTY_USER_REALM;
import static com.adaptris.core.http.apache5.JettyHelper.METADATA_KEY_CONTENT_TYPE;
import static com.adaptris.core.http.apache5.JettyHelper.TEXT;
import static com.adaptris.core.http.apache5.JettyHelper.URL_TO_POST_TO;
import static com.adaptris.core.http.apache5.JettyHelper.createAndStartChannel;
import static com.adaptris.core.http.apache5.JettyHelper.createChannel;
import static com.adaptris.core.http.apache5.JettyHelper.createConnection;
import static com.adaptris.core.http.apache5.JettyHelper.createConsumer;
import static com.adaptris.core.http.apache5.JettyHelper.createURL;
import static com.adaptris.core.http.apache5.JettyHelper.createWorkflow;
import static com.adaptris.core.http.apache5.JettyHelper.stopAndRelease;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.adaptris.core.http.apache5.request.AcceptEncoding;
import com.adaptris.core.http.apache5.request.BasicHMACSignature;
import com.adaptris.core.http.apache5.request.DateHeader;
import com.adaptris.core.http.apache5.request.HMACSignatureImpl;
import com.adaptris.core.http.apache5.request.RemoveHeaders;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageFactory;
import com.adaptris.core.Channel;
import com.adaptris.core.CoreConstants;
import com.adaptris.core.DefaultMessageFactory;
import com.adaptris.core.MetadataElement;
import com.adaptris.core.ServiceCase;
import com.adaptris.core.ServiceException;
import com.adaptris.core.ServiceList;
import com.adaptris.core.StandaloneProducer;
import com.adaptris.core.StandaloneRequestor;
import com.adaptris.core.http.ConfiguredContentTypeProvider;
import com.adaptris.core.http.HttpConstants;
import com.adaptris.core.http.RawContentTypeProvider;
import com.adaptris.core.http.auth.AdapterResourceAuthenticator;
import com.adaptris.core.http.auth.ConfiguredUsernamePassword;
import com.adaptris.core.http.auth.MetadataUsernamePassword;
import com.adaptris.core.http.client.ConfiguredRequestMethodProvider;
import com.adaptris.core.http.client.RequestMethodProvider.RequestMethod;
import com.adaptris.core.http.jetty.ConfigurableSecurityHandler;
import com.adaptris.core.http.jetty.HashLoginServiceFactory;
import com.adaptris.core.http.jetty.HttpConnection;
import com.adaptris.core.http.jetty.JettyConstants;
import com.adaptris.core.http.jetty.JettyMessageConsumer;
import com.adaptris.core.http.jetty.JettyResponseService;
import com.adaptris.core.http.jetty.MetadataResponseHeaderProvider;
import com.adaptris.core.http.jetty.SecurityConstraint;
import com.adaptris.core.http.jetty.StandardResponseProducer;
import com.adaptris.core.http.server.HttpStatusProvider.HttpStatus;
import com.adaptris.core.metadata.NoOpMetadataFilter;
import com.adaptris.core.metadata.RemoveAllMetadataFilter;
import com.adaptris.core.services.WaitService;
import com.adaptris.core.services.metadata.AddMetadataService;
import com.adaptris.core.services.metadata.PayloadFromTemplateService;
import com.adaptris.core.stubs.MockMessageProducer;
import com.adaptris.core.util.LifecycleHelper;
import com.adaptris.interlok.junit.scaffolding.ExampleProducerCase;
import com.adaptris.util.GuidGeneratorWithTime;
import com.adaptris.util.KeyValuePair;
import com.adaptris.util.TimeInterval;
import com.adaptris.util.text.Conversion;

@SuppressWarnings("deprecation")
public class ApacheHttpProducerTest extends ExampleProducerCase {

  protected static Logger log = LoggerFactory.getLogger(ApacheHttpProducerTest.class);


  @Test
  public void testSetIgnoreServerResponse() throws Exception {
    ApacheHttpProducer p = new ApacheHttpProducer();
    assertFalse(p.ignoreServerResponseCode());
    p.setIgnoreServerResponseCode(true);
    assertNotNull(p.getIgnoreServerResponseCode());
    assertEquals(Boolean.TRUE, p.getIgnoreServerResponseCode());
    assertTrue(p.ignoreServerResponseCode());
    p.setIgnoreServerResponseCode(false);
    assertNotNull(p.getIgnoreServerResponseCode());
    assertEquals(Boolean.FALSE, p.getIgnoreServerResponseCode());
    assertFalse(p.ignoreServerResponseCode());
  }


  @Test
  public void testSetRequestHandler() throws Exception {
    ApacheHttpProducer p = new ApacheHttpProducer();
    assertEquals(NoOpRequestHeaders.class, p.getRequestHeaderProvider().getClass());
    try {
      p.setRequestHeaderProvider(null);
      fail();
    } catch (Exception expected) {

    }
    assertEquals(NoOpRequestHeaders.class, p.getRequestHeaderProvider().getClass());
    p.setRequestHeaderProvider(new MetadataRequestHeaders(new RemoveAllMetadataFilter()));
    assertEquals(MetadataRequestHeaders.class, p.getRequestHeaderProvider().getClass());
  }


  @Test
  public void testSetResponseHandler() throws Exception {
    ApacheHttpProducer p = new ApacheHttpProducer();
    assertEquals(DiscardResponseHeaders.class, p.getResponseHeaderHandler().getClass());
    try {
      p.setResponseHeaderHandler(null);
      fail();
    } catch (Exception expected) {

    }
    assertEquals(DiscardResponseHeaders.class, p.getResponseHeaderHandler().getClass());
    p.setResponseHeaderHandler(new ResponseHeadersAsMetadata());
    assertEquals(ResponseHeadersAsMetadata.class, p.getResponseHeaderHandler().getClass());
  }

  private static void doAuthenticatedProduce(MockMessageProducer mock, ApacheHttpProducer http, AdaptrisMessage msg)
      throws Exception {
    Channel c = JettyHelper.createChannel(createConnection(createSecurityWrapper()), JettyHelper.createConsumer(URL_TO_POST_TO),
        mock);
    try {
      start(c);
      http.setUrl(createURL(c));
      StandaloneProducer producer = new StandaloneProducer(http);
      ServiceCase.execute(producer, msg);
      waitForMessages(mock, 1);
    } finally {
      stopAndRelease(c);
    }
  }

  private static void doProduce(MockMessageProducer mock, ApacheHttpProducer http, AdaptrisMessage msg) throws Exception {
    Channel c = createAndStartChannel(mock);
    http.setUrl(createURL(c));
    StandaloneProducer producer = new StandaloneProducer(http);
    try {
      ServiceCase.execute(producer, msg);
      waitForMessages(mock, 1);
    } finally {
      stopAndRelease(c);
    }
  }

  private static void doRequest(MockMessageProducer mock, ApacheHttpProducer http, AdaptrisMessage msg) throws Exception {
    Channel c = createAndStartChannel(mock);
    http.setUrl(createURL(c));
    StandaloneRequestor producer = new StandaloneRequestor(http);
    try {
      ServiceCase.execute(producer, msg);
      waitForMessages(mock, 1);
    } finally {
      stopAndRelease(c);
    }
  }

  private static void doAuthenticatedRequest(MockMessageProducer mock, ApacheHttpProducer http, AdaptrisMessage msg)
      throws Exception {
    Channel c = JettyHelper.createChannel(createConnection(createSecurityWrapper()), JettyHelper.createConsumer(URL_TO_POST_TO),
        mock);
    http.setUrl(createURL(c));
    StandaloneRequestor producer = new StandaloneRequestor(http);
    try {
      start(c);
      ServiceCase.execute(producer, msg);
      waitForMessages(mock, 1);
    } finally {
      stopAndRelease(c);
    }
  }

  @Test
  public void testMethod() throws Exception {
    for (HttpProducer.HttpMethod m : HttpProducer.HttpMethod.values()) {
      assertNotNull(m.create("http://localhost:8080/index.html"));
    }
  }

  @Test
  public void testProduce() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    ApacheHttpProducer http = new ApacheHttpProducer();
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    msg.addMetadata(METADATA_KEY_CONTENT_TYPE, "text/complicated");
    doProduce(mock, http, msg);
    doAssertions(mock, true);
    AdaptrisMessage m2 = mock.getMessages().get(0);
    assertTrue(m2.headersContainsKey("User-Agent"));
    assertFalse(m2.headersContainsKey(METADATA_KEY_CONTENT_TYPE));
    assertEquals("text/plain", m2.getMetadataValue("Content-Type"));
  }

  @Test
  public void testProducerReuse() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    ApacheHttpProducer http = new ApacheHttpProducer();
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    msg.addMetadata(METADATA_KEY_CONTENT_TYPE, "text/complicated");
    Channel c = createAndStartChannel(mock);
    http.setUrl(createURL(c));
    StandaloneProducer producer = new StandaloneProducer(http);

    start(producer);
    producer.doService(msg);

    /* go again, reusing the same producer */
    msg = new DefaultMessageFactory().newMessage(TEXT);
    msg.addMetadata(METADATA_KEY_CONTENT_TYPE, "text/complicated");

    producer.doService(msg);

    stop(producer);
    waitForMessages(mock, 2);
    stopAndRelease(c);
    doAssertions(mock, 2, true);
    AdaptrisMessage m2 = mock.getMessages().get(0);
    assertTrue(m2.headersContainsKey("User-Agent"));
    assertFalse(m2.headersContainsKey(METADATA_KEY_CONTENT_TYPE));
    assertEquals("text/plain", m2.getMetadataValue("Content-Type"));
  }

  @Test
  public void testProduce_WithInterceptors() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    ApacheHttpProducer http = new ApacheHttpProducer();
    // empty RequestInterceptorClientBuilder just to get an empty list for coverage
    CompositeClientBuilder builder = new CompositeClientBuilder().withBuilders(new DefaultClientBuilder(),
        new RequestInterceptorClientBuilder().withInterceptors(new RemoveHeaders("Accept-Encoding", "User-Agent", "Connection"),
            new AcceptEncoding("gzip", "compress", "deflate", "*"),
            new DateHeader()),
        new RequestInterceptorClientBuilder());
    http.setClientConfig(builder);
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    doProduce(mock, http, msg);
    doAssertions(mock, true);
    AdaptrisMessage m2 = mock.getMessages().get(0);
    // User-Agent should have been removed.
    assertTrue(m2.headersContainsKey("Date"));
    assertFalse(m2.headersContainsKey("User-Agent"));
    assertFalse(m2.headersContainsKey(METADATA_KEY_CONTENT_TYPE));
    assertEquals("text/plain", m2.getMetadataValue("Content-Type"));
  }

  @Test
  public void testProduce_WithFewerInterceptors() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    ApacheHttpProducer http = new ApacheHttpProducer();
    // empty RequestInterceptorClientBuilder just to get an empty list for coverage
    CompositeClientBuilder builder = new CompositeClientBuilder().withBuilders(new DefaultClientBuilder(),
            new RequestInterceptorClientBuilder().withInterceptors(new AcceptEncoding()));
    http.setClientConfig(builder);
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    doProduce(mock, http, msg);
    doAssertions(mock, true);
    AdaptrisMessage m2 = mock.getMessages().get(0);
    // User-Agent should have been removed.
    assertTrue(m2.headersContainsKey("Accept-Encoding"));
    assertEquals("gzip, x-gzip, deflate", m2.getMetadataValue("Accept-Encoding"));
  }

  @Test
  public void testProduce_With_HMAC() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    ApacheHttpProducer http = new ApacheHttpProducer();
    BasicHMACSignature hmac = new BasicHMACSignature().withIdentity("MyIdentity").withHeaders("Date")
        .withSecretKey("MySecretKey").withTargetHeader("hmac").withEncoding(HMACSignatureImpl.Encoding.BASE64)
        .withHmacAlgorithm(HMACSignatureImpl.Algorithm.HMAC_SHA256);
    CompositeClientBuilder builder = new CompositeClientBuilder().withBuilders(new DefaultClientBuilder(),
        new RequestInterceptorClientBuilder().withInterceptors(new RemoveHeaders("Accept-Encoding"), new AcceptEncoding(), new DateHeader(), hmac));
    http.setClientConfig(builder);
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    doProduce(mock, http, msg);
    doAssertions(mock, true);
    AdaptrisMessage m2 = mock.getMessages().get(0);
    // User-Agent should have been removed.
    assertTrue(m2.headersContainsKey("Date"));
    assertTrue(m2.headersContainsKey("hmac"));
    assertTrue(m2.getMetadataValue("hmac").startsWith("MyIdentity:"));
    assertFalse(m2.headersContainsKey(METADATA_KEY_CONTENT_TYPE));
    assertEquals("text/plain", m2.getMetadataValue("Content-Type"));
  }

  @Test
  public void testProduce_WithMetadata() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    ApacheHttpProducer http = new ApacheHttpProducer();
    http.setRequestHeaderProvider(new MetadataRequestHeaders(new NoOpMetadataFilter()));
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    msg.addMetadata(METADATA_KEY_CONTENT_TYPE, "text/complicated");
    doProduce(mock, http, msg);
    doAssertions(mock, true);
    AdaptrisMessage m2 = mock.getMessages().get(0);
    assertTrue(m2.headersContainsKey(METADATA_KEY_CONTENT_TYPE));
    assertEquals("text/plain", m2.getMetadataValue("Content-Type"));
  }

  @Test
  public void testProduce_ReplyHasCharset() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    HttpConnection jc = createConnection();
    JettyMessageConsumer mc = createConsumer(URL_TO_POST_TO);

    ServiceList sl = new ServiceList();
    sl.add(new AddMetadataService(Arrays.asList(new MetadataElement(getName(), "text/plain; charset=UTF-8"))));
    StandardResponseProducer responder = new StandardResponseProducer(HttpStatus.OK_200);
    responder.setContentTypeProvider(new com.adaptris.core.http.MetadataContentTypeProvider(getName()));
    sl.add(new StandaloneProducer(responder));
    Channel c = createChannel(jc, createWorkflow(mc, mock, sl));

    ApacheHttpProducer http = new ApacheHttpProducer().withURL(createURL(c));
    http.setMethodProvider(new ConfiguredRequestMethodProvider(RequestMethod.POST));
    StandaloneRequestor producer = new StandaloneRequestor(http);
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    try {
      start(c);
      ServiceCase.execute(producer, msg);
      waitForMessages(mock, 1);
    } finally {
      stopAndRelease(c);
    }
    assertEquals("UTF-8", msg.getContentEncoding());
  }


  @Test
  public void testProduce_ReplyHasNoData() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    HttpConnection jc = createConnection();
    JettyMessageConsumer mc = createConsumer(URL_TO_POST_TO);

    ServiceList sl = new ServiceList();
    StandardResponseProducer responder = new StandardResponseProducer(HttpStatus.OK_200);
    responder.setSendPayload(false);
    sl.add(new StandaloneProducer(responder));
    Channel c = createChannel(jc, createWorkflow(mc, mock, sl));

    ApacheHttpProducer http = new ApacheHttpProducer().withURL(createURL(c));
    http.setMethodProvider(new ConfiguredRequestMethodProvider(RequestMethod.POST));
    StandaloneRequestor producer = new StandaloneRequestor(http);
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    try {
      start(c);
      ServiceCase.execute(producer, msg);
      waitForMessages(mock, 1);
    } finally {
      stopAndRelease(c);
    }
    doAssertions(mock, true);
    assertEquals(0, msg.getSize());
  }

  @Test
  public void testProduce_WithContentTypeMetadata() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    ApacheHttpProducer http = new ApacheHttpProducer();
    http.setContentTypeProvider(new RawContentTypeProvider("%message{" + METADATA_KEY_CONTENT_TYPE + "}"));
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    msg.addMetadata(METADATA_KEY_CONTENT_TYPE, "text/complicated");
    doProduce(mock, http, msg);
    doAssertions(mock, true);
    AdaptrisMessage m2 = mock.getMessages().get(0);
    assertTrue(m2.headersContainsKey("Content-Type"));
    assertFalse(m2.headersContainsKey(METADATA_KEY_CONTENT_TYPE));
    assertEquals("text/complicated", m2.getMetadataValue("Content-Type"));
  }

  @Test
  public void testRequest_GetMethod_ZeroBytes() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    ApacheHttpProducer http = new ApacheHttpProducer();
    http.setMethodProvider(new ConfiguredRequestMethodProvider(RequestMethod.GET));
    doRequest(mock, http, new DefaultMessageFactory().newMessage());
    doAssertions(mock, false);
    AdaptrisMessage m2 = mock.getMessages().get(0);
    assertEquals("GET", m2.getMetadataValue(CoreConstants.HTTP_METHOD));
    assertEquals(0, m2.getSize());
  }

  @Test
  public void testRequest_GetMethod_NonZeroBytes() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    ApacheHttpProducer http = new ApacheHttpProducer();
    http.setMethodProvider(new ConfiguredRequestMethodProvider(RequestMethod.GET));
    StandaloneRequestor producer = new StandaloneRequestor(http);
    doRequest(mock, http, new DefaultMessageFactory().newMessage(TEXT));
    doAssertions(mock, false);
    AdaptrisMessage m2 = mock.getMessages().get(0);
    assertEquals("GET", m2.getMetadataValue(CoreConstants.HTTP_METHOD));
    assertEquals(TEXT.length(), m2.getSize());
  }

  @Test
  public void testRequest_ProduceException_401() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    HttpConnection jc = createConnection();
    JettyMessageConsumer mc = createConsumer(URL_TO_POST_TO);
    ServiceList services = new ServiceList();
    services.add(new StandaloneProducer(new StandardResponseProducer(HttpStatus.UNAUTHORIZED_401)));
    Channel c = createChannel(jc, createWorkflow(mc, mock, services));
    StandaloneRequestor producer =
        new StandaloneRequestor(new ApacheHttpProducer().withURL(createURL(c)));
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    try {
      start(c);
      ServiceCase.execute(producer, msg);
      fail();
    } catch (ServiceException expected) {

    }
    finally {
      stopAndRelease(c);
      stop(producer);
    }
  }

  @Test
  public void testRequest_WithErrorResponse() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    HttpConnection jc = createConnection();
    JettyMessageConsumer mc = createConsumer(URL_TO_POST_TO);
    ServiceList services = new ServiceList();

    services.add(new StandaloneProducer(new StandardResponseProducer(HttpStatus.UNAUTHORIZED_401)));
    Channel c = createChannel(jc, createWorkflow(mc, mock, services));
    ApacheHttpProducer http = new ApacheHttpProducer().withURL(createURL(c));
    http.setMethodProvider(new ConfiguredRequestMethodProvider(RequestMethod.POST));
    http.setIgnoreServerResponseCode(true);
    http.setResponseHeaderHandler(new ResponseHeadersAsMetadata("HTTP_"));
    StandaloneRequestor producer = new StandaloneRequestor(http);
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    try {
      start(c);
      ServiceCase.execute(producer, msg);
      waitForMessages(mock, 1);
    } finally {
      stopAndRelease(c);
      stop(producer);
    }
    doAssertions(mock, false);
    AdaptrisMessage m2 = mock.getMessages().get(0);
    assertEquals("POST", m2.getMetadataValue(CoreConstants.HTTP_METHOD));
    assertEquals("401", msg.getMetadataValue(CoreConstants.HTTP_PRODUCER_RESPONSE_CODE));
    assertNotNull(msg.getMetadata("HTTP_Server"));
  }

  @Test
  public void testProduce_WithDynamicUsernamePassword() throws Exception {
    String threadName = Thread.currentThread().getName();
    Thread.currentThread().setName(getName());
    MockMessageProducer mock = new MockMessageProducer();
    ApacheHttpProducer http = new ApacheHttpProducer();
    try {
      AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage(TEXT);
      DynamicBasicAuthorizationHeader auth = new DynamicBasicAuthorizationHeader("user", "password");
      http.setAuthenticator(auth);
      doAuthenticatedProduce(mock, http, msg);
      doAssertions(mock, true);
    } finally {
      Thread.currentThread().setName(threadName);
      assertEquals(0, AdapterResourceAuthenticator.getInstance().currentAuthenticators().size());
    }
  }


  @Test
  public void testProduce_WithUsernamePassword_BadCredentials() throws Exception {
    String threadName = Thread.currentThread().getName();
    Thread.currentThread().setName(getName());
    MockMessageProducer mock = new MockMessageProducer();
    ApacheHttpProducer http = new ApacheHttpProducer();
    try {
      AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage(TEXT);
      http.setAuthenticator(new ConfiguredUsernamePassword(getName(), getName()));
      doAuthenticatedProduce(mock, http, msg);
      fail();
    } catch (ServiceException expected) {

    } finally {
      Thread.currentThread().setName(threadName);
      assertEquals(0, AdapterResourceAuthenticator.getInstance().currentAuthenticators().size());
    }
  }

  @Test
  public void testProduce_WithMetadataCredentials() throws Exception {
    String threadName = Thread.currentThread().getName();
    Thread.currentThread().setName(getName());
    MockMessageProducer mock = new MockMessageProducer();
    ApacheHttpProducer http = new ApacheHttpProducer();
    try {
      AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage(TEXT);
      msg.addMetadata("apacheUser", "user");
      msg.addMetadata("apachePassword", "password");
      MetadataUsernamePassword acl = new MetadataUsernamePassword();
      acl.setPasswordMetadataKey("apachePassword");
      acl.setUsernameMetadataKey("apacheUser");
      http.setAuthenticator(acl);
      doAuthenticatedProduce(mock, http, msg);
      doAssertions(mock, true);
    }
    finally {
      Thread.currentThread().setName(threadName);
      assertEquals(0, AdapterResourceAuthenticator.getInstance().currentAuthenticators().size());
    }
  }

  @Test
  public void testProduce_WithAuthHeader() throws Exception {
    String threadName = Thread.currentThread().getName();
    Thread.currentThread().setName(getName());
    MockMessageProducer mock = new MockMessageProducer();
    ApacheHttpProducer http = new ApacheHttpProducer();
    try {
      AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage(TEXT);
      ConfiguredAuthorizationHeader acl = new ConfiguredAuthorizationHeader(buildAuthHeader("user", "password"));
      http.setAuthenticator(acl);
      doAuthenticatedProduce(mock, http, msg);
      doAssertions(mock, true);
    }
    finally {
      Thread.currentThread().setName(threadName);
      assertEquals(0, AdapterResourceAuthenticator.getInstance().currentAuthenticators().size());
    }
  }

  @Test
  public void testRequest_WithReplyAsMetadata() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    HttpConnection jc = createConnection();
    JettyMessageConsumer mc = createConsumer(URL_TO_POST_TO);

    ServiceList sl = new ServiceList();
    sl.add(new AddMetadataService(Arrays.asList(new MetadataElement(getName(), "text/plain; charset=UTF-8"))));
    StandardResponseProducer responder = new StandardResponseProducer(HttpStatus.OK_200);
    responder.setContentTypeProvider(new ConfiguredContentTypeProvider("%message{" + getName() + "}"));

    sl.add(new StandaloneProducer(responder));
    Channel c = LifecycleHelper.initAndStart(createChannel(jc, createWorkflow(mc, mock, sl)));

    ApacheHttpProducer http = new ApacheHttpProducer().withURL(createURL(c));
    http.setResponseHandlerFactory(new MetadataResponseHandlerFactory(getName()));
    StandaloneRequestor requestor = new StandaloneRequestor(http);
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    try {
      ServiceCase.execute(requestor, msg);
      waitForMessages(mock, 1);
    } finally {
      stopAndRelease(c);
    }
    doAssertions(mock, false);

    assertTrue(msg.headersContainsKey(getName()));
    assertEquals(TEXT, msg.getMetadataValue(getName()));
  }


  // INTERLOK-2682
  @Test
  public void test_ReplyMetadata_ShouldNotOverwrite() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    HttpConnection jc = createConnection();
    JettyMessageConsumer mc = createConsumer(URL_TO_POST_TO);

    ServiceList sl = new ServiceList();
    sl.add(new AddMetadataService(
        Arrays.asList(new MetadataElement(getName(), "text/plain; charset=UTF-8"))));
    // Should at least return "getName()" as a metadata key...
    sl.add(new JettyResponseService().withContentType("%message{" + getName() + "}")
        .withHttpStatus("200").withResponseHeaderProvider(
            new MetadataResponseHeaderProvider(new NoOpMetadataFilter())));
    Channel c = LifecycleHelper.initAndStart(createChannel(jc, createWorkflow(mc, mock, sl)));

    ApacheHttpProducer http = new ApacheHttpProducer().withURL(createURL(c));
    http.setResponseHeaderHandler(new ResponseHeadersAsMetadata());
    StandaloneRequestor requestor = new StandaloneRequestor(http);
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    msg.addMetadata(getName(), "original metadata value that should get overwritten");
    try {
      ServiceCase.execute(requestor, msg);
      waitForMessages(mock, 1);
    } finally {
      stopAndRelease(c);
    }
    assertTrue(msg.headersContainsKey(getName()));
    assertEquals("text/plain; charset=UTF-8", msg.getMetadataValue(getName()));
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

    Channel c = LifecycleHelper.initAndStart(createChannel(jc, createWorkflow(mc, mock, services)));

    ApacheHttpProducer http = new ApacheHttpProducer().withURL(createURL(c));
    http.setMethodProvider(new ConfiguredRequestMethodProvider(RequestMethod.GET));
    http.setRequestHeaderProvider(
        new ConfiguredRequestHeaders().withHeaders(new KeyValuePair(HttpConstants.EXPECT, "102-Processing")));
    StandaloneRequestor requestor = new StandaloneRequestor(http);
    requestor.setReplyTimeout(new TimeInterval(60L, TimeUnit.SECONDS));
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage("Hello World");

    try {
      ServiceCase.execute(requestor, msg);
      assertEquals(TEXT, msg.getContent());
    }
    finally {
      stopAndRelease(c);
      Thread.currentThread().setName(threadName);
    }
  }


  // This is INTERLOK-3396 effectively. Send data, but put the reply into metadata.
  @Test
  public void testRequest_ReplyIntoMetadata() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    String responseText = new GuidGeneratorWithTime().create(this);
    HttpConnection jc = createConnection();
    JettyMessageConsumer mc = createConsumer(URL_TO_POST_TO);

    ServiceList services = new ServiceList();
    services.add(new PayloadFromTemplateService().withTemplate(responseText));
    services.add(new JettyResponseService(HttpStatus.OK_200.getStatusCode(), "text/plain"));
    Channel c = createChannel(jc, createWorkflow(mc, mock, services));

    ApacheHttpProducer http = new ApacheHttpProducer().withURL(createURL(c));
    http.setMethodProvider(new ConfiguredRequestMethodProvider(RequestMethod.POST));
    http.setResponseHandlerFactory(new MetadataResponseHandlerFactory("httpReplyPayload"));

    StandaloneRequestor producer = new StandaloneRequestor(http);
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    try {
      start(c);
      ServiceCase.execute(producer, msg);
      waitForMessages(mock, 1);
    } finally {
      stopAndRelease(c);
    }
    assertTrue(msg.containsKey("httpReplyPayload"));
    assertEquals(responseText, msg.getMetadataValue("httpReplyPayload"));
    assertEquals(TEXT, msg.getContent());
  }


  @Override
  protected Object retrieveObjectForSampleConfig() {
    ApacheHttpProducer producer =
        new ApacheHttpProducer().withURL("http://myhost.com/url/to/post/to");
    producer.setAuthenticator(new ConfiguredUsernamePassword("user", "password"));
    producer.setClientConfig(new CompositeClientBuilder().withBuilders(new DefaultClientBuilder().withProxy("http://my.proxy:3128"),
        new CustomTlsBuilder().withHostnameVerification(CustomTlsBuilder.HostnameVerification.NONE), new NoConnectionManagement(),
        new RequestInterceptorClientBuilder().withInterceptors(new RemoveHeaders("User-Agent"))));
    StandaloneProducer result = new StandaloneProducer(producer);
    return result;
  }

  protected AdaptrisMessage doAssertions(MockMessageProducer mockProducer, boolean assertPayload) {
    return doAssertions(mockProducer, 1, assertPayload);
  }

  protected AdaptrisMessage doAssertions(MockMessageProducer mockProducer, int expected, boolean assertPayload) {
    assertEquals(expected, mockProducer.getMessages().size());
    AdaptrisMessage msg = mockProducer.getMessages().get(0);
    if (assertPayload) {
      assertEquals(TEXT, msg.getContent());
    }
    assertTrue(msg.headersContainsKey(JettyConstants.JETTY_URI));
    assertEquals(URL_TO_POST_TO, msg.getMetadataValue(JettyConstants.JETTY_URI));
    assertTrue(msg.headersContainsKey(JettyConstants.JETTY_URL));
    return msg;
  }

  private static String buildAuthHeader(String user, String password) {

    String authString = "";
    if (user != null && user.length() > 0) {
      String source = user + ":" + password;
      authString = "Basic " + Conversion.byteArrayToBase64String(source.getBytes());
    }
    return authString;
  }

  private static ConfigurableSecurityHandler createSecurityWrapper() {
    ConfigurableSecurityHandler handler = new ConfigurableSecurityHandler();
    HashLoginServiceFactory login = new HashLoginServiceFactory("InterlokJetty", PROPERTIES.getProperty(JETTY_USER_REALM));
    SecurityConstraint securityConstraint = new SecurityConstraint();
    securityConstraint.setMustAuthenticate(true);
    securityConstraint.setRoles("user");

    handler.setSecurityConstraints(Arrays.asList(securityConstraint));
    handler.setLoginService(login);
    return handler;
  }

}

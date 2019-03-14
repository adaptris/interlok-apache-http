package com.adaptris.core.http.apache;

import static com.adaptris.core.http.apache.JettyHelper.JETTY_USER_REALM;
import static com.adaptris.core.http.apache.JettyHelper.METADATA_KEY_CONTENT_TYPE;
import static com.adaptris.core.http.apache.JettyHelper.TEXT;
import static com.adaptris.core.http.apache.JettyHelper.URL_TO_POST_TO;
import static com.adaptris.core.http.apache.JettyHelper.createAndStartChannel;
import static com.adaptris.core.http.apache.JettyHelper.createChannel;
import static com.adaptris.core.http.apache.JettyHelper.createConnection;
import static com.adaptris.core.http.apache.JettyHelper.createConsumer;
import static com.adaptris.core.http.apache.JettyHelper.createProduceDestination;
import static com.adaptris.core.http.apache.JettyHelper.createWorkflow;
import static com.adaptris.core.http.apache.JettyHelper.stopAndRelease;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageFactory;
import com.adaptris.core.Channel;
import com.adaptris.core.ConfiguredProduceDestination;
import com.adaptris.core.CoreConstants;
import com.adaptris.core.DefaultMessageFactory;
import com.adaptris.core.MetadataElement;
import com.adaptris.core.ProducerCase;
import com.adaptris.core.ServiceCase;
import com.adaptris.core.ServiceException;
import com.adaptris.core.ServiceList;
import com.adaptris.core.StandaloneProducer;
import com.adaptris.core.StandaloneRequestor;
import com.adaptris.core.http.ConfiguredContentTypeProvider;
import com.adaptris.core.http.HttpConstants;
import com.adaptris.core.http.RawContentTypeProvider;
import com.adaptris.core.http.apache.CustomTlsBuilder.HostnameVerification;
import com.adaptris.core.http.apache.HttpProducer.HttpMethod;
import com.adaptris.core.http.apache.request.BasicHMACSignature;
import com.adaptris.core.http.apache.request.DateHeader;
import com.adaptris.core.http.apache.request.HMACSignatureImpl.Algorithm;
import com.adaptris.core.http.apache.request.HMACSignatureImpl.Encoding;
import com.adaptris.core.http.apache.request.RemoveHeaders;
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
import com.adaptris.core.services.metadata.PayloadFromMetadataService;
import com.adaptris.core.stubs.MockMessageProducer;
import com.adaptris.core.util.LifecycleHelper;
import com.adaptris.util.KeyValuePair;
import com.adaptris.util.TimeInterval;
import com.adaptris.util.text.Conversion;

@SuppressWarnings("deprecation")
public class ApacheHttpProducerTest extends ProducerCase {

  protected static Logger log = LoggerFactory.getLogger(ApacheHttpProducerTest.class);

  public ApacheHttpProducerTest(String name) {
    super(name);
  }

  @Override
  protected void setUp() throws Exception {
  }

  public void testSetHandleRedirection() throws Exception {
    ApacheHttpProducer p = new ApacheHttpProducer();
    p.setAllowRedirect(true);
    assertNotNull(p.getAllowRedirect());
    assertEquals(Boolean.TRUE, p.getAllowRedirect());
    p.setAllowRedirect(false);
    assertNotNull(p.getAllowRedirect());
    assertEquals(Boolean.FALSE, p.getAllowRedirect());
  }

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


  public void testSetRequestHandler() throws Exception {
    ApacheHttpProducer p = new ApacheHttpProducer();
    assertEquals(NoOpRequestHeaders.class, p.getRequestHeaderProvider().getClass());
    try {
      p.setRequestHeaderProvider(null);
      fail();
    } catch (IllegalArgumentException expected) {

    }
    assertEquals(NoOpRequestHeaders.class, p.getRequestHeaderProvider().getClass());
    p.setRequestHeaderProvider(new MetadataRequestHeaders(new RemoveAllMetadataFilter()));
    assertEquals(MetadataRequestHeaders.class, p.getRequestHeaderProvider().getClass());
  }


  public void testSetResponseHandler() throws Exception {
    ApacheHttpProducer p = new ApacheHttpProducer();
    assertEquals(DiscardResponseHeaders.class, p.getResponseHeaderHandler().getClass());
    try {
      p.setResponseHeaderHandler(null);
      fail();
    } catch (IllegalArgumentException expected) {

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
      http.setDestination(createProduceDestination(c));
      StandaloneProducer producer = new StandaloneProducer(http);
      ServiceCase.execute(producer, msg);
      waitForMessages(mock, 1);
    } finally {
      stopAndRelease(c);
    }
  }

  private static void doProduce(MockMessageProducer mock, ApacheHttpProducer http, AdaptrisMessage msg) throws Exception {
    Channel c = createAndStartChannel(mock);
    http.setDestination(createProduceDestination(c));
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
    http.setDestination(createProduceDestination(c));
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
    http.setDestination(createProduceDestination(c));
    StandaloneRequestor producer = new StandaloneRequestor(http);
    try {
      start(c);
      ServiceCase.execute(producer, msg);
      waitForMessages(mock, 1);
    } finally {
      stopAndRelease(c);
    }
  }

  public void testMethod() throws Exception {
    for (HttpMethod m : HttpMethod.values()) {
      assertNotNull(m.create("http://localhost:8080/index.html"));
    }
  }

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

  public void testProduce_WithInterceptors() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    ApacheHttpProducer http = new ApacheHttpProducer();
    // empty RequestInterceptorClientBuilder just to get an empty list for coverage
    CompositeClientBuilder builder = new CompositeClientBuilder().withBuilders(new DefaultClientBuilder(),
        new RequestInterceptorClientBuilder().withInterceptors(new RemoveHeaders("Accept-Encoding", "User-Agent", "Connection"),
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

  public void testProduce_With_HMAC() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    ApacheHttpProducer http = new ApacheHttpProducer();
    BasicHMACSignature hmac = new BasicHMACSignature().withIdentity("MyIdentity").withHeaders("Date")
        .withSecretKey("MySecretKey").withTargetHeader("hmac").withEncoding(Encoding.BASE64)
        .withHmacAlgorithm(Algorithm.HMAC_SHA256);
    CompositeClientBuilder builder = new CompositeClientBuilder().withBuilders(new DefaultClientBuilder(),
        new RequestInterceptorClientBuilder().withInterceptors(new DateHeader(), hmac));
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

    ApacheHttpProducer http = new ApacheHttpProducer(createProduceDestination(c));
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


  public void testProduce_ReplyHasNoData() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    HttpConnection jc = createConnection();
    JettyMessageConsumer mc = createConsumer(URL_TO_POST_TO);

    ServiceList sl = new ServiceList();
    StandardResponseProducer responder = new StandardResponseProducer(HttpStatus.OK_200);
    responder.setSendPayload(false);
    sl.add(new StandaloneProducer(responder));
    Channel c = createChannel(jc, createWorkflow(mc, mock, sl));

    ApacheHttpProducer http = new ApacheHttpProducer(createProduceDestination(c));
    http.setAllowRedirect(true);
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

  public void testRequest_GetMethod_NonZeroBytes() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    ApacheHttpProducer http = new ApacheHttpProducer();
    http.setMethodProvider(new ConfiguredRequestMethodProvider(RequestMethod.GET));
    StandaloneRequestor producer = new StandaloneRequestor(http);
    doRequest(mock, http, new DefaultMessageFactory().newMessage(TEXT));
    doAssertions(mock, false);
    AdaptrisMessage m2 = mock.getMessages().get(0);
    assertEquals("GET", m2.getMetadataValue(CoreConstants.HTTP_METHOD));
    assertEquals(0, m2.getSize());
  }

  public void testRequest_ProduceException_401() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    HttpConnection jc = createConnection();
    JettyMessageConsumer mc = createConsumer(URL_TO_POST_TO);
    ServiceList services = new ServiceList();
    services.add(new StandaloneProducer(new StandardResponseProducer(HttpStatus.UNAUTHORIZED_401)));
    Channel c = createChannel(jc, createWorkflow(mc, mock, services));
    StandaloneRequestor producer = new StandaloneRequestor(new ApacheHttpProducer(createProduceDestination(c)));
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

  public void testRequest_WithErrorResponse() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    HttpConnection jc = createConnection();
    JettyMessageConsumer mc = createConsumer(URL_TO_POST_TO);
    ServiceList services = new ServiceList();

    services.add(new StandaloneProducer(new StandardResponseProducer(HttpStatus.UNAUTHORIZED_401)));
    Channel c = createChannel(jc, createWorkflow(mc, mock, services));
    ApacheHttpProducer http = new ApacheHttpProducer(createProduceDestination(c));
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


  @SuppressWarnings("deprecation")
  public void testProduce_WithUsernamePassword() throws Exception {
    String threadName = Thread.currentThread().getName();
    MockMessageProducer mock = new MockMessageProducer();
    ApacheHttpProducer http = new ApacheHttpProducer();
    try {
      AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage(TEXT);
      http.setUserName("user");
      http.setPassword("password");
      doAuthenticatedProduce(mock, http, msg);
      doAssertions(mock, true);
    } finally {
      Thread.currentThread().setName(threadName);
      assertEquals(0, AdapterResourceAuthenticator.getInstance().currentAuthenticators().size());
    }
  }

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

  public void testProduce_WithMetadataAuthHeader() throws Exception {
    String threadName = Thread.currentThread().getName();
    Thread.currentThread().setName(getName());
    MockMessageProducer mock = new MockMessageProducer();
    ApacheHttpProducer http = new ApacheHttpProducer();
    try {
      AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage(TEXT);
      MetadataAuthorizationHeader acl = new MetadataAuthorizationHeader("apacheAuth");
      msg.addMetadata("apacheAuth", buildAuthHeader("user", "password"));
      http.setAuthenticator(acl);
      doAuthenticatedProduce(mock, http, msg);
      doAssertions(mock, true);
    }
    finally {
      Thread.currentThread().setName(threadName);
      assertEquals(0, AdapterResourceAuthenticator.getInstance().currentAuthenticators().size());
    }
  }

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

    ApacheHttpProducer http = new ApacheHttpProducer(createProduceDestination(c));
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
  public void test_ReplyMetadataReplacesOriginal() throws Exception {
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

    ApacheHttpProducer http = new ApacheHttpProducer(createProduceDestination(c));
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


  public void testRequest_ExpectHeader() throws Exception {
    String threadName = Thread.currentThread().getName();
    Thread.currentThread().setName(getName());

    MockMessageProducer mock = new MockMessageProducer();
    HttpConnection jc = createConnection();
    JettyMessageConsumer mc = createConsumer(URL_TO_POST_TO);
    mc.setSendProcessingInterval(new TimeInterval(100L, TimeUnit.MILLISECONDS));
    ServiceList services = new ServiceList();
    services.add(new PayloadFromMetadataService(TEXT));
    services.add(new WaitService(new TimeInterval(2L, TimeUnit.SECONDS)));
    services.add(new StandaloneProducer(new StandardResponseProducer(HttpStatus.OK_200)));

    Channel c = LifecycleHelper.initAndStart(createChannel(jc, createWorkflow(mc, mock, services)));

    ApacheHttpProducer http = new ApacheHttpProducer(createProduceDestination(c));
    http.setMethodProvider(new ConfiguredRequestMethodProvider(RequestMethod.GET));
    http.setRequestHeaderProvider(
        new ConfiguredRequestHeaders().withHeaders(new KeyValuePair(HttpConstants.EXPECT, "102-Processing")));
    http.setConnectTimeout(new TimeInterval(60L, TimeUnit.SECONDS));
    http.setReadTimeout(new TimeInterval(60L, TimeUnit.SECONDS));
    http.setHttpProxy(":");
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

  @Override
  protected Object retrieveObjectForSampleConfig() {
    ApacheHttpProducer producer = new ApacheHttpProducer(new ConfiguredProduceDestination("http://myhost.com/url/to/post/to"));
    producer.setAuthenticator(new ConfiguredUsernamePassword("user", "password"));
    producer.setClientConfig(new CompositeClientBuilder().withBuilders(new DefaultClientBuilder().withProxy("http://my.proxy:3128"),
        new CustomTlsBuilder().withHostnameVerification(HostnameVerification.NONE), new NoConnectionManagement(),
        new RequestInterceptorClientBuilder().withInterceptors(new RemoveHeaders("User-Agent"))));
    StandaloneProducer result = new StandaloneProducer(producer);
    return result;
  }

  protected AdaptrisMessage doAssertions(MockMessageProducer mockProducer, boolean assertPayload) {
    assertEquals(1, mockProducer.getMessages().size());
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

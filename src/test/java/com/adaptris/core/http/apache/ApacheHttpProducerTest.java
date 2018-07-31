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
import java.util.Map;
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
import com.adaptris.core.PortManager;
import com.adaptris.core.ProducerCase;
import com.adaptris.core.ServiceException;
import com.adaptris.core.ServiceList;
import com.adaptris.core.StandaloneProducer;
import com.adaptris.core.StandaloneRequestor;
import com.adaptris.core.http.ConfiguredContentTypeProvider;
import com.adaptris.core.http.HttpConstants;
import com.adaptris.core.http.RawContentTypeProvider;
import com.adaptris.core.http.apache.CustomTlsBuilder.HostnameVerification;
import com.adaptris.core.http.auth.AdapterResourceAuthenticator;
import com.adaptris.core.http.auth.ConfiguredUsernamePassword;
import com.adaptris.core.http.auth.MetadataUsernamePassword;
import com.adaptris.core.http.client.ConfiguredRequestMethodProvider;
import com.adaptris.core.http.client.RequestMethodProvider.RequestMethod;
import com.adaptris.core.http.jetty.ConfigurableSecurityHandler;
import com.adaptris.core.http.jetty.HashLoginServiceFactory;
import com.adaptris.core.http.jetty.HttpConnection;
import com.adaptris.core.http.jetty.JettyMessageConsumer;
import com.adaptris.core.http.jetty.SecurityConstraint;
import com.adaptris.core.http.jetty.StandardResponseProducer;
import com.adaptris.core.http.server.HttpStatusProvider.HttpStatus;
import com.adaptris.core.metadata.NoOpMetadataFilter;
import com.adaptris.core.metadata.RemoveAllMetadataFilter;
import com.adaptris.core.services.WaitService;
import com.adaptris.core.services.metadata.AddMetadataService;
import com.adaptris.core.services.metadata.PayloadFromMetadataService;
import com.adaptris.core.stubs.MockMessageProducer;
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

  public void testProduce() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    Channel c = createAndStartChannel(mock);
    ApacheHttpProducer http = new ApacheHttpProducer(createProduceDestination(c));
    StandaloneProducer producer = new StandaloneProducer(http);
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    msg.addMetadata(METADATA_KEY_CONTENT_TYPE, "text/complicated");
    try {
      start(producer);
      producer.doService(msg);
      waitForMessages(mock, 1);
    } finally {
      stopAndRelease(c);
      stop(producer);
    }
    doAssertions(mock, true);
    AdaptrisMessage m2 = mock.getMessages().get(0);
    assertFalse(m2.headersContainsKey(METADATA_KEY_CONTENT_TYPE));
    assertEquals("text/plain", m2.getMetadataValue("Content-Type"));
  }

  public void testProduce_WithMetadata() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    Channel c = createAndStartChannel(mock);
    ApacheHttpProducer http = new ApacheHttpProducer(createProduceDestination(c));
    http.setRequestHeaderProvider(new MetadataRequestHeaders(new NoOpMetadataFilter()));
    StandaloneProducer producer = new StandaloneProducer(http);
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    msg.addMetadata(METADATA_KEY_CONTENT_TYPE, "text/complicated");
    try {
      c.requestStart();
      start(producer);
      producer.doService(msg);
      waitForMessages(mock, 1);
    } finally {
      stopAndRelease(c);
      stop(producer);
    }
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
      start(producer);
      producer.doService(msg);
      waitForMessages(mock, 1);
    } finally {
      stopAndRelease(c);
      stop(producer);
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
      start(producer);
      producer.doService(msg);
      waitForMessages(mock, 1);
    } finally {
      stopAndRelease(c);
      stop(producer);
    }
    doAssertions(mock, true);
    assertEquals(0, msg.getSize());
  }

  @SuppressWarnings("deprecation")
  public void testProduce_WithContentTypeMetadata_Legacy() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    Channel c = createAndStartChannel(mock);
    ApacheHttpProducer http = new ApacheHttpProducer(createProduceDestination(c));
    http.setContentTypeProvider(new MetadataContentTypeProvider(METADATA_KEY_CONTENT_TYPE));
    StandaloneProducer producer = new StandaloneProducer(http);
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    msg.addMetadata(METADATA_KEY_CONTENT_TYPE, "text/complicated");
    try {
      start(producer);
      producer.doService(msg);
      waitForMessages(mock, 1);
    }
    finally {
      stopAndRelease(c);
      stop(producer);
    }
    doAssertions(mock, true);
    AdaptrisMessage m2 = mock.getMessages().get(0);
    assertTrue(m2.headersContainsKey("Content-Type"));
    assertFalse(m2.headersContainsKey(METADATA_KEY_CONTENT_TYPE));
    assertEquals("text/complicated", m2.getMetadataValue("Content-Type"));
  }

  public void testProduce_WithContentTypeMetadata() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    Channel c = createAndStartChannel(mock);
    ApacheHttpProducer http = new ApacheHttpProducer(createProduceDestination(c));
    http.setContentTypeProvider(new RawContentTypeProvider("%message{" + METADATA_KEY_CONTENT_TYPE + "}"));
    StandaloneProducer producer = new StandaloneProducer(http);
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    msg.addMetadata(METADATA_KEY_CONTENT_TYPE, "text/complicated");
    try {
      start(producer);
      producer.doService(msg);
      waitForMessages(mock, 1);
    } finally {
      stopAndRelease(c);
      stop(producer);
    }
    doAssertions(mock, true);
    AdaptrisMessage m2 = mock.getMessages().get(0);
    assertTrue(m2.headersContainsKey("Content-Type"));
    assertFalse(m2.headersContainsKey(METADATA_KEY_CONTENT_TYPE));
    assertEquals("text/complicated", m2.getMetadataValue("Content-Type"));
  }

  public void testRequest_GetMethod_ZeroBytes() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    Channel c = createAndStartChannel(mock);
    ApacheHttpProducer http = new ApacheHttpProducer(createProduceDestination(c));
    http.setMethodProvider(new ConfiguredRequestMethodProvider(RequestMethod.GET));
    StandaloneRequestor producer = new StandaloneRequestor(http);
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage();
    try {
      start(c);
      start(producer);
      producer.doService(msg);
      waitForMessages(mock, 1);
    }
    finally {
      stopAndRelease(c);
      stop(producer);
    }
    doAssertions(mock, false);
    AdaptrisMessage m2 = mock.getMessages().get(0);
    assertEquals("GET", m2.getMetadataValue(CoreConstants.HTTP_METHOD));
    assertEquals(0, m2.getSize());
  }

  public void testRequest_GetMethod_NonZeroBytes() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    Channel c = createAndStartChannel(mock);
    ApacheHttpProducer http = new ApacheHttpProducer(createProduceDestination(c));
    http.setMethodProvider(new ConfiguredRequestMethodProvider(RequestMethod.GET));
    StandaloneRequestor producer = new StandaloneRequestor(http);
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    try {
      start(c);
      start(producer);
      producer.doService(msg);
      waitForMessages(mock, 1);
    }
    finally {
      stopAndRelease(c);
      stop(producer);
    }
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
      start(producer);
      producer.doService(msg);
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
      start(producer);
      producer.doService(msg); // msg will now contain the response!
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
    Thread.currentThread().setName(getName());
    ConfigurableSecurityHandler securityWrapper = createSecurityWrapper();


    HttpConnection connection = createConnection(securityWrapper);
    MockMessageProducer mockProducer = new MockMessageProducer();
    JettyMessageConsumer consumer = JettyHelper.createConsumer(URL_TO_POST_TO);
    Channel c = JettyHelper.createChannel(connection, consumer, mockProducer);
    ApacheHttpProducer httpProducer = new ApacheHttpProducer(createProduceDestination(c));
    try {
      c.requestStart();
      AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage(TEXT);

      httpProducer.setUserName("user");
      httpProducer.setPassword("password");
      StandaloneProducer producer = new StandaloneProducer(httpProducer);
      start(producer);
      producer.doService(msg);
      doAssertions(mockProducer, true);
    } finally {
      stop(httpProducer);
      stopAndRelease(c);
      Thread.currentThread().setName(threadName);
      assertEquals(0, AdapterResourceAuthenticator.getInstance().currentAuthenticators().size());
    }
  }

  public void testProduce_WithDynamicUsernamePassword() throws Exception {
    String threadName = Thread.currentThread().getName();
    Thread.currentThread().setName(getName());
    ConfigurableSecurityHandler securityWrapper = createSecurityWrapper();


    HttpConnection connection = createConnection(securityWrapper);
    MockMessageProducer mockProducer = new MockMessageProducer();
    JettyMessageConsumer consumer = JettyHelper.createConsumer(URL_TO_POST_TO);
    Channel c = JettyHelper.createChannel(connection, consumer, mockProducer);
    ApacheHttpProducer httpProducer = new ApacheHttpProducer(createProduceDestination(c));
    try {
      c.requestStart();
      AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage(TEXT);

      DynamicBasicAuthorizationHeader auth = new DynamicBasicAuthorizationHeader("user", "password");
      httpProducer.setAuthenticator(auth);
      StandaloneProducer producer = new StandaloneProducer(httpProducer);
      start(producer);
      producer.doService(msg);
      doAssertions(mockProducer, true);
    } finally {
      stop(httpProducer);
      stopAndRelease(c);
      Thread.currentThread().setName(threadName);
      assertEquals(0, AdapterResourceAuthenticator.getInstance().currentAuthenticators().size());
    }
  }


  public void testProduce_WithUsernamePassword_BadCredentials() throws Exception {
    String threadName = Thread.currentThread().getName();
    Thread.currentThread().setName(getName());
    ConfigurableSecurityHandler securityWrapper = createSecurityWrapper();


    HttpConnection connection = createConnection(securityWrapper);
    MockMessageProducer mockProducer = new MockMessageProducer();
    JettyMessageConsumer consumer = JettyHelper.createConsumer(URL_TO_POST_TO);
    Channel c = JettyHelper.createChannel(connection, consumer, mockProducer);
    ApacheHttpProducer httpProducer = new ApacheHttpProducer(createProduceDestination(c));
    try {
      c.requestStart();
      AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage(TEXT);

      httpProducer.setAuthenticator(new ConfiguredUsernamePassword(getName(), getName()));
      StandaloneProducer producer = new StandaloneProducer(httpProducer);
      start(producer);
      producer.doService(msg);
      fail();
    } catch (ServiceException expected) {

    } finally {
      stop(httpProducer);
      stopAndRelease(c);
      Thread.currentThread().setName(threadName);
      PortManager.release(connection.getPort());
      assertEquals(0, AdapterResourceAuthenticator.getInstance().currentAuthenticators().size());
    }
  }

  public void testProduce_WithMetadataCredentials() throws Exception {
    String threadName = Thread.currentThread().getName();
    Thread.currentThread().setName(getName());
    ConfigurableSecurityHandler securityWrapper = createSecurityWrapper();

    HttpConnection connection = createConnection(securityWrapper);
    MockMessageProducer mockProducer = new MockMessageProducer();
    JettyMessageConsumer consumer = JettyHelper.createConsumer(URL_TO_POST_TO);
    Channel c = JettyHelper.createChannel(connection, consumer, mockProducer);
    ApacheHttpProducer httpProducer = new ApacheHttpProducer(createProduceDestination(c));
    try {
      c.requestStart();
      AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage(TEXT);
      msg.addMetadata("apacheUser", "user");
      msg.addMetadata("apachePassword", "password");
      MetadataUsernamePassword acl = new MetadataUsernamePassword();
      acl.setPasswordMetadataKey("apachePassword");
      acl.setUsernameMetadataKey("apacheUser");
      httpProducer.setAuthenticator(acl);
      StandaloneProducer producer = new StandaloneProducer(httpProducer);
      start(producer);
      producer.doService(msg);
      doAssertions(mockProducer, true);
    }
    finally {
      stop(httpProducer);
      stopAndRelease(c);
      Thread.currentThread().setName(threadName);
      assertEquals(0, AdapterResourceAuthenticator.getInstance().currentAuthenticators().size());
    }
  }

  public void testProduce_WithAuthHeader() throws Exception {
    String threadName = Thread.currentThread().getName();
    Thread.currentThread().setName(getName());
    ConfigurableSecurityHandler securityWrapper = createSecurityWrapper();

    HttpConnection connection = createConnection(securityWrapper);
    MockMessageProducer mockProducer = new MockMessageProducer();
    JettyMessageConsumer consumer = JettyHelper.createConsumer(URL_TO_POST_TO);
    Channel c = JettyHelper.createChannel(connection, consumer, mockProducer);
    ApacheHttpProducer httpProducer = new ApacheHttpProducer(createProduceDestination(c));
    try {
      c.requestStart();
      AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage(TEXT);
      ConfiguredAuthorizationHeader acl = new ConfiguredAuthorizationHeader(buildAuthHeader("user", "password"));
      httpProducer.setAuthenticator(acl);
      StandaloneProducer producer = new StandaloneProducer(httpProducer);
      start(producer);
      producer.doService(msg);
      doAssertions(mockProducer, true);
    }
    finally {
      stop(httpProducer);
      stopAndRelease(c);
      Thread.currentThread().setName(threadName);
      assertEquals(0, AdapterResourceAuthenticator.getInstance().currentAuthenticators().size());
    }
  }

  public void testProduce_WithMetadataAuthHeader() throws Exception {
    String threadName = Thread.currentThread().getName();
    Thread.currentThread().setName(getName());
    HttpConnection connection = createConnection(createSecurityWrapper());
    MockMessageProducer mockProducer = new MockMessageProducer();
    JettyMessageConsumer consumer = JettyHelper.createConsumer(URL_TO_POST_TO);
    Channel c = JettyHelper.createChannel(connection, consumer, mockProducer);
    ApacheHttpProducer httpProducer = new ApacheHttpProducer(createProduceDestination(c));
    try {
      c.requestStart();
      AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage(TEXT);
      MetadataAuthorizationHeader acl = new MetadataAuthorizationHeader("apacheAuth");
      msg.addMetadata("apacheAuth", buildAuthHeader("user", "password"));
      httpProducer.setAuthenticator(acl);
      StandaloneProducer producer = new StandaloneProducer(httpProducer);
      start(producer);
      producer.doService(msg);
      doAssertions(mockProducer, true);
    }
    finally {
      stop(httpProducer);
      stopAndRelease(c);
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
    Channel c = createChannel(jc, createWorkflow(mc, mock, sl));

    ApacheHttpProducer http = new ApacheHttpProducer(createProduceDestination(c));
    http.setResponseHandlerFactory(new MetadataResponseHandlerFactory(getName()));
    StandaloneRequestor requestor = new StandaloneRequestor(http);
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    try {
      start(c);
      start(requestor);
      requestor.doService(msg);
      waitForMessages(mock, 1);
    } finally {
      stopAndRelease(c);
      stop(requestor);
    }
    doAssertions(mock, false);

    assertTrue(msg.headersContainsKey(getName()));
    assertEquals(TEXT, msg.getMetadataValue(getName()));
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

    Channel c = createChannel(jc, createWorkflow(mc, mock, services));

    ApacheHttpProducer http = new ApacheHttpProducer(createProduceDestination(c));
    http.setMethodProvider(new ConfiguredRequestMethodProvider(RequestMethod.GET));
    http.setRequestHeaderProvider(
        new ConfiguredRequestHeaders().withHeaders(new KeyValuePair(HttpConstants.EXPECT, "102-Processing")));
    http.setConnectTimeout(new TimeInterval(60L, TimeUnit.SECONDS));
    http.setReadTimeout(new TimeInterval(60L, TimeUnit.SECONDS));
    StandaloneRequestor requestor = new StandaloneRequestor(http);
    requestor.setReplyTimeout(new TimeInterval(60L, TimeUnit.SECONDS));
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage("Hello World");

    try {
      start(c);
      start(requestor);
      requestor.doService(msg);
      assertEquals(TEXT, msg.getContent());
    }
    finally {
      stopAndRelease(c);
      stop(requestor);
      Thread.currentThread().setName(threadName);
    }
  }

  @Override
  protected Object retrieveObjectForSampleConfig() {
    ApacheHttpProducer producer = new ApacheHttpProducer(new ConfiguredProduceDestination("http://myhost.com/url/to/post/to"));
    producer.setAuthenticator(new ConfiguredUsernamePassword("user", "password"));
    producer.setClientConfig(new CompositeClientBuilder().withBuilders(new DefaultClientBuilder().withProxy("http://my.proxy:3128"),
        new CustomTlsBuilder().withHostnameVerification(HostnameVerification.NONE), new NoConnectionManagement()));
    StandaloneProducer result = new StandaloneProducer(producer);
    return result;
  }

  protected AdaptrisMessage doAssertions(MockMessageProducer mockProducer, boolean assertPayload) {
    assertEquals(1, mockProducer.getMessages().size());
    AdaptrisMessage msg = mockProducer.getMessages().get(0);
    if (assertPayload) {
      assertEquals(TEXT, msg.getContent());
    }
    assertTrue(msg.headersContainsKey(CoreConstants.JETTY_URI));
    assertEquals(URL_TO_POST_TO, msg.getMetadataValue(CoreConstants.JETTY_URI));
    assertTrue(msg.headersContainsKey(CoreConstants.JETTY_URL));
    Map objMetadata = msg.getObjectHeaders();
    assertNotNull(objMetadata.get(CoreConstants.JETTY_REQUEST_KEY));
    assertNotNull(objMetadata.get(CoreConstants.JETTY_RESPONSE_KEY));
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

  private ConfigurableSecurityHandler createSecurityWrapper() {
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

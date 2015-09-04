package com.adaptris.core.http.apache;

import static com.adaptris.core.http.apache.JettyHelper.createChannel;
import static com.adaptris.core.http.apache.JettyHelper.createConsumer;
import static com.adaptris.core.http.apache.JettyHelper.createWorkflow;

import java.util.Arrays;
import java.util.Map;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageFactory;
import com.adaptris.core.Channel;
import com.adaptris.core.ConfiguredProduceDestination;
import com.adaptris.core.CoreConstants;
import com.adaptris.core.DefaultMessageFactory;
import com.adaptris.core.PortManager;
import com.adaptris.core.ProducerCase;
import com.adaptris.core.ServiceException;
import com.adaptris.core.ServiceList;
import com.adaptris.core.StandaloneProducer;
import com.adaptris.core.StandaloneRequestor;
import com.adaptris.core.StandardWorkflow;
import com.adaptris.core.http.AdapterResourceAuthenticator;
import com.adaptris.core.http.MetadataHeaderHandler;
import com.adaptris.core.http.jetty.HashUserRealmProxy;
import com.adaptris.core.http.jetty.HttpConnection;
import com.adaptris.core.http.jetty.MessageConsumer;
import com.adaptris.core.http.jetty.ResponseProducer;
import com.adaptris.core.http.jetty.SecurityConstraint;
import com.adaptris.core.http.jetty.SecurityHandlerWrapper;
import com.adaptris.core.metadata.NoOpMetadataFilter;
import com.adaptris.core.metadata.RemoveAllMetadataFilter;
import com.adaptris.core.stubs.MockMessageProducer;
import com.adaptris.util.KeyValuePair;

public class ApacheHttpProducerTest extends ProducerCase {
  private static final String METADATA_KEY_CONTENT_TYPE = "content.type";
  private static final String URL_TO_POST_TO = "/url/to/post/to";
  private static final String TEXT = "ABCDEFG";
  private static final String JETTY_USER_REALM = "jetty.user.realm.properties";

  public ApacheHttpProducerTest(String name) {
    super(name);
  }

  @Override
  protected void setUp() throws Exception {
  }

  public void testSetHandleRedirection() throws Exception {
    ApacheHttpProducer p = new ApacheHttpProducer();
    assertFalse(p.handleRedirection());
    p.setAllowRedirect(true);
    assertNotNull(p.getAllowRedirect());
    assertEquals(Boolean.TRUE, p.getAllowRedirect());
    assertTrue(p.handleRedirection());
    p.setAllowRedirect(false);
    assertNotNull(p.getAllowRedirect());
    assertEquals(Boolean.FALSE, p.getAllowRedirect());
    assertFalse(p.handleRedirection());
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
    assertEquals(NoOpRequestHeaders.class, p.getRequestHandler().getClass());
    try {
      p.setRequestHandler(null);
      fail();
    } catch (IllegalArgumentException expected) {

    }
    assertEquals(NoOpRequestHeaders.class, p.getRequestHandler().getClass());
    p.setRequestHandler(new MetadataRequestHeaders(new RemoveAllMetadataFilter()));
    assertEquals(MetadataRequestHeaders.class, p.getRequestHandler().getClass());
  }


  public void testSetResponseHandler() throws Exception {
    ApacheHttpProducer p = new ApacheHttpProducer();
    assertEquals(DiscardResponseHeaders.class, p.getResponseHandler().getClass());
    try {
      p.setResponseHandler(null);
      fail();
    } catch (IllegalArgumentException expected) {

    }
    assertEquals(DiscardResponseHeaders.class, p.getResponseHandler().getClass());
    p.setResponseHandler(new ResponseHeadersAsMetadata());
    assertEquals(ResponseHeadersAsMetadata.class, p.getResponseHandler().getClass());
  }

  public void testProduce() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    MessageConsumer mc = createConsumer(URL_TO_POST_TO);
    mc.setHeaderHandler(new MetadataHeaderHandler());
    mc.setHeaderPrefix("");
    HttpConnection jc = createConnection();

    Channel c = createChannel(jc, createWorkflow(mc, mock, new ServiceList()));
    ApacheHttpProducer http = new ApacheHttpProducer(createProduceDestination(jc.getPort()));
    StandaloneProducer producer = new StandaloneProducer(http);
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    msg.addMetadata(METADATA_KEY_CONTENT_TYPE, "text/complicated");
    try {
      c.requestStart();
      start(producer);
      producer.doService(msg);
      waitForMessages(mock, 1);
    } finally {
      c.requestClose();
      stop(producer);
      PortManager.release(jc.getPort());
    }
    doAssertions(mock, true);
    AdaptrisMessage m2 = mock.getMessages().get(0);
    assertFalse(m2.containsKey(METADATA_KEY_CONTENT_TYPE));
    assertEquals("text/plain", m2.getMetadataValue("Content-Type"));
  }

  public void testProduce_WithMetadata() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    MessageConsumer mc = createConsumer(URL_TO_POST_TO);
    mc.setHeaderHandler(new MetadataHeaderHandler());
    mc.setHeaderPrefix("");
    HttpConnection jc = createConnection();

    Channel c = createChannel(jc, createWorkflow(mc, mock, new ServiceList()));
    ApacheHttpProducer http = new ApacheHttpProducer(createProduceDestination(jc.getPort()));
    http.setRequestHandler(new MetadataRequestHeaders(new NoOpMetadataFilter()));
    StandaloneProducer producer = new StandaloneProducer(http);
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    msg.addMetadata(METADATA_KEY_CONTENT_TYPE, "text/complicated");
    try {
      c.requestStart();
      start(producer);
      producer.doService(msg);
      waitForMessages(mock, 1);
    } finally {
      c.requestClose();
      stop(producer);
      PortManager.release(jc.getPort());
    }
    doAssertions(mock, true);
    AdaptrisMessage m2 = mock.getMessages().get(0);
    assertTrue(m2.containsKey(METADATA_KEY_CONTENT_TYPE));
    assertEquals("text/plain", m2.getMetadataValue("Content-Type"));
  }

  public void testProduceWithContentTypeMetadata() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    MessageConsumer mc = createConsumer(URL_TO_POST_TO);
    mc.setHeaderHandler(new MetadataHeaderHandler());
    mc.setHeaderPrefix("");
    HttpConnection jc = createConnection();

    Channel c = createChannel(jc, createWorkflow(mc, mock, new ServiceList()));
    ApacheHttpProducer http = new ApacheHttpProducer(createProduceDestination(jc.getPort()));

    http.setContentTypeProvider(new MetadataContentTypeProvider(METADATA_KEY_CONTENT_TYPE));
    StandaloneProducer producer = new StandaloneProducer(http);
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    msg.addMetadata(METADATA_KEY_CONTENT_TYPE, "text/complicated");
    try {
      c.requestStart();
      start(producer);
      producer.doService(msg);
      waitForMessages(mock, 1);
    }
    finally {
      c.requestClose();
      stop(producer);
      PortManager.release(jc.getPort());
    }
    doAssertions(mock, true);
    AdaptrisMessage m2 = mock.getMessages().get(0);
    assertTrue(m2.containsKey("Content-Type"));
    assertFalse(m2.containsKey(METADATA_KEY_CONTENT_TYPE));
    assertEquals("text/complicated", m2.getMetadataValue("Content-Type"));
  }

  public void testRequest_GetMethod_ZeroBytes() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    HttpConnection jc = createConnection();
    MessageConsumer mc = createConsumer(URL_TO_POST_TO);
    ServiceList sl = new ServiceList();
    sl.add(new StandaloneProducer(new ResponseProducer(200)));
    Channel c = createChannel(jc, createWorkflow(mc, mock, sl));
    StandardWorkflow workflow = (StandardWorkflow) c.getWorkflowList().get(0);
    ApacheHttpProducer http = new ApacheHttpProducer(createProduceDestination(jc.getPort()));
    http.setMethod(ApacheHttpProducer.HttpMethod.GET);
    StandaloneRequestor producer = new StandaloneRequestor(http);
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage();
    try {
      start(c);
      start(producer);
      producer.doService(msg);
      waitForMessages(mock, 1);
    }
    finally {
      stop(c);
      stop(producer);
    }
    doAssertions(mock, false);
    AdaptrisMessage m2 = mock.getMessages().get(0);
    assertEquals("GET", m2.getMetadataValue(CoreConstants.HTTP_METHOD));
  }

  public void testRequest_GetMethod_NonZeroBytes() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    HttpConnection jc = createConnection();
    MessageConsumer mc = createConsumer(URL_TO_POST_TO);
    Channel c = createChannel(jc, createWorkflow(mc, mock, new ServiceList()));
    StandardWorkflow workflow = (StandardWorkflow) c.getWorkflowList().get(0);
    workflow.getServiceCollection().add(new StandaloneProducer(new ResponseProducer(200)));
    ApacheHttpProducer http = new ApacheHttpProducer(createProduceDestination(jc.getPort()));
    http.setMethod(ApacheHttpProducer.HttpMethod.GET);
    StandaloneRequestor producer = new StandaloneRequestor(http);
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    try {
      start(c);
      start(producer);
      producer.doService(msg);
      waitForMessages(mock, 1);
    }
    finally {
      stop(c);
      stop(producer);
    }
    doAssertions(mock, false);
    AdaptrisMessage m2 = mock.getMessages().get(0);
    assertEquals("GET", m2.getMetadataValue(CoreConstants.HTTP_METHOD));
  }
  
  public void testRequest_ProduceException() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    HttpConnection jc = createConnection();
    jc.setSendServerVersion(true);
    MessageConsumer mc = createConsumer(URL_TO_POST_TO);
    ServiceList services = new ServiceList();
    services.add(new StandaloneProducer(new ResponseProducer(401)));
    Channel c = createChannel(jc, createWorkflow(mc, mock, services));
    ApacheHttpProducer http = new ApacheHttpProducer(createProduceDestination(jc.getPort()));
    http.setMethod(ApacheHttpProducer.HttpMethod.GET);
    http.setResponseHandler(new ResponseHeadersAsMetadata("HTTP_"));
    StandaloneRequestor producer = new StandaloneRequestor(http);
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    try {
      start(c);
      start(producer);
      producer.doService(msg); // msg will now contain the response!
      fail();
    } catch (ServiceException expected) {

    }
    finally {
      stop(c);
      stop(producer);
    }
  }


  public void testRequest_WithErrorResponse() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    HttpConnection jc = createConnection();
    jc.setSendServerVersion(true);
    MessageConsumer mc = createConsumer(URL_TO_POST_TO);
    ServiceList services = new ServiceList();
    services.add(new StandaloneProducer(new ResponseProducer(401)));
    Channel c = createChannel(jc, createWorkflow(mc, mock, services));
    ApacheHttpProducer http = new ApacheHttpProducer(createProduceDestination(jc.getPort()));
    http.setMethod(ApacheHttpProducer.HttpMethod.POST);
    http.setIgnoreServerResponseCode(true);
    http.setResponseHandler(new ResponseHeadersAsMetadata("HTTP_"));
    StandaloneRequestor producer = new StandaloneRequestor(http);
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    try {
      start(c);
      start(producer);
      producer.doService(msg); // msg will now contain the response!
      waitForMessages(mock, 1);
    } finally {
      stop(c);
      stop(producer);
    }
    doAssertions(mock, false);
    AdaptrisMessage m2 = mock.getMessages().get(0);
    assertEquals("POST", m2.getMetadataValue(CoreConstants.HTTP_METHOD));
    assertEquals("401", msg.getMetadataValue(CoreConstants.HTTP_PRODUCER_RESPONSE_CODE));
    assertNotNull(msg.getMetadata("HTTP_Server"));
  }


  public void testProduce_WithUsernamePassword() throws Exception {
    String threadName = Thread.currentThread().getName();
    Thread.currentThread().setName(getName());
    HashUserRealmProxy securityWrapper = new HashUserRealmProxy();
    securityWrapper.setFilename(PROPERTIES.getProperty(JETTY_USER_REALM));

    SecurityConstraint securityConstraint = new SecurityConstraint();
    securityConstraint.setMustAuthenticate(true);
    securityConstraint.setRoles("user");

    securityWrapper.setSecurityConstraints(Arrays.asList(securityConstraint));

    HttpConnection connection = createConnection(securityWrapper);
    MockMessageProducer mockProducer = new MockMessageProducer();
    MessageConsumer consumer = JettyHelper.createConsumer(URL_TO_POST_TO);
    Channel adapter = JettyHelper.createChannel(connection, consumer, mockProducer);
    ApacheHttpProducer httpProducer = new ApacheHttpProducer(createProduceDestination(connection.getPort()));
    try {
      adapter.requestStart();
      AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage(TEXT);

      httpProducer.setUserName("user");
      httpProducer.setPassword("password");
      StandaloneProducer producer = new StandaloneProducer(httpProducer);
      start(producer);
      producer.doService(msg);
      doAssertions(mockProducer, true);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      stop(httpProducer);
      adapter.requestClose();
      Thread.currentThread().setName(threadName);
      PortManager.release(connection.getPort());
      assertEquals(0, AdapterResourceAuthenticator.getInstance().currentAuthenticators().size());
    }
  }


  @Override
  protected Object retrieveObjectForSampleConfig() {
    ApacheHttpProducer producer = new ApacheHttpProducer(new ConfiguredProduceDestination("http://myhost.com/url/to/post/to"));
    producer.setUserName("username");
    producer.setPassword("password");
    StandaloneProducer result = new StandaloneProducer(producer);
    return result;
  }

  private HttpConnection createConnection() {
    return createConnection(null);
  }


  protected HttpConnection createConnection(SecurityHandlerWrapper sh) {
    HttpConnection c = new HttpConnection();
    int port = PortManager.nextUnusedPort(18080);
    c.setPort(port);
    if (sh != null) {
      c.setSecurityHandler(sh);
    }
    c.getHttpProperties().add(new KeyValuePair(HttpConnection.HttpProperty.MaxIdleTime.name(), "30000"));
    return c;
  }


  private ConfiguredProduceDestination createProduceDestination(int port) {
    ConfiguredProduceDestination d = new ConfiguredProduceDestination("http://localhost:" + port + URL_TO_POST_TO);
    return d;
  }

  protected AdaptrisMessage doAssertions(MockMessageProducer mockProducer, boolean assertPayload) {
    assertEquals(1, mockProducer.getMessages().size());
    AdaptrisMessage msg = mockProducer.getMessages().get(0);
    if (assertPayload) {
      assertEquals(TEXT, msg.getStringPayload());
    }
    assertTrue(msg.containsKey(CoreConstants.JETTY_URI));
    assertEquals(URL_TO_POST_TO, msg.getMetadataValue(CoreConstants.JETTY_URI));
    assertTrue(msg.containsKey(CoreConstants.JETTY_URL));
    Map objMetadata = msg.getObjectMetadata();
    assertNotNull(objMetadata.get(CoreConstants.JETTY_REQUEST_KEY));
    assertNotNull(objMetadata.get(CoreConstants.JETTY_RESPONSE_KEY));
    return msg;
  }

}
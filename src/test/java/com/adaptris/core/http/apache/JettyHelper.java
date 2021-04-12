package com.adaptris.core.http.apache;

import com.adaptris.core.AdaptrisConnection;
import com.adaptris.core.AdaptrisMessageProducer;
import com.adaptris.core.Channel;
import com.adaptris.core.ComponentLifecycle;
import com.adaptris.core.CoreException;
import com.adaptris.core.DefaultEventHandler;
import com.adaptris.core.EventHandler;
import com.adaptris.core.NullProcessingExceptionHandler;
import com.adaptris.core.Service;
import com.adaptris.core.ServiceList;
import com.adaptris.core.StandaloneProducer;
import com.adaptris.core.StandardWorkflow;
import com.adaptris.core.Workflow;
import com.adaptris.core.http.jetty.HttpConnection;
import com.adaptris.core.http.jetty.JettyMessageConsumer;
import com.adaptris.core.http.jetty.MetadataHeaderHandler;
import com.adaptris.core.http.jetty.StandardResponseProducer;
import com.adaptris.core.http.server.HttpStatusProvider.HttpStatus;
import com.adaptris.core.management.webserver.SecurityHandlerWrapper;
import com.adaptris.core.stubs.MockChannel;
import com.adaptris.core.stubs.MockMessageProducer;
import com.adaptris.core.util.LifecycleHelper;
import com.adaptris.interlok.junit.scaffolding.util.PortManager;
import com.adaptris.util.KeyValuePair;

/**
 * @author lchan
 *
 */
public class JettyHelper {
  public static final String METADATA_KEY_CONTENT_TYPE = "content.type";
  public static final String URL_TO_POST_TO = "/url/to/post/to";
  public static final String TEXT = "ABCDEFG";
  public static final String JETTY_USER_REALM = "jetty.user.realm.properties";

  public static Channel createAndStartChannel() throws Exception {
    return createAndStartChannel(new MockMessageProducer());
  }

  public static Channel createAndStartChannel(MockMessageProducer mock) throws Exception {
    JettyMessageConsumer mc = createConsumer(URL_TO_POST_TO);
    mc.setHeaderHandler(new MetadataHeaderHandler());
    HttpConnection jc = createConnection();
    Channel c = createChannel(jc, createWorkflow(mc, mock, new ServiceList()));
    start(c);
    return c;
  }

  public static Channel createChannel(AdaptrisConnection connection, JettyMessageConsumer consumer,
                                      AdaptrisMessageProducer producer)
      throws Exception {
    return createChannel(connection, createWorkflow(consumer, producer));
  }

  public static String createURL(Channel channel) {
    return "http://localhost:" + getPort(channel) + URL_TO_POST_TO;
  }

  public static Channel createChannel(AdaptrisConnection connection, Workflow w) throws Exception {
    Channel result = new MockChannel();
    result.setUniqueId("channel");
    result.registerEventHandler(createEventHandler());
    result.setMessageErrorHandler(new NullProcessingExceptionHandler());
    result.setConsumeConnection(connection);
    result.getWorkflowList().add(w);
    return result;
  }

  public static Workflow createWorkflow(JettyMessageConsumer consumer, AdaptrisMessageProducer producer) {
    return createWorkflow(consumer, producer, new StandardResponseProducer(HttpStatus.OK_200));
  }

  public static Workflow createWorkflow(JettyMessageConsumer consumer, AdaptrisMessageProducer producer,
                                        StandardResponseProducer responder) {
    return createWorkflow(consumer, producer, new ServiceList(new Service[]
    {
      new StandaloneProducer(responder)
    }));
  }

  public static Workflow createWorkflow(JettyMessageConsumer consumer, AdaptrisMessageProducer producer, ServiceList list) {
    StandardWorkflow wf = new StandardWorkflow();
    wf.setConsumer(consumer);
    wf.setProducer(producer);
    wf.setServiceCollection(list);
    return wf;
  }

  public static JettyMessageConsumer createConsumer(String dest) {
    JettyMessageConsumer consumer = new JettyMessageConsumer();
    consumer.setAdditionalDebug(true);
    consumer.setPath(dest);
    return consumer;
  }

  private static EventHandler createEventHandler() throws Exception {
    DefaultEventHandler sch = new DefaultEventHandler();
    sch.requestStart();
    return sch;
  }


  private static int getPort(Channel c) {
    HttpConnection conn = (HttpConnection) c.getConsumeConnection();
    if (conn == null) {
      throw new RuntimeException();
    }
    return conn.getPort();
  }

  public static void stopAndRelease(Channel c) {
    stop(c);
    PortManager.release(getPort(c));
  }


  public static void start(ComponentLifecycle c) throws CoreException {
    LifecycleHelper.init(c);
    LifecycleHelper.start(c);
  }

  public static void stop(ComponentLifecycle c) {
    LifecycleHelper.stop(c);
    LifecycleHelper.close(c);
  }


  public static HttpConnection createConnection() {
    return createConnection(null);
  }

  public static HttpConnection createConnection(SecurityHandlerWrapper sh) {
    HttpConnection c = new HttpConnection();
    int port = PortManager.nextUnusedPort(18080);
    c.setPort(port);
    if (sh != null) {
      c.setSecurityHandler(sh);
    }
    c.getHttpConfiguration().add(new KeyValuePair(HttpConnection.HttpConfigurationProperty.SendServerVersion.name(), "true"));
    return c;
  }

}

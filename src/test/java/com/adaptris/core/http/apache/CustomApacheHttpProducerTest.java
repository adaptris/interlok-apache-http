package com.adaptris.core.http.apache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.ConfiguredProduceDestination;
import com.adaptris.core.DefaultMessageFactory;
import com.adaptris.core.ProducerCase;
import com.adaptris.core.StandaloneProducer;
import com.adaptris.core.StandaloneRequestor;
import com.adaptris.core.http.apache.CustomApacheHttpProducer;
import com.adaptris.core.http.apache.CustomApacheHttpProducer.HostnameVerification;
import com.adaptris.core.http.client.ConfiguredRequestMethodProvider;
import com.adaptris.core.http.client.RequestMethodProvider.RequestMethod;
import com.adaptris.core.security.ConfiguredPrivateKeyPasswordProvider;
import com.adaptris.security.keystore.ConfiguredUrl;

public class CustomApacheHttpProducerTest extends ProducerCase {

  protected static Logger log = LoggerFactory.getLogger(CustomApacheHttpProducerTest.class);

  private static final String KEY_KEYSTORE = "jetty.keystore.material";
  private static final String KEY_PASSWORD = "jetty.keystore.password";
  private static final String KEY_KEYSTORE_TYPE = "jetty.keystore.type";
  private static final String KEY_KEYSTORE_URL = "jetty.keystore.url";
  private static final String KEY_TRUSTSTORE_URL = "jetty.trust.url";
  private static final String KEY_TRUSTSTORE_PASSWORD = "jetty.trust.password";

  public CustomApacheHttpProducerTest(String name) {
    super(name);
  }

  @Override
  protected void setUp() throws Exception {
  }

  public void testSetTrustSelfSigned() throws Exception {
    CustomApacheHttpProducer p = new CustomApacheHttpProducer();
    assertFalse(p.trustSelfSigned());
    p.setTrustSelfSigned(Boolean.TRUE);
    assertTrue(p.trustSelfSigned());
    p.setTrustSelfSigned(null);
    assertFalse(p.trustSelfSigned());
  }

  public void testSetHostnameVerification() throws Exception {
    CustomApacheHttpProducer p = new CustomApacheHttpProducer();
    assertEquals(HostnameVerification.STANDARD, p.hostnameVerification());

    p.setHostnameVerification(HostnameVerification.NONE);
    assertEquals(HostnameVerification.NONE, p.hostnameVerification());
    p.setHostnameVerification(null);
    assertEquals(HostnameVerification.STANDARD, p.hostnameVerification());
  }

  public void testGet() throws Exception {
    CustomApacheHttpProducer http = new CustomApacheHttpProducer(
        new ConfiguredProduceDestination("https://www.theguardian.com/uk"));
    StandaloneRequestor producer = new StandaloneRequestor(http);
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage();
    http.setMethodProvider(new ConfiguredRequestMethodProvider(RequestMethod.GET));
    try {
      start(producer);
      producer.doService(msg);
    }
    finally {
      stop(producer);
    }
    assertTrue(msg.getSize() > 0);
  }

  public void testGet_CustomTLS() throws Exception {

    String keystore = PROPERTIES.getProperty(KEY_KEYSTORE);
    String keystorePassword = PROPERTIES.getProperty(KEY_PASSWORD);
    String keystoreType = PROPERTIES.getProperty(KEY_KEYSTORE_TYPE);
    String keystoreURL = PROPERTIES.getProperty(KEY_KEYSTORE_URL);

    String truststoreURL = PROPERTIES.getProperty(KEY_TRUSTSTORE_URL);
    String truststorePassword = PROPERTIES.getProperty(KEY_TRUSTSTORE_PASSWORD);
    
    CustomApacheHttpProducer http = new CustomApacheHttpProducer(new ConfiguredProduceDestination("https://github.com"));
    http.setMethodProvider(new ConfiguredRequestMethodProvider(RequestMethod.GET));
    http.setHostnameVerification(HostnameVerification.NONE);
    http.setPrivateKeyPassword(new ConfiguredPrivateKeyPasswordProvider(keystorePassword));
    http.setTrustSelfSigned(true);
    http.setTruststore(new ConfiguredUrl(truststoreURL, truststorePassword));
    http.setKeystore(new ConfiguredUrl(keystoreURL, keystorePassword));

    StandaloneRequestor producer = new StandaloneRequestor(http);
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage();

    try {
      start(producer);
      producer.doService(msg);
    } finally {
      stop(producer);
    }
    assertTrue(msg.getSize() > 0);
  }


  @Override
  protected StandaloneProducer retrieveObjectForSampleConfig() {
    CustomApacheHttpProducer producer = new CustomApacheHttpProducer(
        new ConfiguredProduceDestination("http://myhost.com/url/to/post/to"));
    producer.setTruststore(new ConfiguredUrl("file:///path/to/my/keystore?keystoreType=JKS", "PW:AAAAEH9N.....AQAjM"));
    producer.setKeystore(new ConfiguredUrl("file:///path/to/my/keystore?keystoreType=JKS", "PW:AAAAEH9N.....AQAjM"));
    producer.setPrivateKeyPassword(new ConfiguredPrivateKeyPasswordProvider("PW:AAAAEH9N.....AQAjM"));
    StandaloneProducer result = new StandaloneProducer(producer);
    return result;
  }
}

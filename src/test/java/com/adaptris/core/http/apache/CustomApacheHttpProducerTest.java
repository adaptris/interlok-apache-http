package com.adaptris.core.http.apache;

import static com.adaptris.core.http.apache.CustomTlsBuilderTest.KEY_KEYSTORE;
import static com.adaptris.core.http.apache.CustomTlsBuilderTest.KEY_KEYSTORE_TYPE;
import static com.adaptris.core.http.apache.CustomTlsBuilderTest.KEY_KEYSTORE_URL;
import static com.adaptris.core.http.apache.CustomTlsBuilderTest.KEY_PASSWORD;
import static com.adaptris.core.http.apache.CustomTlsBuilderTest.KEY_TRUSTSTORE_PASSWORD;
import static com.adaptris.core.http.apache.CustomTlsBuilderTest.KEY_TRUSTSTORE_URL;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.ConfiguredProduceDestination;
import com.adaptris.core.DefaultMessageFactory;
import com.adaptris.core.ProducerCase;
import com.adaptris.core.StandaloneProducer;
import com.adaptris.core.StandaloneRequestor;
import com.adaptris.core.http.apache.CustomTlsBuilder.HostnameVerification;
import com.adaptris.core.http.client.ConfiguredRequestMethodProvider;
import com.adaptris.core.http.client.RequestMethodProvider.RequestMethod;
import com.adaptris.core.security.ConfiguredPrivateKeyPasswordProvider;
import com.adaptris.security.keystore.ConfiguredUrl;

@SuppressWarnings("deprecation")
public class CustomApacheHttpProducerTest extends ProducerCase {

  protected static Logger log = LoggerFactory.getLogger(CustomApacheHttpProducerTest.class);
  @Override
  public boolean isAnnotatedForJunit4() {
    return true;
  }
  @Test
  public void testGet() throws Exception {
    CustomApacheHttpProducer http = new CustomApacheHttpProducer(
        new ConfiguredProduceDestination("https://github.com"));
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

  @Test
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
    http.setTlsVersions(null);
    http.setCipherSuites(null);
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

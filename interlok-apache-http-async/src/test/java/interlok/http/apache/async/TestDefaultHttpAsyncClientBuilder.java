package interlok.http.apache.async;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.adaptris.util.TimeInterval;
import java.util.concurrent.TimeUnit;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.junit.Test;

public class TestDefaultHttpAsyncClientBuilder {

  @Test
  public void testBuilder()  throws Exception {
    assertNotNull( HttpAsyncClientBuilderConfig.defaultIfNull(null));
    HttpAsyncClientBuilder httpClientBuilder = HttpAsyncClientBuilder.create();
    HttpAsyncClientBuilderConfig builder = HttpAsyncClientBuilderConfig.defaultIfNull(null);
    assertEquals(httpClientBuilder, builder.configure(httpClientBuilder));
  }

  @Test
  public void testWithProxy()  throws Exception  {
    HttpAsyncClientBuilder httpClientBuilder = HttpAsyncClientBuilder.create();
    DefaultAsyncClientBuilder builder = new DefaultAsyncClientBuilder();
    builder.setHttpProxy(":");
    assertEquals(httpClientBuilder, builder.configure(httpClientBuilder));
    builder.setHttpProxy("http://localhost:3128");
    assertEquals(httpClientBuilder, builder.configure(httpClientBuilder));
  }

  @Test
  public void testWithTimeouts() throws Exception  {
    HttpAsyncClientBuilder httpClientBuilder = HttpAsyncClientBuilder.create();
    DefaultAsyncClientBuilder builder = new DefaultAsyncClientBuilder();
    builder.setConnectTimeout(new TimeInterval(100L, TimeUnit.MILLISECONDS));
    builder.setSocketTimeout(new TimeInterval(100L, TimeUnit.MILLISECONDS));
    assertEquals(httpClientBuilder, builder.configure(httpClientBuilder));
  }

}
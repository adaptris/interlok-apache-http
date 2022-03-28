package interlok.http.apache.async;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;

@FunctionalInterface
public interface HttpAsyncClientBuilderConfig {

  static HttpAsyncClientBuilderConfig defaultIfNull(HttpAsyncClientBuilderConfig configured) {
    return ObjectUtils.defaultIfNull(configured, new DefaultAsyncClientBuilder());
  }

  /**
   * Do any additional configuration.
   *
   * @param builder the existing builder
   * @return a reconfigured builder.
   */
  HttpAsyncClientBuilder configure(HttpAsyncClientBuilder builder) throws Exception;
}

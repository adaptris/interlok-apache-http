package interlok.http.apache.async;

import static org.apache.commons.lang3.StringUtils.isBlank;

import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.util.TimeInterval;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import java.util.Optional;
import javax.validation.Valid;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;

/**
 * Provides some sensible default behaviour for configuring an {@code HttpAsyncClientBuilder}.
 *
 * @config apache-http-default-async-client-builder
 */
@XStreamAlias("apache-http-default-async-client-builder")
@NoArgsConstructor
@ComponentProfile(since = "4.5.0", summary = "Provides some sensible default behaviour when configuring the HttpAsyncClient")
public class DefaultAsyncClientBuilder implements HttpAsyncClientBuilderConfig {

  /**
   * Explicitly configurd proxy server.
   * <p>Follows the form {@code scheme://host:port} or more simply {@code host:port} and if it ends up being
   * just a {@code :} then is assumed that no proxy is required (this is to make it more convenient to migrate
   * configuration through environments, some of which may require a proxy, some not.
   * </p>
   */
  @Getter
  @Setter
  private String httpProxy;
  /**
   * The connect timeout which is set on {@code RequestConfig.Builder#setConnectTimeout(int)}
   */
  @Valid
  @AdvancedConfig
  @Getter
  @Setter
  private TimeInterval connectTimeout;
  /**
   * The socket timeout is set on {@code RequestConfig.Builder#setSocketTimeout(int)}
   */
  @Valid
  @AdvancedConfig
  @Getter
  @Setter
  private TimeInterval socketTimeout;

  @Override
  public HttpAsyncClientBuilder configure(HttpAsyncClientBuilder builder) {
    if (hasConfiguredProxy()) {
      builder.setProxy(HttpHost.create(getHttpProxy()));
    }
    return customiseTimeouts(builder.useSystemProperties());
  }

  protected HttpAsyncClientBuilder customiseTimeouts(HttpAsyncClientBuilder builder) {
    if (BooleanUtils.or(new boolean[]{
        getConnectTimeout() != null, getSocketTimeout() != null
    })) {
      RequestConfig.Builder requestCfg = RequestConfig.custom();
      Optional.ofNullable(getConnectTimeout()).ifPresent(
          (timeout) -> {
            int t = Long.valueOf(timeout.toMilliseconds()).intValue();
            requestCfg.setConnectTimeout(t);
            requestCfg.setConnectionRequestTimeout(t);
          }
      );
      Optional.ofNullable(getSocketTimeout())
          .ifPresent((timeout) -> requestCfg.setSocketTimeout(Long.valueOf(timeout.toMilliseconds()).intValue()));
      builder.setDefaultRequestConfig(requestCfg.build());
    }
    return builder;
  }

  private boolean hasConfiguredProxy() {
    String s = StringUtils.trimToEmpty(getHttpProxy());
    return BooleanUtils.and(new boolean[]{!isBlank(s), !":".equals(s)});
  }
}

package interlok.http.apache.async;


import com.adaptris.annotation.ComponentProfile;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import interlok.http.apache.credentials.CredentialsProviderBuilder;
import java.util.Optional;
import javax.validation.Valid;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.SystemDefaultCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;

/**
 * Allows you to specify a {@code CredentialsProvider} as a default for the {@code HttpClientBuilder}.
 * <p>
 * Most of the time you more likely to configure an {@link com.adaptris.core.http.auth.HttpAuthenticator} instance on
 * the producer, but this is included for completeness as a way of configuring the {@link HttpClientBuilder}.
 * </p>
 *
 * @config apache-http-async-client-builder-with-credentials
 */
@XStreamAlias("apache-http-async-client-builder-with-credentials")
@ComponentProfile(since = "4.5.0", summary = "Allows you to configure a 'CredentialsProvider' when building the HttpClient")
@NoArgsConstructor
public class AsyncWithCredentials implements HttpAsyncClientBuilderConfig {

  /**
   * The credentials provider.
   * <p>If not explicitly configured, then the resulting {@code CredentialsProvider} is a {@code
   * org.apache.http.impl.client.SystemDefaultCredentialsProvider} with no configuration.
   * </p>
   */
  @Getter
  @Setter
  @Valid
  private CredentialsProviderBuilder credentialsProvider;

  @Override
  public HttpAsyncClientBuilder configure(HttpAsyncClientBuilder builder) throws Exception {
    CredentialsProvider provider = Optional.ofNullable(getCredentialsProvider()).map(CredentialsProviderBuilder::build)
        .orElse(new SystemDefaultCredentialsProvider());
    builder.setDefaultCredentialsProvider(provider);
    return builder;
  }

  public AsyncWithCredentials withProvider(CredentialsProviderBuilder builder) {
    setCredentialsProvider(builder);
    return this;
  }

}

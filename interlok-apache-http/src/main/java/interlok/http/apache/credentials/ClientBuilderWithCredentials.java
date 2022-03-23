package interlok.http.apache.credentials;


import com.adaptris.annotation.ComponentProfile;
import com.adaptris.core.http.apache.HttpClientBuilderConfigurator;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import interlok.http.apache.credentials.CredentialsProviderBuilder;
import java.util.Optional;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.SystemDefaultCredentialsProvider;

/**
 * Allows you to specify a {@code CredentialsProvider} as a default for the {@code HttpClientBuilder}.
 * <p>
 * Most of the time you more likely to configure an {@link com.adaptris.core.http.auth.HttpAuthenticator} instance on
 * the producer, but this is included for completeness as a way of configuring the {@link HttpClientBuilder}.
 * </p>
 *
 * @config credentials-provider-apache-http-client-builder
 */
@XStreamAlias("credentials-provider-apache-http-client-builder")
@ComponentProfile(since = "4.5.0", summary = "Allows you to configure a 'CredentialsProvider' when building the HttpClient")
@NoArgsConstructor
public class ClientBuilderWithCredentials implements HttpClientBuilderConfigurator {

  /**
   * The credentials provider.
   * <p>If not explicitly configured, then the resulting {@code CredentialsProvider} is a {@code
   * org.apache.http.impl.client.SystemDefaultCredentialsProvider} with no configuration.
   * </p>
   */
  @Getter
  @Setter
  private CredentialsProviderBuilder credentialsProvider;

  @Override
  public HttpClientBuilder configure(HttpClientBuilder builder) throws Exception {
    CredentialsProvider provider = Optional.ofNullable(getCredentialsProvider()).map(CredentialsProviderBuilder::build)
        .orElse(new SystemDefaultCredentialsProvider());
    builder.setDefaultCredentialsProvider(provider);
    return builder;
  }

  public ClientBuilderWithCredentials withProvider(CredentialsProviderBuilder builder) {
    setCredentialsProvider(builder);
    return this;
  }

}

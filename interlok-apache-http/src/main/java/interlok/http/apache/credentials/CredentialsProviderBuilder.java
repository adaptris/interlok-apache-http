package interlok.http.apache.credentials;

import org.apache.http.client.CredentialsProvider;

@FunctionalInterface
public interface CredentialsProviderBuilder {

  /**
   * Build the {@code org.apache.http.client.CredentialsProvider} instance.
   */
  CredentialsProvider build();
}

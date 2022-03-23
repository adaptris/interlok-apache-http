package interlok.http.apache.credentials;

import org.apache.http.auth.Credentials;

/**
 * Adds support for {@code oorg.apache.http.auth.Credentials} in a configuration friendly way.
 */
@FunctionalInterface
public interface CredentialsBuilder {

  /**
   * Build the {@code Credentials} object.
   */
  Credentials build();
}

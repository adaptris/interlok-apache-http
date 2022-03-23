package interlok.http.apache.credentials;

import org.apache.http.auth.AuthScope;

/**
 * Adds support for {@code org.apache.http.auth.AuthScope} for username and password in a configuration friendly way.
 */
@FunctionalInterface
public interface AuthScopeBuilder {

  /**
   * Build the {@code AuthScope} instance.
   */
  AuthScope build();
}

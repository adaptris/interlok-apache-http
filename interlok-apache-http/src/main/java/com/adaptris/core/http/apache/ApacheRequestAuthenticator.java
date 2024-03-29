package com.adaptris.core.http.apache;

import org.apache.http.client.methods.HttpRequestBase;
import com.adaptris.core.http.auth.HttpAuthenticator;

/**
 * ApacheRequestAuthenticator is an interface designed to facilitate HTTP Authentication in various ways.
 * <p>
 * Some implementations of this interface will need to temporarily mutate global state and therefore must be closed in a finally
 * statement or try-with-resources block.
 * </p>
 *
 * @author gdries
 */
public interface ApacheRequestAuthenticator extends HttpAuthenticator {

  /**
   * Perform whatever actions are required to the HttpRequestBase.
   */
  void configure(HttpRequestBase req) throws Exception;

}

package interlok.http.apache.credentials;

import com.adaptris.annotation.ComponentProfile;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.NoArgsConstructor;
import org.apache.http.auth.AuthScope;

/**
 * Returns {@code org.apache.http.auth.AuthScope#ANY} when requested to build a scope.
 *
 * @config apache-any-authentication-scope
 */
@XStreamAlias("apache-any-authentication-scope")
@ComponentProfile(summary="Any authentication scope")
@NoArgsConstructor
public class AnyScope implements AuthScopeBuilder {

  @Override
  public AuthScope build() {
    return AuthScope.ANY;
  }
}

package interlok.http.apache.credentials;

import com.adaptris.annotation.ComponentProfile;
import com.adaptris.util.NumberUtils;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.http.auth.AuthScope;

/** An explicitly configured {@link AuthScope} builder.
 *
 * @config apache-http-configured-authentication-scope
 */
@XStreamAlias("apache-http-configured-authentication-scope")
@ComponentProfile(summary="Explicitly configured authentication scope")
@NoArgsConstructor
public class ConfiguredScope implements AuthScopeBuilder {

  /** The host associated with the scope
   *  <p>Defaults to {@code AuthScope.ANY_HOST} if not explicitly configured</p>
   */
  @Getter
  @Setter
  private String host = AuthScope.ANY_HOST;
  /** The port associated with the scope
   *  <p>Defaults to {@code AuthScope.ANY_PORT} if not explicitly configured</p>
   */
  @Getter
  @Setter
  private Integer port;
  /** The realm associated with the scope
   *  <p>Defaults to {@code AuthScope.ANY_REALM} if not explicitly configured</p>
   *
   */
  @Getter
  @Setter
  private String realm = AuthScope.ANY_REALM;
  /** The scheme associated with the scope
   *  <p>Defaults to {@code AuthScope.ANY_SCHEME} if not explicitly configured</p>
   */
  @Getter
  @Setter
  private String scheme = AuthScope.ANY_SCHEME;

  @Override
  public AuthScope build() {
    return new AuthScope(getHost(), port(), getRealm(), getScheme());
  }

  private int port() {
    return NumberUtils.toIntDefaultIfNull(getPort(), AuthScope.ANY_PORT);
  }

}

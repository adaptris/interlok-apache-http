package interlok.http.apache.credentials;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;

/**
 * Wraps a {@code AuthScope} and {@code Credentials} for insertion into a {@code CredentialsProvider}
 *
 */
@NoArgsConstructor
@XStreamAlias("apache-http-credentials-wrapper")
public class CredentialsWrapper {

  /** The authentication scope associated with these credentials.
   *
   */
  @Getter
  @Setter
  private AuthScopeBuilder authenticationScope;

  /** The credentials.
   *
   */
  @Getter
  @Setter
  @NotNull
  @NonNull
  private CredentialsBuilder credentials;

  protected AuthScope authenticationScope() {
    return ObjectUtils.defaultIfNull(getAuthenticationScope(), new AnyScope()).build();
  }

  protected Credentials credentials() {
    return getCredentials().build();
  }

  public CredentialsWrapper withScope(AuthScopeBuilder scope) {
    setAuthenticationScope(scope);
    return this;
  }

  public CredentialsWrapper withCredentials(CredentialsBuilder builder) {
    setCredentials(builder);
    return this;
  }

}

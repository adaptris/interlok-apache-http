package interlok.http.apache.credentials;

import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.annotation.InputFieldHint;
import com.adaptris.interlok.resolver.ExternalResolver;
import com.adaptris.security.exc.PasswordException;
import com.adaptris.security.password.Password;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;

/**
 * Adds support for {@code org.apache.http.client.CredentialsProvider} for username and password in a configuration
 * friendly way.
 *
 * @config apache-username-password-credentials
 */
@XStreamAlias("apache-username-password-credentials")
@DisplayOrder(order = {"username", "password"})
@ComponentProfile(summary = "Providers username+password credentials")
@NoArgsConstructor
public class UsernamePassword implements CredentialsBuilder {

  /**
   * The username.
   */
  @Getter
  @Setter
  private String username;
  /**
   * The password.
   * <p>The password may be encoded and/or resolved from system properties / environment variables.</p>
   */
  @InputFieldHint(style = "PASSWORD", external = true)
  @Getter
  @Setter
  private String password;

  @Override
  @SneakyThrows(PasswordException.class)
  public Credentials build() {
    return new UsernamePasswordCredentials(getUsername(), Password.decode(ExternalResolver.resolve(getPassword())));
  }

  public UsernamePassword withCredentials(String user, String password) {
    setUsername(user);
    setPassword(password);
    return this;
  }

}

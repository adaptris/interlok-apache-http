package interlok.http.apache.credentials;

import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.annotation.InputFieldHint;
import com.adaptris.interlok.resolver.ExternalResolver;
import com.adaptris.security.exc.PasswordException;
import com.adaptris.security.password.Password;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;

/**
 * Adds support for {@code org.apache.http.client.CredentialsProvider} for username and password in a configuration
 * friendly way.
 * <p>This builds a {@code UsernamePasswordCredentials} under the covers, interestingly it allows you to have a non null
 * blank username; there should be no reason why you would want to do that, so validation on the username field is set
 * to be {@code NotBlank}.
 * </p>
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
  @NotBlank(message="No Username associated with UsernamePassword credentials")
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

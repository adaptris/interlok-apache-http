package com.adaptris.core.http.apache;

import com.adaptris.annotation.InputFieldHint;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.CoreException;
import com.adaptris.core.http.HttpConstants;
import com.adaptris.core.http.auth.ResourceTargetMatcher;
import com.adaptris.core.http.auth.UserPassAuthentication;
import com.adaptris.core.util.Args;
import com.adaptris.core.util.ExceptionHelper;
import com.adaptris.interlok.resolver.ExternalResolver;
import com.adaptris.security.exc.PasswordException;
import com.adaptris.security.password.Password;
import com.adaptris.util.text.Base64ByteTranslator;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import java.net.PasswordAuthentication;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.http.client.methods.HttpRequestBase;

/**
 * Build a {@link HttpConstants#AUTHORIZATION} (Basic only) from configuration (or metadata).
 * <p>
 * In some situations it may be preferred to build the {@link HttpConstants#AUTHORIZATION} header rather than relying on the
 * behaviour of {@link URLConnection} to request a {@link PasswordAuthentication} object when accessing protected resources (this is
 * what happens if you use {@link UserPassAuthentication}). You can use this class to create a Basic authorization.
 * </p>
 *
 * @config apache-http-dynamic-authorization-header
 *
 */
@XStreamAlias("apache-http-dynamic-authorization-header")
@NoArgsConstructor
public class DynamicBasicAuthorizationHeader implements ApacheRequestAuthenticator {

  /** The username.
   *
   */
  @Getter
  @Setter
  @NotBlank(message="username may not be blank for authorization")
  @InputFieldHint(expression = true)
  private String username;
  /** The password.
   *
   */
  @Getter
  @Setter
  @NotBlank(message="password may not be blank for authorization")
  @InputFieldHint(expression = true, style = "PASSWORD", external = true)
  private String password;

  private transient String authHeader;

  public DynamicBasicAuthorizationHeader(String username, String password) {
    this();
    setUsername(username);
    setPassword(password);
  }

  @Override
  public void setup(String target, AdaptrisMessage msg, ResourceTargetMatcher auth) throws CoreException {
    try {
      String username = Args.notBlank(msg.resolve(getUsername()), "username");
      String password = Args.notBlank(msg.resolve(ExternalResolver.resolve(getPassword())), "password");
      String encoded = new Base64ByteTranslator().translate(
          String.format("%s:%s", username, Password.decode(password)).getBytes(
              StandardCharsets.UTF_8));
      authHeader = String.format("Basic %s", encoded);
    } catch (IllegalArgumentException | PasswordException e) {
      throw ExceptionHelper.wrapCoreException(e);
    }
  }

  @Override
  public void configure(HttpRequestBase req) {
    req.addHeader(HttpConstants.AUTHORIZATION, authHeader);
  }

  @Override
  public void close() {
  }

}

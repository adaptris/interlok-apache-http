package com.adaptris.core.http.apache;

import java.io.UnsupportedEncodingException;
import java.net.PasswordAuthentication;
import java.net.URLConnection;
import javax.validation.constraints.NotBlank;
import org.apache.http.client.methods.HttpRequestBase;
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
public class DynamicBasicAuthorizationHeader implements ApacheRequestAuthenticator {

  @NotBlank
  @InputFieldHint(expression = true)
  private String username;
  @NotBlank
  @InputFieldHint(expression = true, style = "PASSWORD", external = true)
  private String password;

  private transient String authHeader;

  public DynamicBasicAuthorizationHeader() {

  }

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
      String encoded = new Base64ByteTranslator().translate(String.format("%s:%s", username, Password.decode(password)).getBytes("UTF-8"));
      authHeader = String.format("Basic %s", encoded);
    } catch (UnsupportedEncodingException | IllegalArgumentException | PasswordException e) {
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

  public String getUsername() {
    return username;
  }

  /**
   * Set the username
   */
  public void setUsername(String s) {
    username = Args.notBlank(s, "username");
  }

  /**
   * @return the password
   */
  public String getPassword() {
    return password;
  }

  /**
   * @param pw the password to set
   */
  public void setPassword(String pw) {
    password = Args.notBlank(pw, "password");
  }

}

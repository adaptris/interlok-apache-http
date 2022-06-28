package com.adaptris.core.http.apache;

import com.adaptris.annotation.InputFieldHint;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.CoreException;
import com.adaptris.core.http.HttpConstants;
import com.adaptris.core.http.auth.ResourceTargetMatcher;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.http.client.methods.HttpRequestBase;

/**
 * Build an {@link HttpConstants#AUTHORIZATION} header from static data.
 *
 * @config apache-http-configured-authorization-header
 */
@XStreamAlias("apache-http-configured-authorization-header")
@NoArgsConstructor
public class ConfiguredAuthorizationHeader implements ApacheRequestAuthenticator {

  @Getter
  @Setter
  @NotBlank(message="Authorization Header Value should not be blank")
  @InputFieldHint(expression = true)
  private String headerValue;

  private transient String actualHeaderValue;

  public ConfiguredAuthorizationHeader(String value) {
    this();
    setHeaderValue(value);
  }

  @Override
  public void setup(String target, AdaptrisMessage msg, ResourceTargetMatcher m) throws CoreException {
    actualHeaderValue = msg.resolve(getHeaderValue());
  }

  @Override
  public void close() {
  }

  @Override
  public void configure(HttpRequestBase req) {
    req.addHeader(HttpConstants.AUTHORIZATION, actualHeaderValue);
  }

}

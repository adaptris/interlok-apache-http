package com.adaptris.core.http.apache;

import com.adaptris.annotation.InputFieldHint;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.CoreException;
import com.adaptris.core.http.HttpConstants;
import com.adaptris.core.http.auth.ResourceTargetMatcher;
import com.adaptris.core.util.Args;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

import javax.validation.constraints.NotBlank;

/**
 * Build an {@link HttpConstants#AUTHORIZATION} header from static data.
 *
 * @config apache-http-configured-authorization-header
 */
@XStreamAlias("apache-http5-configured-authorization-header")
public class ConfiguredAuthorizationHeader implements ApacheRequestAuthenticator {

  @NotBlank
  @InputFieldHint(expression = true)
  private String headerValue;

  private transient String actualHeaderValue;

  public ConfiguredAuthorizationHeader() {

  }

  public ConfiguredAuthorizationHeader(String value) {
    this();
    setHeaderValue(value);
  }

  public String getHeaderValue() {
    return headerValue;
  }

  /**
   * The value for the authorization header
   * @param headerValue
   */
  public void setHeaderValue(String headerValue) {
    this.headerValue = Args.notBlank(headerValue, "headerValue");
  }

  @Override
  public void setup(String target, AdaptrisMessage msg, ResourceTargetMatcher m) throws CoreException {
    actualHeaderValue = msg.resolve(getHeaderValue());
  }

  @Override
  public void close() {
  }

  @Override
  public void configure(HttpUriRequestBase req) {
    req.addHeader(HttpConstants.AUTHORIZATION, actualHeaderValue);
  }

}

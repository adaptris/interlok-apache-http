package com.adaptris.core.http.apache5;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.URL;

import org.junit.jupiter.api.Test;

import com.adaptris.core.http.ResourceAuthenticator.ResourceTarget;

public class ApacheResourceTargetMatcherTest extends ApacheHttpProducer {

  @Test
  public void testMatch_HTTP_URL() throws Exception {
    String httpUrlExplicit = "http://localhost:80/path/to/index.html";
    String httpUrlImplicit = "http://localhost/path/to/index.html";

    ResourceTarget httpTarget = new ResourceTarget().withRequestingURL(new URL(httpUrlExplicit));
    assertTrue(new ApacheResourceTargetMatcher(new URI(httpUrlExplicit)).matches(httpTarget));
    assertTrue(new ApacheResourceTargetMatcher(new URI(httpUrlImplicit)).matches(httpTarget));

    assertFalse(new ApacheResourceTargetMatcher(new URI(httpUrlImplicit))
        .matches(new ResourceTarget().withRequestingURL(new URL("http://localhost/another/path"))));
  }

  @Test
  public void testMatch_HTTPS_URL() throws Exception {
    String httpsUrlExplicit = "https://localhost:443/path/to/index.html";
    String httpsUrlImplicit = "https://localhost/path/to/index.html";
    ResourceTarget httpsTarget = new ResourceTarget().withRequestingURL(new URL(httpsUrlExplicit));
    assertTrue(new ApacheResourceTargetMatcher(new URI(httpsUrlExplicit)).matches(httpsTarget));
    assertTrue(new ApacheResourceTargetMatcher(new URI(httpsUrlImplicit)).matches(httpsTarget));
    assertFalse(new ApacheResourceTargetMatcher(new URI(httpsUrlImplicit))
        .matches(new ResourceTarget().withRequestingURL(new URL("https://localhost/another/path"))));
  }

  @Test
  public void testMatch_HostPortScheme() throws Exception {
    String url = "http://localhost/path/to/index.html";
    ApacheResourceTargetMatcher matcher = new ApacheResourceTargetMatcher(new URI(url));
    assertTrue(matcher.matches(new ResourceTarget().withRequestingHost("localhost").withRequestingPort(80).withRequestingScheme("http")));
    assertFalse(matcher
        .matches(new ResourceTarget().withRequestingHost("localhost").withRequestingPort(8080).withRequestingScheme("http")));
    assertFalse(matcher
        .matches(new ResourceTarget().withRequestingHost("microsoft.com").withRequestingPort(80).withRequestingScheme("http")));
  }

}

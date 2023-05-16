package interlok.http.apache.credentials;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.Test;

import com.adaptris.security.exc.PasswordException;

public class TestCredentialsBuilder {

  @Test
  public void testProviderBuilder() {
    DefaultCredentialsProviderBuilder builder = new DefaultCredentialsProviderBuilder().withScopedCredentials(
        new ScopedCredential().withScope(new AnyScope()).withCredentials(new UsernamePassword().withCredentials("myUser", "myPassword")));
    assertNotNull(builder.build());
    assertTrue(CredentialsProvider.class.isAssignableFrom(builder.build().getClass()));
  }

  @Test
  public void testCredentialsProvider() {
    DefaultCredentialsProviderBuilder builder = new DefaultCredentialsProviderBuilder().withScopedCredentials(
        new ScopedCredential().withScope(new AnyScope()).withCredentials(new UsernamePassword().withCredentials("myUser", "myPassword")));
    CredentialsProvider provider = builder.build();
    Credentials creds = provider.getCredentials(AuthScope.ANY);
    assertEquals(UsernamePasswordCredentials.class, creds.getClass());
    assertEquals("myUser", creds.getUserPrincipal().getName());
    assertEquals("myPassword", creds.getPassword());
  }

  @Test
  public void testCredentialsProvider_BadPassword() throws Exception {
    DefaultCredentialsProviderBuilder builder = new DefaultCredentialsProviderBuilder().withScopedCredentials(new ScopedCredential()
        .withScope(new AnyScope()).withCredentials(new UsernamePassword().withCredentials("myUser", "AES_GCM:myPassword")));

    assertThrows(PasswordException.class, () -> builder.build());
  }

  @Test
  public void testConfiguredScope() {
    ConfiguredScope builder = new ConfiguredScope();
    builder.setHost("localhost");
    builder.setPort(443);
    builder.setRealm("realm");
    builder.setScheme("https");
    AuthScope scope = builder.build();
    assertEquals("localhost", scope.getHost());
    assertEquals(443, scope.getPort());
    assertEquals("realm", scope.getRealm());
    assertEquals("https".toUpperCase(Locale.ROOT), scope.getScheme().toUpperCase(Locale.ROOT));
  }

  @Test
  public void testClientBuilderConfigure() throws Exception {
    ClientBuilderWithCredentials builder = new ClientBuilderWithCredentials();
    assertNotNull(builder.configure(HttpClients.custom()));
    DefaultCredentialsProviderBuilder credsProvider = new DefaultCredentialsProviderBuilder().withScopedCredentials(
        new ScopedCredential().withScope(new AnyScope()).withCredentials(new UsernamePassword().withCredentials("myUser", "myPassword")));
    assertNotNull(builder.withProvider(credsProvider).configure(HttpClients.custom()));
  }

}
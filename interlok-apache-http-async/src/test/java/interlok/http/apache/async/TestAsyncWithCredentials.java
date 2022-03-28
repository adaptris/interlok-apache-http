package interlok.http.apache.async;

import static org.junit.Assert.assertNotNull;

import interlok.http.apache.credentials.AnyScope;
import interlok.http.apache.credentials.DefaultCredentialsProviderBuilder;
import interlok.http.apache.credentials.ScopedCredential;
import interlok.http.apache.credentials.UsernamePassword;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.junit.Test;

public class TestAsyncWithCredentials {

  @Test
  public void testAsyncWithCredentials() throws Exception {
    assertNotNull(new AsyncWithCredentials().configure(HttpAsyncClientBuilder.create()));
    DefaultCredentialsProviderBuilder credsProvider = new DefaultCredentialsProviderBuilder().withScopedCredentials(
        new ScopedCredential().withScope(new AnyScope())
            .withCredentials(new UsernamePassword().withCredentials("myUser", "myPassword"))
    );
    AsyncWithCredentials builder = new AsyncWithCredentials().withProvider(credsProvider);
    assertNotNull(builder.configure(HttpAsyncClientBuilder.create()));
  }

}
package interlok.http.apache.credentials;

import com.adaptris.annotation.ComponentProfile;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.SystemDefaultCredentialsProvider;

/**
 * Supports the use of a {@code org.apache.http.client.CredentialsProvider} where supported in a configuration friendly
 * way.
 * <p>This uses {@code org.apache.http.impl.client.SystemDefaultCredentialsProvider} with added configuration from the
 * underlying {@link ScopedCredential} via the {@code CredentialsProvider#setCredentials(AuthScope, Credentials)}
 * method.</p>
 *
 * @config apache-http-default-credentials-provider-builder
 */
@XStreamAlias("apache-http-default-credentials-provider-builder")
@ComponentProfile(since = "4.5.0", summary = "Supports use of 'org.apache.http.client.CredentialsProvider'")
@NoArgsConstructor
public class DefaultCredentialsProviderBuilder implements CredentialsProviderBuilder {

  @Getter
  @Setter
  @NonNull
  @NotNull(message = "No Credentials associated with a CredentialsProvider")
  @XStreamImplicit(itemFieldName = "scoped-credential")
  @Valid
  private List<ScopedCredential> scopedCredentials = new ArrayList<>();

  public DefaultCredentialsProviderBuilder withScopedCredentials(ScopedCredential... w) {
    setScopedCredentials(new ArrayList<>(List.of(w)));
    return this;
  }

  @Override
  public CredentialsProvider build() {
    SystemDefaultCredentialsProvider provider = new SystemDefaultCredentialsProvider();
    this.getScopedCredentials().forEach((c) -> provider.setCredentials(c.authenticationScope(), c.credentials()));
    return provider;
  }
}

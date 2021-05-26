package com.adaptris.core.http.apache;

import com.adaptris.core.AdaptrisMessage;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;

import javax.validation.constraints.NotBlank;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

/**
 * Implementation {@link ResponseHandlerFactory} that writes the response to the named metadata key.
 *
 * @config apache-http-metadata-response-handler
 *
 */
@XStreamAlias("apache-http-metadata-response-handler")
@Slf4j
@NoArgsConstructor
public class MetadataResponseHandlerFactory extends ResponseHandlerFactoryImpl {

  @NotBlank
  @Getter
  @Setter
  private String metadataKey;

  public MetadataResponseHandlerFactory(String key) {
    this();
    setMetadataKey(key);
  }

  @Override
  public HttpClientResponseHandler<AdaptrisMessage> createResponseHandler(HttpProducer owner) {
    return new HttpResponseHandler(owner);
  }

  private class HttpResponseHandler extends ResponseHandlerImpl {
    private HttpResponseHandler(HttpProducer owner) {
      super(owner);
    }

    private InputStreamReader getReader(InputStream in, String encoding) throws UnsupportedEncodingException {
      String encodingToUse = StringUtils.defaultIfEmpty(encoding, Charset.defaultCharset().name());
      return new InputStreamReader(in, encodingToUse);
    }

    @Override
    protected void processEntity(HttpEntity entity, String encoding, AdaptrisMessage msg)
        throws IOException {
      log.trace("Processing data from response {}", entity.getClass().getSimpleName());
      StringBuilder builder = new StringBuilder();
      try (Reader in = getReader(entity.getContent(), encoding);
          StringBuilderWriter out = new StringBuilderWriter(builder)) {
        IOUtils.copy(in, out);
      }
      msg.addMessageHeader(getMetadataKey(), builder.toString());
    }

  }

}

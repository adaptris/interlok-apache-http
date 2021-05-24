package com.adaptris.core.http.apache;

import com.adaptris.core.AdaptrisMessage;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Implementation {@link ResponseHandlerFactory} that writes the response to the {@link AdaptrisMessage} payload.
 *
 * @config apache-http-payload-response-handler
 *
 */
@XStreamAlias("apache-http-payload-response-handler")
@Slf4j
@NoArgsConstructor
public class PayloadResponseHandlerFactory extends ResponseHandlerFactoryImpl {


  @Override
  public HttpClientResponseHandler<AdaptrisMessage> createResponseHandler(HttpProducer owner) {
    return new HttpResponseHandler(owner);
  }

  private class HttpResponseHandler extends ResponseHandlerImpl {
    private HttpResponseHandler(HttpProducer owner) {
      super(owner);
    }

    @Override
    protected void processEntity(HttpEntity entity, String encoding, AdaptrisMessage msg)
        throws IOException {
      log.trace("Processing data from response {}", entity.getClass().getSimpleName());
      try (InputStream in = entity.getContent();
          OutputStream out = new BufferedOutputStream(msg.getOutputStream())) {
        IOUtils.copy(in, out);
        msg.setContentEncoding(encoding);
      }
      msg.addObjectHeader(OBJ_METADATA_PAYLOAD_MODIFIED, Boolean.TRUE);
    }

  }

}

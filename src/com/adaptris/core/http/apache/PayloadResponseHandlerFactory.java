package com.adaptris.core.http.apache;

import static com.adaptris.core.AdaptrisMessageFactory.defaultIfNull;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.CoreConstants;
import com.adaptris.core.MetadataElement;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Implementation {@link ResponseHandlerFactory} that writes the response to the {@link AdaptrisMessage} payload.
 * 
 * @config apache-http-payload-response-handler
 *
 */
@XStreamAlias("apache-http-payload-response-handler")
public class PayloadResponseHandlerFactory implements ResponseHandlerFactory {

  private transient Logger log = LoggerFactory.getLogger(this.getClass());

  @Override
  public ResponseHandler<AdaptrisMessage> createResponseHandler(HttpProducer owner) {
    return new HttpResponseHandler(owner);
  }

  private class HttpResponseHandler implements ResponseHandler<AdaptrisMessage> {

    private HttpProducer owner;

    private HttpResponseHandler(HttpProducer p) {
      owner = p;
    }

    @Override
    public AdaptrisMessage handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
      int status = response.getStatusLine().getStatusCode();
      AdaptrisMessage reply = defaultIfNull(owner.getMessageFactory()).newMessage();
      if (status > 299) {
        if (!owner.ignoreServerResponseCode()) {
          throw new IOException("Failed to complete operation, got " + status);
        }
      }
      HttpEntity entity = response.getEntity();
      if (entity != null) {
        log.trace("Processing data from response {}", response.getEntity().getClass().getSimpleName());
        ContentType contentType = ContentType.get(entity);
        // If the content-type is null, then create a dummy one (which will give us a null Charset)
        if (contentType == null) {
          contentType = ContentType.create("text/plain");
        }
        try (InputStream in = entity.getContent(); OutputStream out = new BufferedOutputStream(reply.getOutputStream())) {
          IOUtils.copy(in, out);
          if (contentType.getCharset() != null) {
            reply.setContentEncoding(contentType.getCharset().name());
          }
        }
      }
      reply = owner.getResponseHeaderHandler().handle(response, reply);
      reply.addMetadata(new MetadataElement(CoreConstants.HTTP_PRODUCER_RESPONSE_CODE, String.valueOf(status)));
      return reply;
    }

  }

}

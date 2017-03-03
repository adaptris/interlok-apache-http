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
public class PayloadResponseHandlerFactory extends ResponseHandlerFactoryImpl {

  public PayloadResponseHandlerFactory() {
    super();
  }

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
      HttpEntity entity = response.getEntity();
      if (status > 299) {
        if (!owner.ignoreServerResponseCode()) {
          logAndThrow(status, entity);
        }
      }
      if (entity != null) {
        log.trace("Processing data from response {}", response.getEntity().getClass().getSimpleName());
        String encoding = contentEncoding(entity);
        try (InputStream in = entity.getContent(); OutputStream out = new BufferedOutputStream(reply.getOutputStream())) {
          IOUtils.copy(in, out);
          reply.setContentEncoding(encoding);
        }
      }
      reply = owner.getResponseHeaderHandler().handle(response, reply);
      reply.addMetadata(new MetadataElement(CoreConstants.HTTP_PRODUCER_RESPONSE_CODE, String.valueOf(status)));
      return reply;
    }

  }

}

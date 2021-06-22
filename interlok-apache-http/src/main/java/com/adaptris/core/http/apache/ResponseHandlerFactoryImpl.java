package com.adaptris.core.http.apache;

import static com.adaptris.core.AdaptrisMessageFactory.defaultIfNull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.entity.ContentType;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.CoreConstants;
import com.adaptris.core.MetadataElement;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@NoArgsConstructor
@Slf4j
public abstract class ResponseHandlerFactoryImpl implements ResponseHandlerFactory {


  protected abstract class ResponseHandlerImpl implements ResponseHandler<AdaptrisMessage> {

    protected HttpProducer owner;

    protected ResponseHandlerImpl(HttpProducer owner) {
      this.owner = owner;
    }

    // Abuse Generated annotation for covarge purposes since this isn't interesting in the context
    // of things.
    @lombok.Generated
    protected void logAndThrow(int status, HttpEntity entity) throws IOException {
      if (entity != null) {
        if (log.isTraceEnabled()) {
          String charset = StringUtils.defaultIfEmpty(contentEncoding(entity), Charset.defaultCharset().name());
          try (InputStream in = entity.getContent()) {
            log.trace("Error Data from remote server : {}", IOUtils.toString(in, charset));
         } catch (IOException e) {
            log.trace("No Error Data available");
          }
        }
      }
      throw new IOException("Failed to complete operation, got " + status);
    }

    // Since logAndThrow will always throw an exception, there are branches that are
    // not covered according to coverage reports.
    protected void exceptionBreakout(int status, HttpEntity entity) throws IOException {
      if (status > 299) {
        if (!owner.ignoreServerResponseCode()) {
          logAndThrow(status, entity);
        }
      }
    }

    protected String contentEncoding(HttpEntity entity) {
      ContentType contentType =
          Optional.ofNullable(ContentType.get(entity)).orElse(ContentType.create("text/plain"));
      return Optional.ofNullable(contentType.getCharset()).map((cs) -> cs.name()).orElse(null);
    }

    @Override
    public AdaptrisMessage handleResponse(HttpResponse response)
        throws ClientProtocolException, IOException {
      int status = response.getStatusLine().getStatusCode();
      AdaptrisMessage reply = defaultIfNull(owner.getMessageFactory()).newMessage();
      reply.addObjectHeader(OBJ_METADATA_PAYLOAD_MODIFIED, Boolean.FALSE);
      HttpEntity entity = response.getEntity();
      exceptionBreakout(status, entity);
      if (entity != null) {
        processEntity(entity, contentEncoding(entity), reply);
      }
      reply = owner.getResponseHeaderHandler().handle(response, reply);
      reply.addMetadata(
          new MetadataElement(CoreConstants.HTTP_PRODUCER_RESPONSE_CODE, String.valueOf(status)));
      reply.addObjectHeader(CoreConstants.HTTP_PRODUCER_RESPONSE_CODE, Integer.valueOf(status));
      return reply;
    }

    protected abstract void processEntity(HttpEntity entity, String encoding, AdaptrisMessage msg)
        throws IOException;

  }

}

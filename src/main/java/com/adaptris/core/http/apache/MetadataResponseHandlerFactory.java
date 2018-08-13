package com.adaptris.core.http.apache;

import static com.adaptris.core.AdaptrisMessageFactory.defaultIfNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.hibernate.validator.constraints.NotBlank;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.CoreConstants;
import com.adaptris.core.MetadataElement;
import com.adaptris.core.util.Args;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Implementation {@link ResponseHandlerFactory} that writes the response to the named metadata key.
 * 
 * @config apache-http-metadata-response-handler
 *
 */
@XStreamAlias("apache-http-metadata-response-handler")
public class MetadataResponseHandlerFactory extends ResponseHandlerFactoryImpl {

  @NotBlank
  private String metadataKey;


  public MetadataResponseHandlerFactory() {
    super();
  }

  public MetadataResponseHandlerFactory(String key) {
    this();
    setMetadataKey(key);
  }

  @Override
  public ResponseHandler<AdaptrisMessage> createResponseHandler(HttpProducer owner) {
    return new HttpResponseHandler(owner);
  }

  public String getMetadataKey() {
    return metadataKey;
  }

  public void setMetadataKey(String key) {
    this.metadataKey = Args.notBlank(key, "Metadata Key");
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
        StringBuilder builder = new StringBuilder();
        try (Reader in = getReader(entity.getContent(), encoding);
            StringBuilderWriter out = new StringBuilderWriter(builder)) {
          IOUtils.copy(in, out);
        }
        reply.addMessageHeader(getMetadataKey(), builder.toString());
      }
      reply = owner.getResponseHeaderHandler().handle(response, reply);
      reply.addMetadata(new MetadataElement(CoreConstants.HTTP_PRODUCER_RESPONSE_CODE, String.valueOf(status)));
      reply.addObjectHeader(CoreConstants.HTTP_PRODUCER_RESPONSE_CODE, Integer.valueOf(status));
      return reply;
    }

    private InputStreamReader getReader(InputStream in, String encoding) throws UnsupportedEncodingException {
      InputStreamReader reader = null;
      if (encoding == null) {
        reader = new InputStreamReader(in);
      } else {
        reader = new InputStreamReader(in, encoding);
      }
      return reader;
    }

  }

}

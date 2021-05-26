package com.adaptris.core.http.apache;

import com.adaptris.core.AdaptrisMessage;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;

/**
 * Factory for creating a {@link HttpClientResponseHandler} for use with the
 * {@link org.apache.hc.client5.http.classic.HttpClient#execute(org.apache.hc.core5.http.ClassicHttpRequest, org.apache.hc.core5.http.io.HttpClientResponseHandler)} method.
 *
 */
public interface ResponseHandlerFactory {

  /**
   * Key in object metadata that tells us if the payload has been modified by the ResponseHandler.
   */
  public static final String OBJ_METADATA_PAYLOAD_MODIFIED =
          HttpClientResponseHandler.class.getSimpleName() + "_modifiedPayload";


  HttpClientResponseHandler<AdaptrisMessage> createResponseHandler(HttpProducer owner);

}

package com.adaptris.core.http.apache;

import org.apache.http.client.ResponseHandler;
import com.adaptris.core.AdaptrisMessage;

/**
 * Factory for creating a {@link ResponseHandler} for use with the
 * {@link org.apache.http.client.HttpClient#execute(org.apache.http.client.methods.HttpUriRequest, ResponseHandler)} method.
 *
 */
public interface ResponseHandlerFactory {

  /**
   * Key in object metadata that tells us if the payload has been modified by the ResponseHandler.
   */
  public static final String OBJ_METADATA_PAYLOAD_MODIFIED =
      ResponseHandler.class.getSimpleName() + "_modifiedPayload";


  ResponseHandler<AdaptrisMessage> createResponseHandler(HttpProducer owner);

}

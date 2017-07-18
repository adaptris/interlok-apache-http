package com.adaptris.core.http.apache;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;

import com.adaptris.core.AdaptrisMessage;

/**
 * Factory for creating a {@link ResponseHandler} for use with the
 * {@link org.apache.http.client.HttpClient#execute(HttpUriRequest, ResponseHandler)} method.
 * 
 * @author lchan
 *
 */
public interface ResponseHandlerFactory {

  ResponseHandler<AdaptrisMessage> createResponseHandler(HttpProducer owner);

}

package com.adaptris.core.http.apache;

import org.apache.http.client.ResponseHandler;

import com.adaptris.core.AdaptrisMessage;

/**
 * Factory for creating a {@link ResponseHandler} for use with the
 * {@link org.apache.http.client.HttpClient#execute(org.apache.http.client.methods.HttpUriRequest, ResponseHandler)} method.
 *
 */
public interface ResponseHandlerFactory {

  ResponseHandler<AdaptrisMessage> createResponseHandler(HttpProducer owner);

}

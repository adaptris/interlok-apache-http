/**
 * Additional request interceptor builders for use with {@link com.adaptris.core.http.apache5.ApacheHttpProducer}.
 * <p>
 * Not all {@code org.apache.http.HttpRequestInterceptor} implementations are included here as
 * {@code org.apache.http.impl.client.HttpClientBuilder} adds a number of interceptors as part of its default behaviour (such as
 * {@code RequestExpectContinue} and {@code RequestContent}.
 * </p>
 */
package com.adaptris.core.http.apache5.request;

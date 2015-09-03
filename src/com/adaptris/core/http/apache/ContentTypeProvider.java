package com.adaptris.core.http.apache;

import org.apache.http.entity.ContentType;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.CoreException;

/**
 * Interface to provide a {@code Content-Type} header for the HTTP request.
 * 
 * @author lchan
 * 
 */
public interface ContentTypeProvider {

  /**
   * Get the content type.
   * 
   * @param msg the Adaptris Message
   * @return the content type.
   * @throws CoreException wrapping other exceptions
   */
  ContentType getContentType(AdaptrisMessage msg) throws CoreException;

}

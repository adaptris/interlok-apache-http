package com.adaptris.core.http.apache;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adaptris.util.stream.Slf4jLoggingOutputStream;

public abstract class ResponseHandlerFactoryImpl implements ResponseHandlerFactory {

  protected transient Logger log = LoggerFactory.getLogger(this.getClass());

  public ResponseHandlerFactoryImpl() {

  }

  protected void logAndThrow(int status, HttpEntity entity) throws IOException {
    if (entity != null) {
      if (log.isTraceEnabled()) {
        String encoding = contentEncoding(entity);
        try (InputStream in = entity.getContent();
            OutputStream slf4j = new Slf4jLoggingOutputStream(log, Slf4jLoggingOutputStream.LogLevel.TRACE);
            PrintStream out = encoding == null ? new PrintStream(slf4j) : new PrintStream(slf4j, false, encoding)) {
          out.println("Error Data from remote server :");
          IOUtils.copy(in, out);
        }
        catch (IOException e) {
          log.trace("No Error Data available");
        }
      }
    }
    throw new IOException("Failed to complete operation, got " + status);
  }

  protected String contentEncoding(HttpEntity entity) {
    ContentType contentType = ContentType.get(entity);
    if (contentType == null) {
      contentType = ContentType.create("text/plain");
    }
    return contentType.getCharset() != null ? contentType.getCharset().name() : null;
  }
}

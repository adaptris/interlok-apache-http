package com.adaptris.core.http.apache5;

import org.junit.Rule;
import org.junit.rules.TestName;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.MetadataElement;
import com.adaptris.core.util.Args;

public abstract class ResponseHeadersCase {

  @Rule
  public TestName testName = new TestName();

  protected static boolean contains(AdaptrisMessage request, String headerKey, String headerValue) {
    boolean matched = false;
    String compareKey = Args.notEmpty(headerKey, "key");
    String compareValue = Args.notEmpty(headerValue, "value");
    for (MetadataElement e : request.getMetadata()) {
      if (compareKey.equals(e.getKey()) && compareValue.equals(e.getValue())) {
        matched = true;
        break;
      }
    }
    return matched;
  }
}

package com.adaptris.core.http.apache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.MetadataElement;
import com.adaptris.core.util.Args;

public abstract class ResponseHeadersCase {

  private TestInfo testInfo;

  @BeforeEach
  public void beforeTests(TestInfo info) {
    testInfo = info;
  }

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

  public String getName() {
    return testInfo.getDisplayName().substring(0, testInfo.getDisplayName().indexOf("("));
  }

}

package com.adaptris.core.http.apache5;

import java.util.Iterator;

import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.Header;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import com.adaptris.core.util.Args;

public abstract class RequestHeadersCase {

  private TestInfo testInfo;

  @BeforeEach
  public void beforeTests(TestInfo info) {
    testInfo = info;
  }

  protected static boolean contains(HttpUriRequestBase request, String headerKey, String headerValue) {
    boolean matched = false;
    String compareKey = Args.notEmpty(headerKey, "key");
    String compareValue = Args.notEmpty(headerValue, "value");
    for (Iterator<?> iter = request.headerIterator(compareKey); iter.hasNext();) {
      // At least one header of that name...
      Header header = (Header) iter.next();
      if (compareValue.equals(header.getValue())) {
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

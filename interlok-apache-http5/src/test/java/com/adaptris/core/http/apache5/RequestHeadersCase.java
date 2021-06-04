package com.adaptris.core.http.apache5;

import com.adaptris.core.util.Args;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.Header;
import org.junit.Rule;
import org.junit.rules.TestName;

import java.util.Iterator;

public abstract class RequestHeadersCase {

  @Rule
  public TestName testName = new TestName();

  protected static boolean contains(HttpUriRequestBase request, String headerKey, String headerValue) {
    boolean matched = false;
    String compareKey = Args.notEmpty(headerKey, "key");
    String compareValue = Args.notEmpty(headerValue, "value");
    for (Iterator i = request.headerIterator(compareKey); i.hasNext();) {
      // At least one header of that name...
      Header h = (Header)i.next();
      if (compareValue.equals(h.getValue())) {
        matched = true;
        break;
      }
    }
    return matched;
  }
}

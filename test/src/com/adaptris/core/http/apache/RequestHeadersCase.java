package com.adaptris.core.http.apache;

import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.client.methods.HttpRequestBase;
import org.junit.Rule;
import org.junit.rules.TestName;

import com.adaptris.core.util.Args;

public abstract class RequestHeadersCase {

  @Rule
  public TestName testName = new TestName();

  protected static boolean contains(HttpRequestBase request, String headerKey, String headerValue) {
    boolean matched = false;
    String compareKey = Args.notEmpty(headerKey, "key");
    String compareValue = Args.notEmpty(headerValue, "value");
    for (HeaderIterator i = request.headerIterator(compareKey); i.hasNext();) {
      // At least one header of that name...
      Header h = i.nextHeader();
      if (compareValue.equals(h.getValue())) {
        matched = true;
        break;
      }
    }
    return matched;
  }
}

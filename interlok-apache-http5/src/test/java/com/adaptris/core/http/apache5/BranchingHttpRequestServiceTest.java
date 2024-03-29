/*
 * Copyright 2015 Adaptris Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package com.adaptris.core.http.apache5;
import static com.adaptris.core.http.apache5.JettyHelper.createAndStartChannel;
import static com.adaptris.core.http.apache5.JettyHelper.createURL;
import static com.adaptris.core.http.apache5.JettyHelper.stopAndRelease;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.BranchingServiceCollection;
import com.adaptris.core.Channel;
import com.adaptris.core.DefaultMessageFactory;
import com.adaptris.core.NullService;
import com.adaptris.core.ServiceException;
import com.adaptris.core.http.client.ExactMatch;
import com.adaptris.core.http.client.RangeMatch;
import com.adaptris.core.services.LogMessageService;
import com.adaptris.core.services.exception.ConfiguredException;
import com.adaptris.core.services.exception.ThrowExceptionService;
import com.adaptris.core.stubs.DefectiveMessageFactory;
import com.adaptris.core.stubs.MockMessageProducer;
import com.adaptris.interlok.junit.scaffolding.services.ExampleServiceCase;

public class BranchingHttpRequestServiceTest extends ExampleServiceCase {
  private static final String TEXT = "ABCDEFG";

  @Test
  public void testIsBranching() throws Exception {
    BranchingHttpRequestService service = new BranchingHttpRequestService();
    assertTrue(service.isBranching());
    assertEquals("", service.getDefaultServiceId());
  }

  @Test
  public void testService_Error() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    Channel c = createAndStartChannel(mock);
    BranchingHttpRequestService service = new BranchingHttpRequestService(createURL(c));
    service.setContentType("text/complicated");
    service.setDefaultServiceId("DefaultServiceId");
    AdaptrisMessage msg = new DefectiveMessageFactory().newMessage(TEXT);
    try {
      c.requestStart();
      execute(service, msg);
      fail();
    }
    catch (ServiceException expected) {

    }
    finally {
      stopAndRelease(c);
    }
  }

  @Test
  public void testService_DefaultServiceId() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    Channel c = createAndStartChannel(mock);
    BranchingHttpRequestService service = new BranchingHttpRequestService(createURL(c));

    service.setContentType("text/complicated");
    service.setDefaultServiceId("DefaultServiceId");
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    try {
      c.requestStart();
      execute(service, msg);
      waitForMessages(mock, 1);
    }
    finally {
      stopAndRelease(c);
    }
    assertEquals(1, mock.messageCount());
    assertEquals("DefaultServiceId", msg.getNextServiceId());
  }

  @Test
  public void testService_BlankServiceId() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    Channel c = createAndStartChannel(mock);
    BranchingHttpRequestService service = new BranchingHttpRequestService();
    service.setContentType("text/complicated");
    service.setDefaultServiceId("");
    service.setUrl(createURL(c));

    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    msg.setNextServiceId("should-be-overriden");
    try {
      c.requestStart();
      execute(service, msg);
      waitForMessages(mock, 1);
    } finally {
      stopAndRelease(c);
    }
    assertEquals(1, mock.messageCount());
    assertEquals("", msg.getNextServiceId());
  }

  @Test
  public void testService_NullServiceId() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    Channel c = createAndStartChannel(mock);
    BranchingHttpRequestService service = new BranchingHttpRequestService();
    service.setUrl(createURL(c));

    service.setContentType("text/complicated");
    service.setDefaultServiceId(null);
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    msg.setNextServiceId("should-not-be-overriden");
    try {
      c.requestStart();
      execute(service, msg);
      waitForMessages(mock, 1);
    } finally {
      stopAndRelease(c);
    }
    assertEquals(1, mock.messageCount());
    assertEquals("should-not-be-overriden", msg.getNextServiceId());
  }


  @Test
  public void testService_ExactMatch() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    Channel c = createAndStartChannel(mock);
    BranchingHttpRequestService service = new BranchingHttpRequestService(createURL(c));

    service.setContentType("text/complicated");
    service.setDefaultServiceId("DefaultServiceId");
    service.getStatusMatches().add(new ExactMatch(500, "500 Server Error"));
    service.getStatusMatches().add(new ExactMatch(200, "200 OK"));
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    try {
      c.requestStart();
      execute(service, msg);
      waitForMessages(mock, 1);
    }
    finally {
      stopAndRelease(c);
    }
    assertEquals(1, mock.messageCount());
    assertEquals("200 OK", msg.getNextServiceId());
  }

  @Test
  public void testService_RangeMatch() throws Exception {
    MockMessageProducer mock = new MockMessageProducer();
    Channel c = createAndStartChannel(mock);
    BranchingHttpRequestService service = new BranchingHttpRequestService(createURL(c));

    service.setContentType("text/complicated");
    service.setDefaultServiceId("DefaultServiceId");
    service.getStatusMatches().add(new RangeMatch(100, 199, "1XX Informational"));
    service.getStatusMatches().add(new RangeMatch(300, 399, "3XX Moved"));
    service.getStatusMatches().add(new RangeMatch(200, 299, "2XX OK"));
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage(TEXT);
    try {
      c.requestStart();
      execute(service, msg);
      waitForMessages(mock, 1);
    }
    finally {
      stopAndRelease(c);
    }
    assertEquals(1, mock.messageCount());
    assertEquals("2XX OK", msg.getNextServiceId());
  }

  @Override
  protected BranchingServiceCollection retrieveObjectForSampleConfig() {
    BranchingHttpRequestService service = createForExamples();
    BranchingServiceCollection sl = new BranchingServiceCollection();
    sl.addService(service);
    sl.setFirstServiceId(service.getUniqueId());
    sl.addService(new ThrowExceptionService("5XX Server Error", new ConfiguredException("Got 5XX error from server")));
    sl.addService(new NullService("Not Found"));
    sl.addService(new LogMessageService("2XX OK"));
    sl.addService(new NullService("DefaultServiceId"));

    return sl;
  }

  private BranchingHttpRequestService createForExamples() {
    BranchingHttpRequestService service = new BranchingHttpRequestService("http://myhost.com/url/to/get/data/from/or/post/data/to");

    service.setContentType("text/complicated");
    service.setDefaultServiceId("DefaultServiceId");
    service.setUniqueId("GetData");
    service.setMethod("GET");
    service.getStatusMatches().add(new RangeMatch(500, 599, "5XX Server Error"));
    service.getStatusMatches().add(new RangeMatch(200, 299, "2XX OK"));
    service.getStatusMatches().add(new ExactMatch(404, "Not Found"));
    return service;
  }

  @Override
  protected String createBaseFileName(Object object) {
    return BranchingHttpRequestService.class.getName();
  }

}

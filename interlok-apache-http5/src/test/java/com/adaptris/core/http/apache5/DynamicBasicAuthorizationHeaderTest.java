package com.adaptris.core.http.apache5;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.adaptris.core.AdaptrisMessageFactory;
import com.adaptris.core.CoreException;

public class DynamicBasicAuthorizationHeaderTest {


  @Test
  public void testUsername() throws Exception {
    DynamicBasicAuthorizationHeader auth = new DynamicBasicAuthorizationHeader();
    assertNull(auth.getUsername());
    auth.setUsername("hello");
    assertEquals("hello", auth.getUsername());
  }

  @Test
  public void testPassword() throws Exception {
    DynamicBasicAuthorizationHeader auth = new DynamicBasicAuthorizationHeader();
    assertNull(auth.getPassword());
    auth.setPassword("hello");
    assertEquals("hello", auth.getPassword());
  }

  @Test
  public void testDoAuth_NoUserPassword() throws Exception {
    DynamicBasicAuthorizationHeader auth = new DynamicBasicAuthorizationHeader();
    try {
      auth.setup("http://localhost:8080", AdaptrisMessageFactory.getDefaultInstance().newMessage(), null);
      fail();
    } catch (CoreException expected) {

    }
  }
}

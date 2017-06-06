/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.geode.internal.security;

import static org.apache.geode.distributed.ConfigurationProperties.SECURITY_CLIENT_AUTHENTICATOR;
import static org.apache.geode.distributed.ConfigurationProperties.SECURITY_MANAGER;
import static org.apache.geode.distributed.ConfigurationProperties.SECURITY_PEER_AUTHENTICATOR;
import static org.apache.geode.distributed.ConfigurationProperties.SECURITY_SHIRO_INIT;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.geode.distributed.internal.DistributionConfig;
import org.apache.geode.security.GemFireSecurityException;
import org.apache.geode.security.TestSecurityManager;
import org.apache.geode.test.junit.categories.UnitTest;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Properties;

@Category(UnitTest.class)
public class SecurityServiceTest {

  private Properties properties;
  private DistributionConfig distributionConfig;
  private SecurityService securityService;

  @Before
  public void before() {
    this.properties = new Properties();
    this.distributionConfig = mock(DistributionConfig.class);
    when(this.distributionConfig.getSecurityProps()).thenReturn(this.properties);
    this.securityService = SecurityServiceFactory.create(null, this.distributionConfig);
  }

  @After
  public void after() throws Exception {
    this.securityService.close();
    SecurityUtils.setSecurityManager(null);
  }

  @Test
  public void testGetObjectFromConstructor() {
    String string = SecurityService.getObjectOfType(String.class.getName(), String.class);
    assertNotNull(string);

    CharSequence charSequence =
        SecurityService.getObjectOfType(String.class.getName(), CharSequence.class);
    assertNotNull(charSequence);

    assertThatThrownBy(() -> SecurityService.getObjectOfType("com.abc.testString", String.class))
        .isInstanceOf(GemFireSecurityException.class);

    assertThatThrownBy(() -> SecurityService.getObjectOfType(String.class.getName(), Boolean.class))
        .isInstanceOf(GemFireSecurityException.class);

    assertThatThrownBy(() -> SecurityService.getObjectOfType("", String.class))
        .isInstanceOf(GemFireSecurityException.class);

    assertThatThrownBy(() -> SecurityService.getObjectOfType(null, String.class))
        .isInstanceOf(GemFireSecurityException.class);

    assertThatThrownBy(() -> SecurityService.getObjectOfType("  ", String.class))
        .isInstanceOf(GemFireSecurityException.class);
  }

  @Test
  public void testGetObjectFromFactoryMethod() {
    String string =
        SecurityService.getObjectOfType(Factories.class.getName() + ".getString", String.class);
    assertNotNull(string);

    CharSequence charSequence =
        SecurityService.getObjectOfType(Factories.class.getName() + ".getString", String.class);
    assertNotNull(charSequence);

    assertThatThrownBy(() -> SecurityService
        .getObjectOfType(Factories.class.getName() + ".getStringNonStatic", String.class))
            .isInstanceOf(GemFireSecurityException.class);

    assertThatThrownBy(() -> SecurityService
        .getObjectOfType(Factories.class.getName() + ".getNullString", String.class))
            .isInstanceOf(GemFireSecurityException.class);
  }

  @Test
  public void testInitialSecurityFlags() {
    // initial state of SecurityService
    assertFalse(this.securityService.isIntegratedSecurity());
    assertFalse(this.securityService.isClientSecurityRequired());
    assertFalse(this.securityService.isPeerSecurityRequired());
  }

  @Test
  public void testInitWithSecurityManager() {
    this.properties.setProperty(SECURITY_MANAGER, "org.apache.geode.security.TestSecurityManager");
    this.properties.setProperty(TestSecurityManager.SECURITY_JSON,
        "org/apache/geode/security/templates/security.json");

    this.securityService = SecurityServiceFactory.create(null, this.distributionConfig);

    assertTrue(this.securityService.isIntegratedSecurity());
    assertTrue(this.securityService.isClientSecurityRequired());
    assertTrue(this.securityService.isPeerSecurityRequired());
  }

  @Test
  public void testInitWithClientAuthenticator() {
    this.properties.setProperty(SECURITY_CLIENT_AUTHENTICATOR, "org.abc.test");
    this.securityService = SecurityServiceFactory.create(null, this.distributionConfig);

    assertFalse(this.securityService.isIntegratedSecurity());
    assertTrue(this.securityService.isClientSecurityRequired());
    assertFalse(this.securityService.isPeerSecurityRequired());
  }

  @Test
  public void testInitWithPeerAuthenticator() {
    this.properties.setProperty(SECURITY_PEER_AUTHENTICATOR, "org.abc.test");
    this.securityService = SecurityServiceFactory.create(null, this.distributionConfig);

    assertFalse(this.securityService.isIntegratedSecurity());
    assertFalse(this.securityService.isClientSecurityRequired());
    assertTrue(this.securityService.isPeerSecurityRequired());
  }

  @Test
  public void testInitWithAuthenticators() {
    this.properties.setProperty(SECURITY_CLIENT_AUTHENTICATOR, "org.abc.test");
    this.properties.setProperty(SECURITY_PEER_AUTHENTICATOR, "org.abc.test");

    this.securityService = SecurityServiceFactory.create(null, this.distributionConfig);

    assertFalse(this.securityService.isIntegratedSecurity());
    assertTrue(this.securityService.isClientSecurityRequired());
    assertTrue(this.securityService.isPeerSecurityRequired());
  }

  @Test
  public void testInitWithShiroAuthenticator() {
    this.properties.setProperty(SECURITY_SHIRO_INIT, "shiro.ini");

    this.securityService = SecurityServiceFactory.create(null, this.distributionConfig);

    assertTrue(this.securityService.isIntegratedSecurity());
    assertTrue(this.securityService.isClientSecurityRequired());
    assertTrue(this.securityService.isPeerSecurityRequired());
  }

  @Test
  public void testNoInit() {
    assertFalse(this.securityService.isIntegratedSecurity());
  }

  @Test
  public void testInitWithOutsideShiroSecurityManager() {
    SecurityUtils.setSecurityManager(new DefaultSecurityManager());
    this.securityService = SecurityServiceFactory.create(null, this.distributionConfig);

    assertTrue(this.securityService.isIntegratedSecurity());
  }

  // @Test
  // public void testSetSecurityManager() {
  // // initially
  // assertFalse(this.securityService.isIntegratedSecurity());
  //
  // // init with client authenticator
  // this.properties.setProperty(SECURITY_CLIENT_AUTHENTICATOR, "org.abc.test");
  // this.securityService = SecurityServiceFactory.create(null, this.distributionConfig);
  //
  // assertFalse(this.securityService.isIntegratedSecurity());
  // assertTrue(this.securityService.isClientSecurityRequired());
  // assertFalse(this.securityService.isPeerSecurityRequired());
  //
  // // set a security manager
  // this.securityService.setSecurityManager(new SimpleTestSecurityManager());
  // assertTrue(this.securityService.isIntegratedSecurity());
  // assertTrue(this.securityService.isClientSecurityRequired());
  // assertTrue(this.securityService.isPeerSecurityRequired());
  // assertFalse(this.securityService.needPostProcess());
  //
  // // set a post processor
  // this.securityService.setPostProcessor(new TestPostProcessor());
  // assertTrue(this.securityService.isIntegratedSecurity());
  // assertTrue(this.securityService.needPostProcess());
  // }

  private static class Factories {

    public static String getString() {
      return new String();
    }

    public static String getNullString() {
      return null;
    }

    public String getStringNonStatic() {
      return new String();
    }

    public static Boolean getBoolean() {
      return Boolean.TRUE;
    }
  }
}

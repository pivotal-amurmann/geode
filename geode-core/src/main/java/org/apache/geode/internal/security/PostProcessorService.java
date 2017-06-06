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

import org.apache.commons.lang.SerializationException;
import org.apache.commons.lang.StringUtils;
import org.apache.geode.GemFireIOException;
import org.apache.geode.internal.cache.EntryEventImpl;
import org.apache.geode.internal.logging.LogService;
import org.apache.geode.internal.util.BlobHelper;
import org.apache.geode.management.internal.security.ResourceOperation;
import org.apache.geode.security.PostProcessor;
import org.apache.geode.security.ResourcePermission;
import org.apache.geode.security.SecurityManager;
import org.apache.logging.log4j.Logger;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadState;

import java.io.IOException;
import java.io.Serializable;
import java.util.Properties;
import java.util.concurrent.Callable;

/**
 * Security service with PostProcessor but no SecurityManager
 */
public class PostProcessorService implements SecurityService {
  private static Logger logger = LogService.getLogger(LogService.SECURITY_LOGGER_NAME);

  private final PostProcessor postProcessor;

  PostProcessorService(final PostProcessor postProcessor) {
    if (postProcessor == null) {
      throw new IllegalArgumentException("PostProcessor must not be null");
    }
    this.postProcessor = postProcessor;
  }

  @Override
  public void initSecurity(final Properties securityProps) {
    this.postProcessor.init(securityProps);
  }

  @Override
  public void setSecurityManager(final SecurityManager securityManager) {
    // nothing
  }

  @Override
  public void setPostProcessor(final PostProcessor postProcessor) {
    // nothing
  }

  /**
   * It first looks the shiro subject in AccessControlContext since JMX will use multiple threads to
   * process operations from the same client, then it looks into Shiro's thead context.
   *
   * @return the shiro subject, null if security is not enabled
   */
  @Override
  public Subject getSubject() {
    return null;
  }

  /**
   * @return null if security is not enabled, otherwise return a shiro subject
   */
  @Override
  public Subject login(final Properties credentials) {
    return null;
  }

  @Override
  public void logout() {
    // nothing
  }

  @Override
  public Callable associateWith(final Callable callable) {
    return null;
  }

  @Override
  public ThreadState bindSubject(final Subject subject) {
    return null;
  }

  @Override
  public void authorize(final ResourceOperation resourceOperation) {
    // nothing
  }

  @Override
  public void authorizeClusterManage() {
    // nothing
  }

  @Override
  public void authorizeClusterWrite() {
    // nothing
  }

  @Override
  public void authorizeClusterRead() {
    // nothing
  }

  @Override
  public void authorizeDataManage() {
    // nothing
  }

  @Override
  public void authorizeDataWrite() {
    // nothing
  }

  @Override
  public void authorizeDataRead() {
    // nothing
  }

  @Override
  public void authorizeRegionManage(final String regionName) {
    // nothing
  }

  @Override
  public void authorizeRegionManage(final String regionName, final String key) {
    // nothing
  }

  @Override
  public void authorizeRegionWrite(final String regionName) {
    // nothing
  }

  @Override
  public void authorizeRegionWrite(final String regionName, final String key) {
    // nothing
  }

  @Override
  public void authorizeRegionRead(final String regionName) {
    // nothing
  }

  @Override
  public void authorizeRegionRead(final String regionName, final String key) {
    // nothing
  }

  @Override
  public void authorize(final String resource, final String operation) {
    // nothing
  }

  @Override
  public void authorize(final String resource, final String operation, final String regionName) {
    // nothing
  }

  @Override
  public void authorize(final String resource, final String operation, String regionName,
      final String key) {
    // nothing
  }

  @Override
  public void authorize(final ResourcePermission context) {
    // nothing
  }

  @Override
  public void close() {
    if (this.postProcessor != null) {
      this.postProcessor.close();
    }
  }

  /**
   * postProcess call already has this logic built in, you don't need to call this everytime you
   * call postProcess. But if your postProcess is pretty involved with preparations and you need to
   * bypass it entirely, call this first.
   */
  @Override
  public boolean needPostProcess() {
    return true;
  }

  @Override
  public Object postProcess(final String regionPath, final Object key, final Object value,
      final boolean valueIsSerialized) {
    return postProcess(null, regionPath, key, value, valueIsSerialized);
  }

  @Override
  public Object postProcess(Object principal, final String regionPath, final Object key,
      final Object value, final boolean valueIsSerialized) {
    if (principal == null) {
      Subject subject = getSubject();
      if (subject == null) {
        return value;
      }
      principal = (Serializable) subject.getPrincipal();
    }

    String regionName = StringUtils.stripStart(regionPath, "/");
    Object newValue;

    // if the data is a byte array, but the data itself is supposed to be an object, we need to
    // deserialize it before we pass it to the callback.
    if (valueIsSerialized && value instanceof byte[]) {
      try {
        Object oldObj = EntryEventImpl.deserialize((byte[]) value);
        Object newObj = this.postProcessor.processRegionValue(principal, regionName, key, oldObj);
        newValue = BlobHelper.serializeToBlob(newObj);
      } catch (IOException | SerializationException e) {
        throw new GemFireIOException("Exception de/serializing entry value", e);
      }
    } else {
      newValue = this.postProcessor.processRegionValue(principal, regionName, key, value);
    }

    return newValue;
  }

  @Override
  public SecurityManager getSecurityManager() {
    return null;
  }

  @Override
  public PostProcessor getPostProcessor() {
    return this.postProcessor;
  }

  @Override
  public boolean isIntegratedSecurity() {
    return false;
  }

  @Override
  public boolean isClientSecurityRequired() {
    return false;
  }

  @Override
  public boolean isPeerSecurityRequired() {
    return false;
  }
}

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
package org.apache.geode.internal.cache.tier.sockets;

import static junit.framework.TestCase.assertFalse;
import static org.apache.geode.internal.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.geode.internal.cache.InternalCache;
import org.apache.geode.internal.cache.tier.Acceptor;
import org.apache.geode.internal.cache.tier.CachedRegionHelper;
import org.apache.geode.internal.cache.tier.sockets.sasl.AuthenticationContext;
import org.apache.geode.internal.cache.tier.sockets.sasl.AuthenticationService;
import org.apache.geode.internal.security.SecurityService;
import org.apache.geode.test.junit.categories.UnitTest;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

@Category(UnitTest.class)
public class GenericProtocolServerConnectionTest {
//  @Test
//  public void testProcessFlag() throws IOException {
//    try {
//      System.setProperty("geode.feature-protobuf-protocol", "true");
//      ServerConnection serverConnection = createIOExceptionThrowingServerConnection();
//      assertTrue(serverConnection.processMessages);
//      serverConnection.doOneMessage();
//      assertFalse(serverConnection.processMessages);
//    } finally {
//      System.clearProperty("geode.feature-protobuf-protocol");
//    }
//  }

//  @Test
//  public void testAuthenticationSuccess() throws IOException {
//    GenericServerConnectionBuilder builder = new GenericServerConnectionBuilder();
//    when(builder.mockAuthenticationService.process(any(), any())).thenReturn(
//        AuthenticationService.AuthenticationProgress.AUTHENTICATION_COMPLETE
//    );
//
//    GenericProtocolServerConnection testConnection = builder.buildConnection();
//
//    testConnection.doOneMessage();
//    assertTrue(testConnection.processMessages);
//    verify(builder.mockAuthenticationService, times(1)).process(builder.stubInputStream, builder.stubOutputStream);
//    verify(builder.stubClientProtocolMessageHandler, times(0)).receiveMessage(any(), any(), any());
//
//    testConnection.doOneMessage();
//    assertTrue(testConnection.processMessages);
//    verify(builder.mockAuthenticationService, times(1)).process(builder.stubInputStream, builder.stubOutputStream);
//    verify(builder.stubClientProtocolMessageHandler, times(1)).receiveMessage(eq(builder.stubInputStream), eq(builder.stubOutputStream), any());
//  }
//
//  @Test
//  public void testAuthenticationNotCompleteYet() throws IOException {
//    GenericServerConnectionBuilder builder = new GenericServerConnectionBuilder();
//    when(builder.mockAuthenticationService.process(any(), any())).thenReturn(
//        AuthenticationService.AuthenticationProgress.AUTHENTICATION_IN_PROGRESS
//    );
//
//    GenericProtocolServerConnection testConnection = builder.buildConnection();
//
//    testConnection.doOneMessage();
//    assertTrue(testConnection.processMessages);
//
//    testConnection.doOneMessage();
//    assertTrue(testConnection.processMessages);
//
//    verify(builder.mockAuthenticationService, times(2)).process(builder.stubInputStream, builder.stubOutputStream);
//    verify(builder.stubClientProtocolMessageHandler, times(0)).receiveMessage(any(), any(), any());
//  }
//
//  @Test
//  public void testAuthenticationFailure() throws IOException {
//    GenericServerConnectionBuilder builder = new GenericServerConnectionBuilder();
//    when(builder.mockAuthenticationService.process(any(), any())).thenReturn(
//        AuthenticationService.AuthenticationProgress.AUTHENTICATION_FAILED
//    );
//
//    GenericProtocolServerConnection testConnection = builder.buildConnection();
//
//    testConnection.doOneMessage();
//    assertFalse(testConnection.processMessages);
//
//    verify(builder.mockAuthenticationService, times(1)).process(builder.stubInputStream, builder.stubOutputStream);
//    verify(builder.stubClientProtocolMessageHandler, times(0)).receiveMessage(any(), any(), any());
//  }

//  public class GenericServerConnectionBuilder {
//    public Socket socketStub;
//    public OutputStream stubOutputStream;
//    public InputStream stubInputStream;
//    public InternalCache internalCacheStub;
//    public CachedRegionHelper cachedRegionHelperStub;
//    public CacheServerStats stubCacheServerStats;
//    public AcceptorImpl stubAcceptor;
//    public ClientProtocolMessageHandler stubClientProtocolMessageHandler;
//    public SecurityService stubSecurityService;
//    public AuthenticationContext mockAuthenticationService;
//
//    public GenericServerConnectionBuilder() throws IOException {
//      stubOutputStream = mock(OutputStream.class);
//      stubInputStream = mock(InputStream.class);
//
//      socketStub = mock(Socket.class);
//      when(socketStub.getInetAddress()).thenReturn(InetAddress.getByName("localhost"));
//      when(socketStub.getInputStream()).thenReturn(stubInputStream);
//      when(socketStub.getOutputStream()).thenReturn(stubOutputStream);
//
//      internalCacheStub = mock(InternalCache.class);
//      cachedRegionHelperStub = mock(CachedRegionHelper.class);
//      stubCacheServerStats = mock(CacheServerStats.class);
//      stubAcceptor = mock(AcceptorImpl.class);
//      stubClientProtocolMessageHandler = mock(ClientProtocolMessageHandler.class);
//      stubSecurityService = mock(SecurityService.class);
//      mockAuthenticationService = mock(AuthenticationService.class);
//    }
//
//    public GenericProtocolServerConnection buildConnection() {
//      return new GenericProtocolServerConnection(
//          socketStub,
//          internalCacheStub,
//          cachedRegionHelperStub,
//          stubCacheServerStats,
//          1,
//          1,
//          "PLAIN",
//          (byte) 1,
//          stubAcceptor,
//          stubClientProtocolMessageHandler,
//          stubSecurityService,
//          mockAuthenticationService
//      );
//    }
//  }

  private static ServerConnection createIOExceptionThrowingServerConnection() throws IOException {
    Socket socketMock = mock(Socket.class);
    when(socketMock.getInetAddress()).thenReturn(InetAddress.getByName("localhost"));

    ClientProtocolMessageHandler clientProtocolMock = mock(ClientProtocolMessageHandler.class);
    doThrow(new IOException()).when(clientProtocolMock).receiveMessage(any(), any(), any());

    return new GenericProtocolServerConnection(socketMock, mock(InternalCache.class),
        mock(CachedRegionHelper.class), mock(CacheServerStats.class), 0, 0, "",
        Acceptor.PROTOBUF_CLIENT_SERVER_PROTOCOL, mock(AcceptorImpl.class), clientProtocolMock,
        mock(SecurityService.class), null);
  }
}



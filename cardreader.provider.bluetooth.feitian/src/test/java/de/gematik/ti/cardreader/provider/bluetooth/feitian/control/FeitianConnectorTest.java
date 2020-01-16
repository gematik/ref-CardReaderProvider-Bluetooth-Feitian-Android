/*
 * Copyright (c) 2020 gematik GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.gematik.ti.cardreader.provider.bluetooth.feitian.control;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.ParcelUuid;

import de.gematik.ti.cardreader.provider.bluetooth.feitian.entities.FeitianCard;

public class FeitianConnectorTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void testRun() throws Exception {
        BluetoothDevice bluetoothDevice = Mockito.mock(BluetoothDevice.class);
        BluetoothSocket bluetoothSocket = Mockito.mock(BluetoothSocket.class);

        UUID uuid = UUID.randomUUID();
        Mockito.when(bluetoothDevice.getUuids()).thenReturn(new ParcelUuid[] { new ParcelUuid(uuid) });
        Mockito.when(bluetoothDevice.getName()).thenReturn("Junit Mock");
        Mockito.when(bluetoothDevice.createInsecureRfcommSocketToServiceRecord(ArgumentMatchers.any())).thenReturn(bluetoothSocket);
        Mockito.when(bluetoothSocket.isConnected()).thenReturn(true);
        Mockito.when(bluetoothSocket.getInputStream()).thenReturn(Mockito.mock(InputStream.class));
        Mockito.when(bluetoothSocket.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));

        AtomicReference<FeitianCard> feitianCardCallBack = new AtomicReference<>();
        FeitianConnector feitianConnector = new FeitianConnector(feitianCard -> feitianCardCallBack.set(feitianCard), bluetoothDevice);

        Awaitility.await().atMost(Duration.ONE_MINUTE).until(() -> !feitianConnector.isActive());

        FeitianCard feitianCardCb = feitianCardCallBack.get();
        FeitianCard feitianCard = feitianConnector.getFeitianCard();
        Assert.assertNotNull(feitianCardCb);
        Assert.assertNotNull(feitianCard);
        Assert.assertEquals(feitianCard, feitianCardCb);

        feitianConnector.closeSocket();
        Mockito.verify(bluetoothSocket, Mockito.times(1)).close();

    }

    @Test(expected = RuntimeException.class)
    public void testRunConnectException() throws Exception {
        BluetoothDevice bluetoothDevice = Mockito.mock(BluetoothDevice.class);
        BluetoothSocket bluetoothSocket = Mockito.mock(BluetoothSocket.class);

        UUID uuid = UUID.randomUUID();
        Mockito.when(bluetoothDevice.getUuids()).thenReturn(new ParcelUuid[] { new ParcelUuid(uuid) });
        Mockito.when(bluetoothDevice.getName()).thenReturn("Junit Mock");
        Mockito.when(bluetoothDevice.createInsecureRfcommSocketToServiceRecord(ArgumentMatchers.any())).thenReturn(bluetoothSocket);
        Mockito.doThrow(new RuntimeException("JunitTest")).when(bluetoothSocket).connect();

        AtomicReference<FeitianCard> feitianCardCallBack = new AtomicReference<>();
        FeitianConnector feitianConnector = new FeitianConnector(feitianCard -> feitianCardCallBack.set(feitianCard), bluetoothDevice);

        Awaitility.await().atMost(Duration.ONE_MINUTE).until(() -> !feitianConnector.isActive());

        FeitianCard feitianCardCb = feitianCardCallBack.get();
        FeitianCard feitianCard = feitianConnector.getFeitianCard();
        Assert.assertNull("feitianCardCb is null", feitianCardCb);
        Assert.assertNull("feitianConnector.getFeitianCard() is null", feitianCard);
    }

    @Test(expected = RuntimeException.class)
    public void testRunGetInputStreamException() throws Exception {
        BluetoothDevice bluetoothDevice = Mockito.mock(BluetoothDevice.class);
        BluetoothSocket bluetoothSocket = Mockito.mock(BluetoothSocket.class);

        UUID uuid = UUID.randomUUID();
        Mockito.when(bluetoothDevice.getUuids()).thenReturn(new ParcelUuid[] { new ParcelUuid(uuid) });
        Mockito.when(bluetoothDevice.getName()).thenReturn("Junit Mock");
        Mockito.when(bluetoothDevice.createInsecureRfcommSocketToServiceRecord(ArgumentMatchers.any())).thenReturn(bluetoothSocket);
        Mockito.when(bluetoothSocket.isConnected()).thenReturn(true);
        Mockito.doThrow(new RuntimeException("JunitTest")).when(bluetoothSocket).getInputStream();

        AtomicReference<FeitianCard> feitianCardCallBack = new AtomicReference<>();
        FeitianConnector feitianConnector = new FeitianConnector(feitianCard -> feitianCardCallBack.set(feitianCard), bluetoothDevice);

        Awaitility.await().atMost(Duration.ONE_MINUTE).until(() -> !feitianConnector.isActive());

        FeitianCard feitianCardCb = feitianCardCallBack.get();
        FeitianCard feitianCard = feitianConnector.getFeitianCard();
        Assert.assertNull("feitianCardCb is null", feitianCardCb);
        Assert.assertNull("feitianConnector.getFeitianCard() is null", feitianCard);
    }

}

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

package de.gematik.ti.cardreader.provider.bluetooth.feitian.entities;

import java.util.UUID;

import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.hamcrest.core.Is;
import org.hamcrest.core.IsNull;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import com.feitian.readerdk.Tool.DK;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.ParcelUuid;

import de.gematik.ti.cardreader.provider.bluetooth.feitian.Whitebox;
import de.gematik.ti.cardreader.provider.bluetooth.feitian.control.FeitianConnector;

public class FeitianCardReaderTest {
    private FeitianCardReader feitianCardReader;
    private BluetoothSocket bluetoothSocket;
    private com.feitian.reader.devicecontrol.Card bluetoochCard;

    @Before
    public void setUpBeforeClass() throws Exception {
        // mock BluetoothDevice
        BluetoothDevice bluetoothDevice = Mockito.mock(BluetoothDevice.class);

        // mock ParcelUuid
        ParcelUuid pUuid = Mockito.mock(ParcelUuid.class);
        UUID uuid = UUID.randomUUID();
        Mockito.when(pUuid.getUuid()).thenReturn(uuid);
        Mockito.when(pUuid.toString()).thenReturn("f716eeb7-6a5a-4d10-bb2f-3fa5f0089f52");
        Assert.assertThat(pUuid.getUuid(), IsNull.notNullValue());
        ParcelUuid[] parcelUuids = new ParcelUuid[] { pUuid };
        Mockito.when(bluetoothDevice.getUuids()).thenReturn(parcelUuids);

        // mock bluetoothSocket
        bluetoothSocket = Mockito.mock(BluetoothSocket.class);
        Mockito.doAnswer((a) -> {
            return null;
        }).when(bluetoothSocket).connect();

        Mockito.when(bluetoothDevice.createInsecureRfcommSocketToServiceRecord(Mockito.any())).thenReturn(bluetoothSocket);

        // create FeitianCardReader
        feitianCardReader = new FeitianCardReader(bluetoothDevice);

        FeitianConnector feitianConnector = (FeitianConnector) Whitebox.getInternalState(feitianCardReader, "connector");
        Awaitility.await().atMost(Duration.ONE_MINUTE).until(() -> !feitianConnector.isActive());

        // (power)mock feitianCard
        FeitianCard feitianCard = Mockito.mock(FeitianCard.class);
        // mock bluetoothCard
        bluetoochCard = Mockito.mock(com.feitian.reader.devicecontrol.Card.class);
        Mockito.when(bluetoochCard.getcardStatus()).thenReturn(DK.CARD_PRESENT);
        Mockito.when(feitianCard.getBluetoothCard()).thenReturn(bluetoochCard);
        Whitebox.setInternalState(feitianCardReader, "feitianCard", feitianCard);
        Assert.assertThat(Whitebox.getInternalState(feitianCardReader, "feitianCard"), IsNull.notNullValue());
        Assert.assertThat(feitianCard.getBluetoothCard(), IsNull.notNullValue());
        //
        Assert.assertThat(feitianCardReader.getCardStatus(), Is.is(DK.CARD_PRESENT));

    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void testConnectSocket() {
        feitianCardReader.connectSocket();
        ExpectedException.none();
    }

    @Test
    public void testGetName() {
        Assert.assertThat(feitianCardReader.getName(), IsNull.nullValue());
        feitianCardReader.setName("dummy");
        Assert.assertThat(feitianCardReader.getName(), Is.is("dummy"));
    }

    @Test
    public void testIsCardPresent() {
        Assert.assertThat(feitianCardReader.isCardPresent(), Is.is(true));
    }

    @Test
    public void testWaitForCardAbsent() throws InterruptedException {
        Assert.assertThat(feitianCardReader.waitForCardAbsent(100), Is.is(false));
        Mockito.when(bluetoochCard.getcardStatus()).thenReturn(DK.CARD_ABSENT);
        Assert.assertThat(feitianCardReader.waitForCardAbsent(5000), Is.is(true));
    }

    @Test
    public void testWaitForCardPresent() {
        Assert.assertThat(feitianCardReader.waitForCardPresent(5000), Is.is(true));
        Mockito.when(bluetoochCard.getcardStatus()).thenReturn(DK.CARD_ABSENT);
        Assert.assertThat(feitianCardReader.waitForCardPresent(100), Is.is(false));
    }

    @Test
    public void testIsPowerOn() {
        Assert.assertThat(feitianCardReader.isPowerOn(), Is.is(false));
    }

}

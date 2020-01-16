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

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.ParcelUuid;

import de.gematik.ti.cardreader.provider.api.ICardReader;
import de.gematik.ti.cardreader.provider.bluetooth.feitian.Whitebox;

public class FeitianCardReaderControllerTest {

    private FeitianCardReaderController feitianCardReaderController;
    private BluetoothAdapter bluetoothAdapter;
    private static final String FEITIAN_UUID = "00001101-0000-1000-8000-00805f9b34fb";

    @Before
    public void setUpBeforeTest() throws Exception {
        feitianCardReaderController = FeitianCardReaderController.getInstance();
        Whitebox.setInternalState(feitianCardReaderController, "cardReaders", new HashMap<>());
        feitianCardReaderController.setContext(new MockContext());
        // mock bluetoothAdapter
        bluetoothAdapter = Mockito.mock(BluetoothAdapter.class);
        Whitebox.setInternalState(feitianCardReaderController, "bluetoothAdapter", bluetoothAdapter);
    }

    @After
    public void tearDownAfterTest() throws Exception {
        Whitebox.setInternalState(feitianCardReaderController, "instance", null);
    }

    @Test
    public void testGetAddRemoveCardReaders() throws IOException {
        Collection<ICardReader> collection = feitianCardReaderController.getCardReaders();
        Assert.assertNotNull(collection);
        Assert.assertEquals(0, collection.size());
        BluetoothDevice bluetoothDevice1 = createBluetoothDevice(1, UUID.randomUUID());
        feitianCardReaderController.actualizeReaderList(bluetoothDevice1);
        Assert.assertEquals(1, collection.size());
        feitianCardReaderController.actualizeReaderList(createBluetoothDevice(2, UUID.randomUUID()));
        Assert.assertEquals(2, collection.size());
        feitianCardReaderController.actualizeReaderList(bluetoothDevice1);
        Assert.assertEquals(2, collection.size());
    }

    @Test
    @Ignore
    public void testReadDevices() throws Exception {
        Set<BluetoothDevice> devices = new HashSet<>();
        devices.add(createBluetoothDevice(1, UUID.fromString(FEITIAN_UUID)));
        devices.add(createBluetoothDevice(2, UUID.fromString(FEITIAN_UUID)));
        devices.add(createBluetoothDevice(3, UUID.randomUUID()));
        Mockito.when(bluetoothAdapter.getBondedDevices()).thenReturn(devices);
        Awaitility.await().atMost(Duration.ONE_MINUTE).until(() -> feitianCardReaderController.getCardReaders().size() == 2);
        Assert.assertEquals(2, feitianCardReaderController.getCardReaders().size());
    }

    private BluetoothDevice createBluetoothDevice(int i, UUID uuid) throws IOException {
        BluetoothDevice bluetoothDevice = Mockito.mock(BluetoothDevice.class);
        Mockito.when(bluetoothDevice.getName()).thenReturn("Name" + i);
        Mockito.when(bluetoothDevice.createInsecureRfcommSocketToServiceRecord(ArgumentMatchers.any())).thenReturn(Mockito.mock(BluetoothSocket.class));
        ParcelUuid parcelUuid = Mockito.mock(ParcelUuid.class);
        Mockito.when(parcelUuid.toString()).thenReturn(uuid.toString());
        Mockito.when(parcelUuid.getUuid()).thenReturn(uuid);
        Mockito.when(bluetoothDevice.getUuids()).thenReturn(new ParcelUuid[] { parcelUuid });
        Assert.assertNotNull(bluetoothDevice.getUuids());
        Assert.assertNotNull(bluetoothDevice.getUuids()[0]);
        Assert.assertNotNull(bluetoothDevice.getUuids()[0].getUuid());
        System.out.println("createBluetoothDevice Done!");
        return bluetoothDevice;
    }

}

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

import java.util.HashMap;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelUuid;

import de.gematik.ti.cardreader.provider.bluetooth.feitian.Whitebox;

public class FeitianBluetoothReceiverTest {

    private static FeitianBluetoothReceiver feitianBluetoothReceiver;
    private static FeitianCardReaderController feitianCardReaderController;
    private BluetoothDevice bluetoothDevice;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        feitianBluetoothReceiver = new FeitianBluetoothReceiver();
        feitianCardReaderController = FeitianCardReaderController.getInstance();
        Whitebox.setInternalState(feitianCardReaderController, "cardReaders", new HashMap<>());
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        Whitebox.setInternalState(feitianCardReaderController, "instance", null);
    }

    @Before
    public void init() {
        // Whitebox.setInternalState(feitianCardReaderController, "instance", null);
        // feitianCardReaderController = FeitianCardReaderController.getInstance();

        bluetoothDevice = Mockito.mock(BluetoothDevice.class);
        Mockito.when(bluetoothDevice.getName()).thenReturn("Junit-TestName");
        // mock bluetootchAdapter
        BluetoothAdapter bluetoothAdapter = Mockito.mock(BluetoothAdapter.class);
        Whitebox.setInternalState(feitianCardReaderController, "bluetoothAdapter", bluetoothAdapter);
        Mockito.when(bluetoothAdapter.isEnabled()).thenReturn(true);
    }

    @Test
    public void testOnReceive() {
        Context context = new MockContext();

        // mock intent
        Intent intent = Mockito.mock(Intent.class);
        Mockito.when(intent.getAction()).thenReturn(BluetoothAdapter.ACTION_STATE_CHANGED);

        feitianBluetoothReceiver.onReceive(context, intent);
        ExpectedException.none();

    }

    @Test
    public void testOnReceiveActionBondStateChanged() {
        Context context = new MockContext();
        Mockito.when(bluetoothDevice.getBondState()).thenReturn(BluetoothDevice.BOND_BONDING);

        // mock intent
        Intent intent = Mockito.mock(Intent.class);
        Mockito.when(intent.getAction()).thenReturn(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        Mockito.when(intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)).thenReturn(bluetoothDevice);

        // mock bluetootchAdapter
        BluetoothAdapter bluetoothAdapter = Mockito.mock(BluetoothAdapter.class);
        Whitebox.setInternalState(feitianCardReaderController, "bluetoothAdapter", bluetoothAdapter);
        Mockito.when(bluetoothAdapter.isEnabled()).thenReturn(true);
        //
        feitianBluetoothReceiver.onReceive(context, intent);
        ExpectedException.none();

        Mockito.when(bluetoothDevice.getBondState()).thenReturn(BluetoothDevice.BOND_NONE);
        feitianBluetoothReceiver.onReceive(context, intent);
        ExpectedException.none();
    }

    @Test
    public void testOnReceiveActionAclConnection() {
        Context context = new MockContext();

        Mockito.when(bluetoothDevice.getBondState()).thenReturn(BluetoothDevice.BOND_BONDING);

        // mock intent
        Intent intent = Mockito.mock(Intent.class);
        Mockito.when(intent.getAction()).thenReturn(BluetoothDevice.ACTION_ACL_CONNECTED);
        Mockito.when(intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)).thenReturn(bluetoothDevice);

        //
        feitianBluetoothReceiver.onReceive(context, intent);
        ExpectedException.none();
        Mockito.when(intent.getAction()).thenReturn(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        feitianBluetoothReceiver.onReceive(context, intent);
        ExpectedException.none();

    }

    @Test
    public void testOnReceiveActionPairingRequest() {
        Context context = new MockContext();

        Mockito.when(bluetoothDevice.getBondState()).thenReturn(BluetoothDevice.BOND_BONDING);
        ParcelUuid[] parcelUuids = { ParcelUuid.fromString("JunitUUID") };
        Mockito.when(bluetoothDevice.getUuids()).thenReturn(parcelUuids);

        // mock intent
        Intent intent = Mockito.mock(Intent.class);
        Mockito.when(intent.getAction()).thenReturn(BluetoothDevice.ACTION_PAIRING_REQUEST);
        Mockito.when(intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)).thenReturn(bluetoothDevice);

        //
        feitianBluetoothReceiver.onReceive(context, intent);
        ExpectedException.none();

    }
}

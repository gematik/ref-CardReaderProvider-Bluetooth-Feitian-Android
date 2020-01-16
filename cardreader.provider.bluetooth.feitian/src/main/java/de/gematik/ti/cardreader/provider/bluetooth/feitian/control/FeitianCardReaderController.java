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

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.IntentFilter;
import android.os.ParcelUuid;
import android.util.ArrayMap;

import de.gematik.ti.cardreader.provider.api.ICardReader;
import de.gematik.ti.cardreader.provider.api.listener.InitializationStatus;
import de.gematik.ti.cardreader.provider.bluetooth.feitian.entities.FeitianCardReader;
import de.gematik.ti.openhealthcard.common.AbstractAndroidCardReaderController;

/**
 * include::{userguide}/BFEICRP_Overview.adoc[tag=FeitianCardReaderController]
 *
 */
@Singleton
public final class FeitianCardReaderController extends AbstractAndroidCardReaderController {

    private static final Logger LOG = LoggerFactory.getLogger(FeitianCardReaderController.class);
    private static final String FEITIAN_DEVICE = "Feitian device: ";
    private static final String FEITIAN_UUID = "00001101-0000-1000-8000-00805f9b34fb";

    private static volatile FeitianCardReaderController instance;
    private final Map<String, ICardReader> cardReaders = new ArrayMap<>();
    private FeitianBluetoothReceiver feitianBluetoothReceiver = null;
    private BluetoothAdapter bluetoothAdapter = null;

    private FeitianCardReaderController() {
    }

    /**
     * Returns an instance of FeitianCardReaderController
     *
     * @return this
     */
    public static FeitianCardReaderController getInstance() {
        if (instance == null) {
            instance = new FeitianCardReaderController();
        }
        return instance;
    }

    /**
     * Returns a list of connected cardReaders
     *
     * @return cardReaders
     */
    @Override
    public Collection<ICardReader> getCardReaders() {
        checkContext();
        init();

        return cardReaders.values();
    }

    private void init() {
        new Thread() {
            @Override
            public void run() {
                if (bluetoothAdapter == null) {
                    initializeBluetoothAdapter();
                } else if (!bluetoothAdapter.isEnabled()) {
                    checkAdapter();
                }

                if (feitianBluetoothReceiver == null) {
                    createReceiver();
                }
                readDevices();

            }
        }.start();
    }

    private void initializeBluetoothAdapter() {
        if (bluetoothAdapter == null) {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        if (bluetoothAdapter == null) {
            LOG.info("Device doesn't support Bluetooth");
        } else {
            LOG.info("Bluetoothadapter: " + bluetoothAdapter.getName() + "created");
            if (!bluetoothAdapter.isEnabled()) {
                LOG.info("Bluetoothadapter is disabled, please enable Bluetooth");
                checkAdapter();

            } else {
                LOG.info("going to read paired Bluetooth devices ...");
            }
        }

    }

    private void createReceiver() {
        checkContext();
        feitianBluetoothReceiver = new FeitianBluetoothReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        this.getContext().registerReceiver(feitianBluetoothReceiver, filter);
    }

    private void readDevices() {
        if (bluetoothAdapter == null) {
            return;
        }
        for (BluetoothDevice pairedDevice : bluetoothAdapter.getBondedDevices()) {
            boolean isFD301 = checkUuids(pairedDevice);
            if (isFD301) {
                actualizeReaderList(pairedDevice);
            }
        }
    }

    private boolean checkUuids(final BluetoothDevice pairedDevice) {
        ParcelUuid[] parcelUuids = pairedDevice.getUuids();
        if (parcelUuids[0].toString().equals(FEITIAN_UUID)) {
            LOG.debug("Feitian device identified - " + "/n + Device-Name: " + pairedDevice.getName());
            return true;
        } else {
            return false;
        }
    }

    private void addNewAndInform(final BluetoothDevice pairedDevice) {
        String pairedDeviceName = pairedDevice.getName();

        FeitianCardReader feitianCardReader = new FeitianCardReader(pairedDevice);
        feitianCardReader.setName(pairedDeviceName);
        feitianCardReader.setDisplayName(getDisplayName(pairedDevice, pairedDeviceName));

        cardReaders.put(pairedDeviceName, feitianCardReader);

        LOG.debug(FEITIAN_DEVICE + feitianCardReader.getName() + " added to readers list");
        informAboutReaderConnection(feitianCardReader, InitializationStatus.INIT_SUCCESS);

    }

    private String getDisplayName(final BluetoothDevice pairedDevice, final String pairedDeviceName) {
        String displayName = pairedDeviceName;
        try {
            Method method = pairedDevice.getClass().getMethod("getAlias");
            if (method != null) {
                displayName = (String) method.invoke(pairedDevice);
            }
        } catch (Exception e) {
            LOG.debug(FEITIAN_DEVICE + pairedDeviceName + " Can't read display name, use device name.", e);
        }
        return displayName;
    }

    void removeAndInform(final BluetoothDevice device) {
        if (cardReaders.keySet().contains(device.getName())) {
            informAboutReaderDisconnection(cardReaders.get(device.getName()));
            cardReaders.keySet().remove(device.getName());
            LOG.debug(FEITIAN_DEVICE + device.getName() + " removed from readers list");
        }
    }

    /**
     * checks if the bluethooth adapter currently is disabled. In this case all readers will be removed from readers list.
     */
    public void checkAdapter() {
        if (!bluetoothAdapter.isEnabled()) {
            LOG.debug("Bluetooth is disabled: clear readers list");

            for (ICardReader cardReader : cardReaders.values()) {
                informAboutReaderDisconnection(cardReader);
                cardReaders.remove(cardReader);
            }
        } else {
            readDevices();
        }
    }

    /**
     * checks if a new device is bonded. In this case a new reader object will be added to readers list.
     */
    public void actualizeReaderList(final BluetoothDevice device) {

        boolean isKnownDevice = false;
        for (String deviceKey : cardReaders.keySet()) {
            if (deviceKey.equals(device.getName())) {
                isKnownDevice = true;

            }
        }
        if (!isKnownDevice) {
            addNewAndInform(device);
        }
    }

    public void actualizeSocketToDevice(final BluetoothDevice device) {
        for (Map.Entry<String, ICardReader> deviceEntry : cardReaders.entrySet()) {
            if (deviceEntry.getKey().equals(device.getName())) {
                informAboutReaderDisconnection(deviceEntry.getValue());
                cardReaders.remove(deviceEntry.getKey());
                addNewAndInform(device);
            }
        }
    }

}

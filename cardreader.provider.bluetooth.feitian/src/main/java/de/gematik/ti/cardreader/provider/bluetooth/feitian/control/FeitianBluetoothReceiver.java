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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * include::{userguide}/BFEICRP_Overview.adoc[tag=FeitianBluetoothReceiver]
 *
 */
public class FeitianBluetoothReceiver extends BroadcastReceiver {

    private static final Logger LOG = LoggerFactory.getLogger(FeitianBluetoothReceiver.class);
    private static final String FEITIAN_DEVICE = "Feitian-Device: ";

    /**
     * handled the received intent actions
     *
     * @param context
     * @param intent
     */
    @Override
    public void onReceive(final Context context, final Intent intent) {
        String action = intent.getAction();
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        switch (action) {
            case BluetoothAdapter.ACTION_STATE_CHANGED:
                LOG.debug("BluetoothAdapter --- ACTION_CHANGED");
                FeitianCardReaderController.getInstance().checkAdapter();
                break;
            case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                handleActionBondStateChanged(device);
                break;
            case BluetoothDevice.ACTION_PAIRING_REQUEST:
                handleActionPairingRequest(device);
                break;
            case BluetoothDevice.ACTION_ACL_CONNECTED:
                handleActionAclConnection(device, " ACTION_ACL_CONNECTED");
                break;
            case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                handleActionAclConnection(device, " ACTION_ACL_DISCONNECTED");
                break;
            default:
                LOG.debug("Action not handled");

        }
    }

    private void handleActionAclConnection(final BluetoothDevice device, final String s) {
        if (device != null) {
            LOG.debug(FEITIAN_DEVICE + device.getName() + s);
            FeitianCardReaderController.getInstance().actualizeSocketToDevice(device);
        }
    }

    private void handleActionPairingRequest(final BluetoothDevice device) {
        if (device != null) {
            LOG.debug(FEITIAN_DEVICE + device.getName() + " ACTION_PAIRING_REQUEST");
            FeitianCardReaderController.getInstance().actualizeReaderList(device);
        }
    }

    private void handleActionBondStateChanged(final BluetoothDevice device) {
        if (device != null) {
            LOG.debug(FEITIAN_DEVICE + device.getName() + " ACTION_BOND_STATE_CHANGED");
            int bondState = device.getBondState();
            // Later delete: it may be a alternative
            // int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
            if (bondState == BluetoothDevice.BOND_NONE) {
                FeitianCardReaderController.getInstance().removeAndInform(device);
            }
        }
    }
}

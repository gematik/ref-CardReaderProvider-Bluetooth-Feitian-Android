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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.feitian.reader.devicecontrol.Card;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.ParcelUuid;

import de.gematik.ti.cardreader.provider.bluetooth.feitian.entities.FeitianCard;

/**
 * include::{userguide}/BFEICRP_Overview.adoc[tag=FeitianConnector]
 *
 */
public class FeitianConnector extends Thread {

    private static final Logger LOG = LoggerFactory.getLogger(FeitianConnector.class);
    private final BluetoothDevice feitianDevice;
    private final IFeitianCardCallBack cardCallBack;
    private BluetoothSocket mSocket;
    private FeitianCard feitianCard;
    private boolean active = true;

    public FeitianConnector(final IFeitianCardCallBack feitianCardCallBack, final BluetoothDevice pairedDevice) {
        this.cardCallBack = feitianCardCallBack;
        this.feitianDevice = pairedDevice;
        mSocket = null;

        if (feitianDevice == null) {
            LOG.error("FeitianConnector: device is null");
        }
        start();
    }

    @Override
    public void run() {
        try {
            ParcelUuid[] parcelUuids = feitianDevice.getUuids();
            if (parcelUuids == null || parcelUuids.length == 0) {
                LOG.debug("No Device UUID found for: " + feitianDevice.getName());
            } else {
                mSocket = feitianDevice.createInsecureRfcommSocketToServiceRecord(parcelUuids[0].getUuid());
                LOG.debug("socket to device: " + feitianDevice.getName() + " is created");
                mSocket.connect();
            }
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        } catch (Exception e2) {
            LOG.error(e2.getMessage(), e2);
            active = false;
            throw e2;
        }

        if (mSocket != null && mSocket.isConnected()) {
            LOG.debug("socket to device: " + feitianDevice.getName() + " is connected");

            try {
                setFeitianCard(new Card(mSocket.getInputStream(), mSocket.getOutputStream()));
                cardCallBack.handleFeitianCard(feitianCard);
            } catch (IOException e) {
                LOG.error("getIn/OutputStream failed" + e);
            } catch (Exception e2) {
                active = false;
                throw e2;
            }
        } else {
            LOG.error("Socket is not connected");
        }
        active = false;
    }

    public FeitianCard getFeitianCard() {
        return this.feitianCard;
    }

    private void setFeitianCard(final Card innerCard) {
        try {
            this.feitianCard = new FeitianCard(innerCard);
        } catch (final Exception e) {
            LOG.error("create FeitianCard failed" + e);
        }
    }

    public void closeSocket() throws IOException {
        mSocket.close();
    }

    public boolean isActive() {
        return active;
    }

    public interface IFeitianCardCallBack {
        void handleFeitianCard(FeitianCard feitianCard);
    }
}

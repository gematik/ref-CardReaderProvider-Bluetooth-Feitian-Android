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

import com.feitian.readerdk.Tool.DK;

import android.bluetooth.BluetoothDevice;

import de.gematik.ti.cardreader.provider.api.ICardReader;
import de.gematik.ti.cardreader.provider.api.card.CardException;
import de.gematik.ti.cardreader.provider.api.card.ICard;
import de.gematik.ti.cardreader.provider.bluetooth.feitian.control.CardStatusHandler;
import de.gematik.ti.cardreader.provider.bluetooth.feitian.control.FeitianConnector;

/**
 * include::{userguide}/BFEICRP_Overview.adoc[tag=FeitianCardReader]
 *
 */
public class FeitianCardReader implements ICardReader {

    public static final int UNKNWON_CARD_STATUS = -1;
    public static final long SLEEP_TIME_MILLIS = 100L;
    private final BluetoothDevice pairedDevice;
    private final CardStatusHandler cardStatusHandler;
    private String name;
    private FeitianCard feitianCard;
    private boolean isPowerOn;
    private FeitianConnector connector;
    private String displayName;

    /**
     * Contructor
     * 
     * @param pairedDevice
     */
    public FeitianCardReader(final BluetoothDevice pairedDevice) {
        this.pairedDevice = pairedDevice;
        cardStatusHandler = new CardStatusHandler(this);
        connectSocket();
    }

    void connectSocket() {
        if (connector == null || !connector.isActive()) {
            connector = new FeitianConnector(FeitianCardReader.this::handleFeitianCard, pairedDevice);
        }
    }

    private void handleFeitianCard(final FeitianCard feitianCard) {
        this.feitianCard = feitianCard;
        if (feitianCard != null) {
            feitianCard.getBluetoothCard().registerCardStatusMonitoring(cardStatusHandler);
            isPowerOn = true;
        } else {
            isPowerOn = false;
        }
        if (isCardPresent()) {
            cardStatusHandler.informAboutCardPresent();
        }
    }

    @Override
    public void initialize() {
        // Nothing
    }

    /**
     * Returns the current initialisation status
     *
     * @return true: if card reader is initialized false: card reader not operational
     */
    @Override
    public boolean isInitialized() {
        return true;
    }

    /**
     * Establishes a connection to the card. If a connection has previously established this method returns a card object with protocol "T=1".
     * 
     * @return FeitianCard
     * @throws CardException
     */
    @Override
    public ICard connect() throws CardException {
        if (feitianCard != null) {
            return feitianCard;

        } else {
            throw new CardException("Could note establish connection to the card");
        }
    }

    /**
     * Returns the unique name of this reader.
     * 
     * @return this.name
     */
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * Setter for the name of this reader
     *
     * @param name
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Returns whether a card is present in this cardReader.
     *
     * @return true if card is present false if card is not present
     */
    @Override
    public boolean isCardPresent() {

        if (feitianCard != null) {
            return feitianCard.getBluetoothCard().getcardStatus() == DK.CARD_PRESENT;
        }
        return false;
    }

    /**
     * Waits until a card is absent in this reader or the timeout expires. If the method returns due to an expired timeout, it returns false. Otherwise it
     * return true.
     *
     * @param timeout
     * @return
     */
    public boolean waitForCardAbsent(final long timeout) {
        long start = System.currentTimeMillis();

        while (isCardPresent()) {
            try {
                Thread.sleep(SLEEP_TIME_MILLIS);
            } catch (InterruptedException e) {
                // Not handled
            }
            if (System.currentTimeMillis() - start > timeout) {
                return false;
            }
        }
        return true;
    }

    /**
     * Waits until a card is present in this reader or the timeout expires. If the method returns due to an expired timeout, it returns false. Otherwise it
     * return true.
     *
     * @param timeout
     * @return
     */
    public boolean waitForCardPresent(final long timeout) {
        long start = System.currentTimeMillis();
        while (!isCardPresent()) {
            try {
                Thread.sleep(SLEEP_TIME_MILLIS);
            } catch (InterruptedException e) {
                // Not handled
            }
            if (System.currentTimeMillis() - start > timeout) {
                return false;
            }
        }
        return true;
    }

    public int getCardStatus() {
        if (feitianCard != null) {
            return feitianCard.getBluetoothCard().getcardStatus();
        }
        return UNKNWON_CARD_STATUS;
    }

    public boolean isPowerOn() {
        return isPowerOn;
    }

    @Override
    public String getDisplayName() {
        return displayName != null ? displayName : getName();
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

}

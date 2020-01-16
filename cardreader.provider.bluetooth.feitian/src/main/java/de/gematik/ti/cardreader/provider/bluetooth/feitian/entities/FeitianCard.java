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

import de.gematik.ti.cardreader.provider.api.card.Atr;
import de.gematik.ti.cardreader.provider.api.card.CardException;
import de.gematik.ti.cardreader.provider.api.card.CardProtocol;
import de.gematik.ti.cardreader.provider.api.card.ICard;
import de.gematik.ti.cardreader.provider.api.card.ICardChannel;
import de.gematik.ti.cardreader.provider.api.command.CommandApdu;
import de.gematik.ti.cardreader.provider.api.command.ICommandApdu;
import de.gematik.ti.cardreader.provider.api.command.IResponseApdu;
import de.gematik.ti.cardreader.provider.api.command.ResponseApdu;

/**
 * include::{userguide}/BFEICRP_Overview.adoc[tag=FeitianCard]
 *
 */
public class FeitianCard implements ICard {

    private static final ICommandApdu MANAGE_CHANNEL_COMMAND_OPEN = new CommandApdu(0x00, 0x70, 0x00, 0x00, 0x01);
    private static final int RESPONSE_SUCCESS = 0x9000;
    private static final int RESPONSE_BUFFER = 65536;
    private final com.feitian.reader.devicecontrol.Card bluetoothCard;
    private final byte[] feitianCardAtr;

    private final FeitianCardChannel basicChannel;
    private Thread exclusiveThread;

    /**
     * Consructs a new card object
     * 
     * @param bluetoothCard
     * @throws CardException
     */
    public FeitianCard(final com.feitian.reader.devicecontrol.Card bluetoothCard) {
        this.bluetoothCard = bluetoothCard;
        feitianCardAtr = bluetoothCard.getAtr();
        basicChannel = new FeitianCardChannel(this);
        this.exclusiveThread = null;
    }

    /**
     * Returns the ATR of this card
     * 
     * @return the ATR of this card.
     */
    @Override
    public Atr getATR() {
        checkCardOpen();
        return new Atr(feitianCardAtr);
    }

    /**
     * @return the protocol in use for this card, for example "T=0" or "T=1".
     */
    @Override
    public CardProtocol getProtocol() {
        checkCardOpen();
        String protocol = String.valueOf(bluetoothCard.getProtocol());
        CardProtocol cardProtocol = null;
        switch (protocol) {
            case "0":
                cardProtocol = CardProtocol.T0;
                break;
            case "1":
                cardProtocol = CardProtocol.T1;
                break;
            default:
                cardProtocol = CardProtocol.T15;

        }
        return cardProtocol;
    }

    @Override
    public ICardChannel openBasicChannel() throws CardException {
        return getBasicChannel();
    }

    /**
     * Returns the CardChannel for the basic logical channel. The basic logical channel has a channel number of 0.
     * 
     * @return the CardChannel for the basic logical channel. The basic logical channel has a channel number of 0.
     */
    public ICardChannel getBasicChannel() {
        checkCardOpen();
        return basicChannel;
    }

    /**
     * Opens a new logical channel to the card and returns it. The channel is opened by issuing a 'MANAGE CHANNEL' command that should use the format '[00 70 00
     * 00 01]'.
     * 
     * @return Opens a new logical channel to the card and returns it. The channel is opened by issuing a 'MANAGE CHANNEL' command that should use the format
     *         '[00 70 00 00 01]'.
     * @throws CardException
     */
    @Override
    public ICardChannel openLogicalChannel() throws CardException {
        checkCardOpen();
        checkExclusive();

        IResponseApdu response = transceive(MANAGE_CHANNEL_COMMAND_OPEN);
        if (response.getSW() != RESPONSE_SUCCESS) {
            throw new CardException("openLogicalChannel failed, response code: " + String.format("0x%04x", response.getSW()));
        }
        int channelNumber = response.getData()[0];
        return new FeitianCardChannel(this, channelNumber);
    }

    IResponseApdu transceive(final ICommandApdu commandAPDU) throws CardException {

        IResponseApdu responseAPDU;

        byte[] cmd = commandAPDU.getBytes();
        int[] responseLength = new int[2];
        byte[] responseBuffer = new byte[RESPONSE_BUFFER];

        int ret = bluetoothCard.transApdu(cmd.length, cmd, responseLength, responseBuffer);
        if (ret == DK.RETURN_SUCCESS) {
            byte[] response = new byte[responseLength[0]];
            System.arraycopy(responseBuffer, 0, response, 0, responseLength[0]);
            responseAPDU = new ResponseApdu(response);
        } else if (ret == DK.BUFFER_NOT_ENOUGH) {
            throw new CardException("transceive: receive buffer not enough");
        } else if (ret == DK.CARD_ABSENT) {
            throw new CardException("transceive: Card is not connected (anymore)");
        } else {
            throw new CardException("transceive: trans apdu error: " + ret);
        }
        return responseAPDU;
    }

    /**
     * Requests exclusive access to this card. Once a thread has invoked 'beginExclusive', only this thread is allowed to communicate with this card until it
     * calls 'endExclusive'. Other threads attempting communication will receive a CardException. Applications have to ensure that exclusive access is correctly
     * released. This can be achieved by executing the 'beginExclusive()' and 'endExclusive' calls in a 'try ... finally' block.
     */
    public void beginExclusive() {
        checkCardOpen();
        exclusiveThread = Thread.currentThread();
    }

    /**
     * Releases the exclusive access previously established using 'beginExclusive'.
     * 
     * @throws CardException
     */
    public void endExclusive() throws CardException {
        if (exclusiveThread == Thread.currentThread()) {
            exclusiveThread = null;
        } else {
            throw new CardException("This thread " + Thread.currentThread().getName() + " has no exclusive access and thus cannot terminate exclusive access");
        }
    }

    /**
     * Disconnects the connection with this card.
     * 
     * @param reset
     * @throws CardException
     */
    @Override
    public void disconnect(final boolean reset) throws CardException {
        if (bluetoothCard.PowerOn() == DK.RETURN_SUCCESS) {
            checkExclusive();
            bluetoothCard.PowerOff();
            exclusiveThread = null;
        }
    }

    void checkExclusive() throws CardException {
        if (exclusiveThread == null) {
            return;
        }
        if (exclusiveThread != Thread.currentThread()) {
            throw new CardException("Another thread than this thread " + Thread.currentThread().getName() + " has exclusive access");
        }
    }

    /**
     * Returns this card object
     * 
     * @return this card object
     */
    public com.feitian.reader.devicecontrol.Card getBluetoothCard() {
        return this.bluetoothCard;
    }

    void checkCardOpen() {
        if (bluetoothCard.PowerOn() != DK.RETURN_SUCCESS) {
            throw new IllegalStateException("card is not connected");
        }
    }
}

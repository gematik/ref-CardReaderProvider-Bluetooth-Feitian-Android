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

import com.feitian.readerdk.Tool.DK;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import de.gematik.ti.cardreader.provider.api.CardEventTransmitter;
import de.gematik.ti.cardreader.provider.api.ICardReader;

/**
 * include::{userguide}/BFEICRP_Overview.adoc[tag=CardStatusHandler]
 *
 */
public class CardStatusHandler extends Handler {

    private static final Logger LOG = LoggerFactory.getLogger(CardStatusHandler.class);
    private final CardEventTransmitter cardEventTransmitter;

    /**
     * Create a card reader specific Object to handleMessages from bluetooth card reader devices. Compute and transmit CardEvent. This Handler compute only
     * DK.CARD_ABSENT, DK.CARD_UNKNOWN, DK.CARD_PRESENT, DK.CARD_TIMEOUT Messages
     *
     * @param cardReader
     */
    public CardStatusHandler(final ICardReader cardReader) {
        super(Looper.getMainLooper());
        this.cardEventTransmitter = FeitianCardReaderController.getInstance().createCardEventTransmitter(cardReader);
    }

    /**
     * Compute only DK.CARD_ABSENT, DK.CARD_UNKNOWN, DK.CARD_PRESENT, DK.CARD_TIMEOUT Messages and transmit EventBus Events
     *
     * @param msg
     */
    @Override
    public void handleMessage(final Message msg) {
        switch (msg.what) {
            case DK.CARD_STATUS:
                handleDkCardStatus(msg);
                break;
            default:
                LOG.debug("Unhandled Feitian-Device Message: \"" + msg + "\"");
                break;
        }
    }

    private void handleDkCardStatus(final Message msg) {
        switch (msg.arg1) {
            case DK.CARD_ABSENT:
                cardEventTransmitter.informAboutCardAbsent();
                break;
            case DK.CARD_PRESENT:
                informAboutCardPresent();
                break;
            case DK.CARD_UNKNOWN:
                cardEventTransmitter.informAboutCardUnknown();
                break;
            case DK.CARD_TIMEOUT:
                cardEventTransmitter.informAboutCardTimeout();
                break;
            default:
                LOG.debug("Unhandled Feitian-Device DK.CARD_STATUS: \"" + msg.arg1 + "\"");
                break;
        }
    }

    /**
     * Send a Message to inform about card present
     */
    public void informAboutCardPresent() {
        cardEventTransmitter.informAboutCardPresent();
    }

}

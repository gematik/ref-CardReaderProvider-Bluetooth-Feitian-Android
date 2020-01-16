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

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.hamcrest.core.Is;
import org.hamcrest.core.IsNull;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.feitian.readerdk.Tool.DK;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Message;
import android.os.ParcelUuid;
import de.gematik.ti.cardreader.provider.api.ICardReader;
import de.gematik.ti.cardreader.provider.api.events.card.CardAbsentEvent;
import de.gematik.ti.cardreader.provider.api.events.card.CardPresentEvent;
import de.gematik.ti.cardreader.provider.bluetooth.feitian.entities.FeitianCardReader;

public class CardStatusHandlerTest {
    private static final Logger LOG = LoggerFactory.getLogger(CardStatusHandlerTest.class);
    private static ICardReader feitianCardReader;
    private static CardStatusHandler cardStatusHandler;
    private static BluetoothSocket bluetoothSocket;
    private static ConnectionListener listener1;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
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

        feitianCardReader = new FeitianCardReader(bluetoothDevice);
        cardStatusHandler = new CardStatusHandler(feitianCardReader);

        //
        listener1 = new ConnectionListener();
        EventBus.getDefault().register(listener1);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void testHandleMessage() {
        Assert.assertThat(listener1.isCardPresent(), Is.is(false));
        Message msg = new Message();
        msg.what = DK.CARD_STATUS;
        msg.arg1 = DK.CARD_PRESENT;
        cardStatusHandler.handleMessage(msg);
        //
        org.awaitility.Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> listener1.isCardPresent());
    }

    // Implementation is same as testHandleMessage()
    @Test
    public void testInformAboutCardPresent() {
        // done
    }

    public static class ConnectionListener {

        private int cards = 0;

        @Subscribe
        public void cardPresent(final CardPresentEvent cardPresentEvent) {
            LOG.debug("cardPresent " + cardPresentEvent.getCardReader());
            cards += 1;
        }

        @Subscribe
        public void cardAbsent(final CardAbsentEvent cardAbsentEvent) {
            LOG.debug("cardAbsent " + cardAbsentEvent.getCardReader());
            cards -= 1;
        }

        public int getCards() {
            return cards;
        }

        public Boolean isCardPresent() {
            return cards > 0;
        }
    }

}

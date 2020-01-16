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

import org.hamcrest.core.Is;
import org.hamcrest.core.IsEqual;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import com.feitian.readerdk.Tool.DK;

import de.gematik.ti.cardreader.provider.api.card.CardException;
import de.gematik.ti.cardreader.provider.api.card.CardProtocol;
import de.gematik.ti.cardreader.provider.api.card.ICardChannel;
import de.gematik.ti.cardreader.provider.api.command.IResponseApdu;
import de.gematik.ti.cardreader.provider.api.command.ResponseApdu;

/**
 */
public class FeitianCardTest {
    private static FeitianCard feitianCard;
    private static com.feitian.reader.devicecontrol.Card bluetoothCard;

    @BeforeClass
    public static void setUpBeforeClass() {
        bluetoothCard = Mockito.mock(com.feitian.reader.devicecontrol.Card.class);
        Mockito.when(bluetoothCard.getProtocol()).thenReturn(0);
        Mockito.when(bluetoothCard.PowerOn()).thenReturn(DK.RETURN_SUCCESS);
        Mockito.when(bluetoothCard.getAtr()).thenReturn(new byte[] { (byte) 0x3B, (byte) 0xDD, (byte) 0x00, (byte) 0xFF, (byte) 0x81, (byte) 0x50, (byte) 0xFE,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 });
        feitianCard = new FeitianCard(bluetoothCard);
    }

    @AfterClass
    public static void tearDownAfterClass() {
    }

    @Test
    public void testGetATR() {
        Assert.assertThat(feitianCard.getATR().getBytes(), IsEqual.equalTo(new byte[] { (byte) 0x3B, (byte) 0xDD, (byte) 0x00, (byte) 0xFF, (byte) 0x81,
                (byte) 0x50, (byte) 0xFE, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 }));
    }

    @Test
    public void testGetProtocol() {
        Assert.assertThat(feitianCard.getProtocol(), Is.is(CardProtocol.T0));
    }

    @Test
    public void testGetBasicChannel() {
        Assert.assertThat(feitianCard.getBasicChannel().getChannelNumber(), Is.is(0));
    }

    @Test
    public void testOpenLogicalChannel() {

        byte[] cmd = { 0x00, 0x70, 0x00, 0x00, 0x01 };
        int[] responseLength = new int[] { 3, 0 };
        byte[] responseBuffer = de.gematik.ti.utils.codec.Hex.decode("019000");
        Mockito.when(bluetoothCard.transApdu(cmd.length, cmd, responseLength, responseBuffer)).thenReturn(com.feitian.readerdk.Tool.DK.RETURN_SUCCESS);

        byte[] response = new byte[responseLength[0]];
        System.arraycopy(responseBuffer, 0, response, 0, responseLength[0]);
        IResponseApdu responseApdu = new ResponseApdu(response);

        Assert.assertThat(responseApdu.getSW(), IsEqual.equalTo(0x9000));

        int channelNumber = responseApdu.getData()[0];
        ICardChannel channel = new FeitianCardChannel(feitianCard, channelNumber);

        Assert.assertThat(channel.getChannelNumber(), Is.is(1));
        int maxResponseLength = channel.getMaxResponseLength();
        Assert.assertThat(maxResponseLength, Is.is(256));
        int maxMessageLength = channel.getMaxMessageLength();
        Assert.assertThat(maxMessageLength, Is.is(256));

    }

    @Test
    public void testDisconnect() {
        try {
            feitianCard.disconnect(false);
            ExpectedException.none();
        } catch (CardException e) {
            Assert.fail(e.toString());
        }
    }

    @Test
    public void testGetBluetoothCard() {
        Assert.assertThat(feitianCard.getBluetoothCard(), Is.is(bluetoothCard));
    }

    @Test
    public void testCheckCardOpen() {
        feitianCard.checkCardOpen();
        Assert.assertThat(bluetoothCard.PowerOn(), Is.is(DK.RETURN_SUCCESS));
    }
}

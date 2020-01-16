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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.feitian.reader.devicecontrol.Card;

import de.gematik.ti.cardreader.provider.api.command.CommandApdu;
import de.gematik.ti.cardreader.provider.api.command.ICommandApdu;
import de.gematik.ti.cardreader.provider.api.command.IResponseApdu;
import de.gematik.ti.cardreader.provider.api.command.ResponseApdu;

/**
 */
public class FeitianCardChannelTest {
    private static final Logger LOG = LoggerFactory.getLogger(FeitianCardChannelTest.class);
    private static Card bluetoothCard;
    private static FeitianCard feitianCard;
    private static FeitianCardChannel feitianCardChannel;
    @Rule
    public TestName testName = new TestName();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        bluetoothCard = Mockito.mock(Card.class);
        feitianCard = new FeitianCard(bluetoothCard);
        feitianCardChannel = new FeitianCardChannel(feitianCard);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void testGetCard() {
        Assert.assertThat(feitianCardChannel.getCard(), Is.is(feitianCard));
    }

    @Test
    public void testGetChannelNumber() {
        Assert.assertThat(feitianCardChannel.getChannelNumber(), Is.is(0));
    }

    @Test
    public void testTransmitCommandAPDU() {
        ICommandApdu command = new CommandApdu(0x00, 0x70, 0x00, 0x00, 0x01);
        byte[] cmd = { 0x00, 0x70, 0x00, 0x00, 0x01 };
        int[] responseLength = new int[] { 3, 0 };
        byte[] responseBuffer = de.gematik.ti.utils.codec.Hex.decode("019000");
        Mockito.when(bluetoothCard.transApdu(cmd.length, cmd, responseLength, responseBuffer)).thenReturn(com.feitian.readerdk.Tool.DK.RETURN_SUCCESS);

        byte[] response = new byte[responseLength[0]];
        System.arraycopy(responseBuffer, 0, response, 0, responseLength[0]);
        IResponseApdu responseApdu = new ResponseApdu(response);

        Assert.assertThat(responseApdu.getSW(), IsEqual.equalTo(0x9000));
    }

    @Test
    public void shouldSucceedExtendedLengthSupported() {
        LOG.info("------ Start " + testName.getMethodName());
        FeitianCard cardMock = Mockito.mock(FeitianCard.class);
        FeitianCardChannel feitianCardChannelLocal = new FeitianCardChannel(cardMock, 0);
        boolean extendedLengthSupported = feitianCardChannelLocal.isExtendedLengthSupported();
        Assert.assertThat(extendedLengthSupported, Is.is(true));
    }

    @Test
    public void testGetMaxMessageLength() {
        LOG.info("------ Start " + testName.getMethodName());
        FeitianCard cardMock = Mockito.mock(FeitianCard.class);
        FeitianCardChannel feitianCardChannelLocal = new FeitianCardChannel(cardMock, 0);
        int maxMessageLength = feitianCardChannelLocal.getMaxMessageLength();
        Assert.assertThat(maxMessageLength, Is.is(256));
    }

    @Test
    public void testGetMaxResponseLength() {
        LOG.info("------ Start " + testName.getMethodName());
        FeitianCard cardMock = Mockito.mock(FeitianCard.class);
        FeitianCardChannel feitianCardChannelLocal = new FeitianCardChannel(cardMock, 0);
        int maxResponseLength = feitianCardChannelLocal.getMaxResponseLength();
        Assert.assertThat(maxResponseLength, Is.is(256));
    }
}

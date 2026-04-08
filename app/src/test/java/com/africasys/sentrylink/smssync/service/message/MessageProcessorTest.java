package com.africasys.sentrylink.smssync.service.message;

import com.africasys.sentrylink.smssync.dtos.SMSDecryptedDTO;
import com.africasys.sentrylink.smssync.enums.MessageType;
import com.africasys.sentrylink.smssync.utils.MessageHelpers;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MessageProcessorTest {

    private MessageProcessor processor;

    @Before
    public void setUp() {
        processor = MessageProcessor.getInstance();
    }

    @After
    public void tearDown() {
        // unregister any test handlers to avoid side effects
        processor.unregisterHandler(MessageType.MSG);
    }

    @Test
    public void testProcessDispatchesToRegisteredHandler() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> received = new AtomicReference<>();

        MessageHandler testHandler = (context, sender, dto, receivedTimestamp, prefix) -> {
            received.set(sender + ":" + dto.getMessage());
            latch.countDown();
        };

        processor.registerHandler(MessageType.MSG, testHandler);

        String plaintext = MessageHelpers.formatMessage(1L, MessageType.MSG, "hello-test");
        SMSDecryptedDTO dto = new SMSDecryptedDTO(plaintext);

        processor.process(null, "+1000", dto, System.currentTimeMillis(), "SL0");

        boolean ok = latch.await(3, TimeUnit.SECONDS);
        assertTrue("Handler was not invoked in time", ok);
        assertEquals("+1000:hello-test", received.get());
    }
}


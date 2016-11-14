/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.config;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.channel.PriorityChannel;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.PriorityCapableChannelMessageStore;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Dave Syer
 * @author Artem Bilan
 */
@RunWith(SpringRunner.class)
@DirtiesContext
public class ChannelWithMessageStoreParserTests {

	private static final String BASE_PACKAGE = "org.springframework.integration";

	@Autowired
	@Qualifier("input")
	private MessageChannel input;

	@Autowired
	@Qualifier("output")
	private PollableChannel output;

	@Autowired
	private TestHandler handler;

	@Autowired
	@Qualifier("messageStore")
	private MessageGroupStore messageGroupStore;

	@Autowired
	@Qualifier("priority")
	private PollableChannel priorityChannel;

	@Autowired
	@Qualifier("priorityMessageStore")
	private MessageGroupStore priorityMessageStore;

	@Test
	@DirtiesContext
	public void testActivatorSendsToPersistentQueue() throws Exception {

		input.send(createMessage("123", "id1", 3, 1, null));
		handler.getLatch().await(100, TimeUnit.MILLISECONDS);
		assertEquals("The message payload is not correct", "123", handler.getMessageString());
		// The group id for buffered messages is the channel name
		assertEquals(1, messageGroupStore.getMessageGroup("messageStore:output").size());

		Message<?> result = output.receive(100);
		assertEquals("hello", result.getPayload());
		assertEquals(0, messageGroupStore.getMessageGroup(BASE_PACKAGE + ".store:output").size());

	}

	@Test
	@DirtiesContext
	public void testPriorityMessageStore() {
		assertSame(this.priorityMessageStore, TestUtils.getPropertyValue(this.priorityChannel, "queue.messageGroupStore"));
		assertThat(this.priorityChannel, instanceOf(PriorityChannel.class));
	}

	private static <T> Message<T> createMessage(T payload, Object correlationId, int sequenceSize, int sequenceNumber,
			MessageChannel outputChannel) {
		return MessageBuilder.withPayload(payload).setCorrelationId(correlationId).setSequenceSize(sequenceSize)
				.setSequenceNumber(sequenceNumber).setReplyChannel(outputChannel).build();
	}

	public static class DummyPriorityMS extends SimpleMessageStore implements PriorityCapableChannelMessageStore {

		@Override
		public boolean isPriorityEnabled() {
			return true;
		}

	}

}
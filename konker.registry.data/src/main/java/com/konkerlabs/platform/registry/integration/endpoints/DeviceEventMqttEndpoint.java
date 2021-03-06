package com.konkerlabs.platform.registry.integration.endpoints;

import com.konkerlabs.platform.registry.business.exceptions.BusinessException;
import com.konkerlabs.platform.registry.integration.processors.DeviceEventProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

@MessageEndpoint
public class DeviceEventMqttEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceEventMqttEndpoint.class);

    private DeviceEventProcessor deviceEventProcessor;

    @Autowired
    public DeviceEventMqttEndpoint(DeviceEventProcessor deviceEventProcessor) {
        this.deviceEventProcessor = deviceEventProcessor;
    }

    @ServiceActivator(inputChannel = "konkerMqttInputChannel")
    public void onEvent(Message<String> message) throws MessagingException {

        if (LOGGER.isDebugEnabled())
            LOGGER.debug("A message has arrived -> " + message.toString());

        Object topic = message.getHeaders().get(MqttHeaders.TOPIC);
        if (topic == null || topic.toString().isEmpty()) {
            throw new MessagingException(message,"Topic cannot be null or empty");
        }

        try {
            deviceEventProcessor.process(extractFromResource(topic.toString(),1),
                    extractFromResource(topic.toString(),2),
                    message.getPayload());
        } catch (BusinessException be) {
            LOGGER.error(message.getPayload(),be);
        }
    }

    private String extractFromResource(String channel, int index) {
        try {
            return channel.split("/")[index];
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }
}

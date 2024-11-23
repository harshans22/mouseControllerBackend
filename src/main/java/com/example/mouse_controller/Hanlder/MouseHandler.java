package com.example.mouse_controller.Hanlder;

import java.awt.*;
import java.awt.event.InputEvent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class MouseHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(MouseHandler.class);
    private Robot robot;
    private final ObjectMapper objectMapper; // ObjectMapper is now injected by Spring
    private final Dimension screenSize; // To store the screen dimensions

    @Autowired
    public MouseHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;  // ObjectMapper is now injected by Spring
        screenSize = Toolkit.getDefaultToolkit().getScreenSize(); // Get screen dimensions

        try {
            if (!GraphicsEnvironment.isHeadless()) {
                robot = new Robot();
            } else {
                log.error("Headless environment detected. Robot functionality is unavailable.");
            }
        } catch (AWTException ignored) {
            log.error(ignored.getMessage());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        try {
            JsonNode jsonNode = objectMapper.readTree(payload);

            if (jsonNode.has("leftClickEvent") && jsonNode.get("leftClickEvent").asBoolean()) {
                robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                log.info("Left click event triggered.");
                return;
            }

            if (jsonNode.has("rightClickEvent") && jsonNode.get("rightClickEvent").asBoolean()) {
                robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
                robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
                log.info("Right click event triggered.");
                return;
            }

            if (jsonNode.has("TapEvent") && jsonNode.get("TapEvent").asBoolean()) {
                robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                return;
            }

            if (jsonNode.has("scroll")) {
                double scrollAmount = jsonNode.get("scroll").asDouble();
                log.info("scroll amount {}", scrollAmount);
                robot.mouseWheel((int) (scrollAmount * 2));
                return;
            }

            double x = jsonNode.has("x") ? jsonNode.get("x").asDouble() : 0;
            double y = jsonNode.has("y") ? jsonNode.get("y").asDouble() : 0;

            double scalingFactor = jsonNode.has("changeSensitivityEvent") ? jsonNode.get("changeSensitivityEvent").asDouble() : 0.5;

            Point currentMousePosition = MouseInfo.getPointerInfo().getLocation();

            int newX = (int) (currentMousePosition.getX() + x * scalingFactor * 50);
            int newY = (int) (currentMousePosition.getY() + y * scalingFactor * 50);

            // Constrain the mouse movement to the screen dimensions
//            newX = Math.max(0, Math.min(screenSize.width - 1, newX));
//            newY = Math.max(0, Math.min(screenSize.height - 1, newY));

            robot.mouseMove(newX, newY);
            log.info("Cursor moved to: {}, {}", newX, newY);

        } catch (Exception e) {
            log.error("Error handling message: ", e);
        }
    }
}

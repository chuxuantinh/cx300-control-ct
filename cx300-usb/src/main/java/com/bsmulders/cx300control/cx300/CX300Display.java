package com.bsmulders.cx300control.cx300;

import java.util.Iterator;

import com.bsmulders.cx300control.hid.HidService;
import com.google.common.base.Splitter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CX300Display {

    private static final byte INITIALISE_GROUP = 0x13;
    private static final byte POSITION_GROUP = 0x14;
    private static final byte TEXT_GROUP = 0x15;

    private static final byte TEXT_MESSAGE = (byte) 0x00;
    private static final byte TEXT_MESSAGE_END = (byte) 0x80;
    private static final byte POSITION_END = (byte) 0x80;

    private static final byte INITIALISE_CLEAR = 0x00;
    private static final byte INITIALISE_CORNERS = 0x0d;
    private static final byte INITIALISE_DOUBLE = 0x15;

    private static final byte POSITION_TOP_LEFT = 0x01;
    private static final byte POSITION_BOTTOM_LEFT = 0x02;
    private static final byte POSITION_TOP_RIGHT = 0x03;
    private static final byte POSITION_BOTTOM_RIGHT = 0x04;
    private static final byte POSITION_TOP = 0x05;
    private static final byte POSITION_BOTTOM = 0x0a;

    private final HidService hidService;

    @Autowired
    public CX300Display(HidService hidService) {
        this.hidService = hidService;
    }

    void setText(String top, String bottom) {
        if (top.length() > 28 || bottom.length() > 28) {
            throw new IllegalArgumentException();
        }

        initialise(INITIALISE_DOUBLE);
        setPosition(POSITION_TOP);
        writeText(top);
        setPosition(POSITION_BOTTOM);
        writeText(bottom);
    }

    void setText(String topLeft, String bottomLeft, String topRight, String bottomRight) {
        if (topLeft.length() > 16 || bottomLeft.length() > 16 || topRight.length() > 16 || bottomRight.length() > 16) {
            throw new IllegalArgumentException();
        }

        initialise(INITIALISE_CORNERS);
        setPosition(POSITION_TOP_LEFT);
        writeText(topLeft);
        setPosition(POSITION_BOTTOM_LEFT);
        writeText(bottomLeft);
        setPosition(POSITION_TOP_RIGHT);
        writeText(topRight);
        setPosition(POSITION_BOTTOM_RIGHT);
        writeText(bottomRight);
    }

    void clear() {
        hidService.sendMessage(new byte[]{INITIALISE_GROUP, INITIALISE_CLEAR});
    }

    private void initialise(byte initialise) {
        hidService.sendMessage(new byte[]{INITIALISE_GROUP, initialise});
    }

    private void setPosition(byte position) {
        hidService.sendMessage(new byte[]{POSITION_GROUP, position, POSITION_END});
    }

    private void writeText(String text) {
        for (Iterator<String> parts = Splitter.fixedLength(8)
                                              .split(text)
                                              .iterator(); parts.hasNext(); ) {
            String part = parts.next();
            byte type = parts.hasNext() ? TEXT_MESSAGE : TEXT_MESSAGE_END;
            hidService.sendMessage(createMessage(part, type));
        }
    }

    private byte[] createMessage(String input, byte type) {
        byte[] template = new byte[18];
        template[0] = TEXT_GROUP;
        template[1] = type;
        fillTemplate(template, input);

        return template;
    }

    private void fillTemplate(byte[] template, String input) {
        if (input.length() <= 8) {
            byte[] inputBytes = input.getBytes();
            byte[] spacedBytes = new byte[inputBytes.length * 2];
            for (int i = 0; i < inputBytes.length; i++) {
                spacedBytes[i * 2] = inputBytes[i];
                spacedBytes[(i * 2) + 1] = 0x00;
            }

            System.arraycopy(spacedBytes, 0, template, 2, spacedBytes.length);
        }
    }
}

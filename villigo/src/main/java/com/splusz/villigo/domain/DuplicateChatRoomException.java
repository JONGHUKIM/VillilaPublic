package com.splusz.villigo.domain;

public class DuplicateChatRoomException extends RuntimeException {
    public DuplicateChatRoomException(String message) {
        super(message);
    }
}

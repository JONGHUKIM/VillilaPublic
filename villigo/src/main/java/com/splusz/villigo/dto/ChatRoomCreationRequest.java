package com.splusz.villigo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatRoomCreationRequest {
    private Long userId1;
    private Long userId2;
}
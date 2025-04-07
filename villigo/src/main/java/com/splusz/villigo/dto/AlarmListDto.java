package com.splusz.villigo.dto;

import java.time.LocalDateTime;

import com.splusz.villigo.domain.Alarm;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class AlarmListDto {
	private Long id;
	private Long alarmCategoryId;
	String content;
	LocalDateTime createdTime;
	boolean status;
	
	public AlarmListDto(Alarm alarm) {
		this.id = alarm.getId();
		this.alarmCategoryId = alarm.getAlarmCategory().getId();
		this.content = alarm.getContent();
		this.createdTime = alarm.getCreatedTime();
		this.status = alarm.getStatus();
	}
}

package com.splusz.villigo.domain;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "alarms")
@Builder
@Getter 
@Setter
@ToString(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class Alarm extends BaseTimeEntity {
	
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alarm_category_id")
    @ToString.Exclude
    @Basic(optional = false)
    private AlarmCategory alarmCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    @ToString.Exclude
    private Reservation reservation;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id")
    @ToString.Exclude
    private User receiver;
    
    @Basic(optional = false)
    private String content;
    
    @Basic(optional = false)
    private Boolean status;  // 알람 상태(읽음: true, 안 읽음: false)
    
}

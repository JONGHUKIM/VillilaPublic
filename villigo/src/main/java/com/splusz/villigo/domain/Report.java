package com.splusz.villigo.domain;

import jakarta.persistence.Id;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "reports")
@Getter @Setter
@NoArgsConstructor
public class Report extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

	@ToString.Exclude @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reporter_id") @Basic(optional = false)
    private User reporter; // 신고한 유저

	@ToString.Exclude @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reported_id") @Basic(optional = false)
    private User reportedUser; // 신고 당한 유저

	@Basic(optional = false)
    private String detail;
	
	@Basic(optional = false)
    private Long itemId; // 좋아요 대상 아이템 ID (BAG, CAR 등)
	
	@ToString.Exclude @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "rental_category_id") @Basic(optional = false)
    private RentalCategory rentalCategory; // 좋아요 대상의 유형 (예: "BAG", "CAR")
    
}

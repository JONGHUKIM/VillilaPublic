package com.splusz.villigo.domain;

import java.time.LocalDateTime;

import com.splusz.villigo.dto.ReservationCreateDto;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
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
@Table(name = "reservations")
@Getter
@Setter
@Builder
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class Reservation extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ToString.Exclude
//	@Basic(optional = false)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_category_id")
    private RentalCategory retalCategory; // 예약한 상품 카테고리 (BAG, CAR 등)
    
    @ToString.Exclude
	@Basic(optional = false)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product; // 예약한 상품 ID
	
    @Basic(optional = false)
    @Column(insertable = false)
    private Integer status;

	@Basic(optional = false)
    private LocalDateTime startTime;
	
	@Basic(optional = false)
    private LocalDateTime endTime;

	@ToString.Exclude
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "renter_id")
	@Basic(optional = false)
    private User renter;
    
	// 겹침 여부 확인 메서드
	public boolean isOverlapping(ReservationCreateDto dto) {
		return !(this.endTime.isBefore(dto.getStartTime()) || this.startTime.isAfter(dto.getEndTime()));
	}

}

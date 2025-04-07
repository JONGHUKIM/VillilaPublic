package com.splusz.villigo.domain;

import com.splusz.villigo.dto.CarUpdateDto;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "cars")
@Getter @Setter
@Builder
@ToString(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class Car {
	
    @Id
    private Long id;
    
    @ToString.Exclude @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "product_id")
    @MapsId
    private Product product;

    @Basic(optional = false)
    private int old;

    @Basic(optional = false)
    private Boolean drive;
    
    @Column(name = "min_rental_time")
    private int minRentalTime;

    public Car update(CarUpdateDto dto) {
        this.drive = dto.getDrive();
        this.minRentalTime = dto.getMinRentalTime();
        return this;
    }
}

package com.splusz.villigo.domain;

import java.time.LocalDateTime;

import com.splusz.villigo.dto.BagUpdateDto;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "bags")
@Getter @Setter
@Builder
@ToString(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class Bag {
	
    @Id
    private Long id;
    
    @ToString.Exclude @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "product_id")
    @MapsId
    private Product product;

}

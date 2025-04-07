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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "rental_images")
@Getter @Setter
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class RentalImage extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "product_id") @Basic(optional = false)
    private Product product;
	
	@Basic(optional = false)
    private String filePath; // 이미지 파일 경로+
    
}

package com.splusz.villigo.domain;

import org.hibernate.annotations.NaturalId;

import com.splusz.villigo.dto.SelectBrandsByCategoryDto;

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
@Table(name = "brands")
@Getter @Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Brand {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Basic(optional = false)
    @NaturalId
    private String name;
    
    @Basic(optional = false)
    @Column(name = "image_path")
    private String imagePath;

    @ToString.Exclude @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "rental_category_id") @Basic(optional = false)
    private RentalCategory rentalCategory;
    
}

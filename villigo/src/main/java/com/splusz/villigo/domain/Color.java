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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "colors")
@Getter @Setter
@NoArgsConstructor
public class Color {
	
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Basic(optional = false)
    private String name;

	@ToString.Exclude @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "rental_categories_id") @Basic(optional = false)
    private RentalCategory rentalCategory;

    @Basic(optional = false)
    private String colorNumber;
}

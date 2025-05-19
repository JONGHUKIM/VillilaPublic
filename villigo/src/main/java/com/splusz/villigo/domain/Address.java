package com.splusz.villigo.domain;

import java.util.Map;

import com.splusz.villigo.dto.AddressUpdateDto;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
@Table(name = "ADDRESSES")
@Getter @Setter
@NoArgsConstructor
@ToString
@Builder
@AllArgsConstructor
public class Address {
    @Id
    private Long id;
    
    @ToString.Exclude @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "product_id")
    @MapsId
    private Product product;

    @Basic(optional = false)
    private int zonecode;
    
    @Basic(optional = false)
    private String sido;
    
    @Basic(optional = false)
    private String sigungu;
    
    @Basic(optional = false)
    private String bname;

    @Basic(optional = false) @Column(name = "full_address")
    private String fullAddress;
    
    @Basic(optional = false)
    private Double latitude;
    
    @Basic(optional = false)
    private Double longitude;

    public Address update(AddressUpdateDto addDto) {
        this.zonecode = addDto.getZonecode();
        this.sido = addDto.getSido();
        this.sigungu = addDto.getSigungu();
        this.bname = addDto.getBname();
        this.fullAddress = addDto.getFullAddress();
        this.latitude = addDto.getLatitude();
        this.longitude = addDto.getLongitude();
        return this;
    }

}

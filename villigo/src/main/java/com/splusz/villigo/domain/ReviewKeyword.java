package com.splusz.villigo.domain;

import org.hibernate.annotations.NaturalId;
import jakarta.persistence.Id;
import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "review_keywords")
@Getter @Setter
@NoArgsConstructor
public class ReviewKeyword {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Basic(optional = false) @NaturalId
    private String keyword; 
    
    @Basic(optional = false)
    private int score;
}

package com.splusz.villigo.domain;

import java.time.LocalDate;

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
@Table(name = "user_jjams")
@Getter @Setter
@NoArgsConstructor
public class UserJjam {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

	@ToString.Exclude @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id") @Basic(optional = false)
    private User user;

	@Basic(optional = false)
    private LocalDate transactionDate;
    
	@Basic(optional = false)
    private int transactionAmount;
}

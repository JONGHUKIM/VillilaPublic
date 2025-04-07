package com.splusz.villigo.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "faq")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Faq {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String question;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;
}

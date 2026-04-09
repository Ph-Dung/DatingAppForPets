package com.petmatch.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "nicknames")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Nickname {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "setter_id", nullable = false)
    private User setter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Column(nullable = false)
    private String nickname;
}

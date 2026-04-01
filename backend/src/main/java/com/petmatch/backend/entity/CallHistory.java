package com.petmatch.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "call_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CallHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caller_id", nullable = false)
    private User caller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "callee_id", nullable = false)
    private User callee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CallType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CallStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    @PrePersist
    protected void onCreate() {
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
    }
}

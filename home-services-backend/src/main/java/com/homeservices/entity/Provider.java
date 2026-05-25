package com.homeservices.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "providers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Provider {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private Integer experienceYears;

    private String serviceArea;

    private Double latitude;

    private Double longitude;

    @Column(nullable = false)
    @Builder.Default
    private boolean verified = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private Double rating = 0.0;

    private String shopName;

    @Column(columnDefinition = "TEXT")
    private String shopAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ServiceCategory category;

    // ── Documents (URLs / file paths stored after upload) ─────────────────

    /** Government-issued ID document URL (Aadhaar, PAN, Passport, etc.) */
    private String govtIdDocumentUrl;

    /** Business registration / GST certificate URL */
    private String businessCertificateUrl;

    /** Address proof document URL */
    private String addressProofUrl;

    /** Any additional notes or rejection reason set by the admin */
    @Column(columnDefinition = "TEXT")
    private String adminNotes;
    
    private String phoneOtp;

    private LocalDateTime otpExpiry;

    @Builder.Default
    private boolean phoneVerified = false;
}

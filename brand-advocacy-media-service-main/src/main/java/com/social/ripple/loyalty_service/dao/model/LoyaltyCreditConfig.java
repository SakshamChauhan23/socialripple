package com.social.ripple.loyalty_service.dao.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "loyalty_configuraction")
@Getter
@Setter
public class LoyaltyCreditConfig {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "credit_id")
	private Long creditId;

    @Column(name = "credit_type", nullable = false, length = 100)
    private String creditType;

    @Column(name = "description", length = 350)  
    private String description;

    @Column(name = "default_wallet_id", nullable = false)
    private Integer defaultWalletId;

    @Column(name = "default_points", nullable = false)
    private Double defaultPoints;


}

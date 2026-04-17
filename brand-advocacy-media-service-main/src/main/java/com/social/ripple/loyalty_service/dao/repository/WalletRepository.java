package com.social.ripple.loyalty_service.dao.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.social.ripple.loyalty_service.dao.model.Wallet;

public interface WalletRepository extends JpaRepository<Wallet, Integer> {
	Optional<Wallet> findByName(String name);
}
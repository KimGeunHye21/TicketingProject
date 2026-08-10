package com.example.ticketing.repository;

import com.example.ticketing.domain.Provider;
import com.example.ticketing.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByProviderAndProviderId(
            Provider provider,
            String providerId
    );
}

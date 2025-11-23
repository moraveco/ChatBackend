package com.moraveco.springboot.auth.repository;

import com.moraveco.springboot.auth.entity.Login;
import com.moraveco.springboot.auth.entity.Register;
import com.moraveco.springboot.auth.entity.User;
import com.moraveco.springboot.auth.entity.VerificationToken;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, String>  {

    Optional<VerificationToken> findByToken(String token);

    @Modifying
    @Transactional
    @Query("DELETE FROM VerificationToken v WHERE v.user.id = :userId")
    void deleteByUserId(String userId);


}

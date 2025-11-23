package com.moraveco.springboot.auth.repository;

import com.moraveco.springboot.auth.entity.Login;
import com.moraveco.springboot.auth.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LoginRepository extends JpaRepository<Login, String> {
    boolean existsByEmail(String email);
    Optional<Login> findByEmail(String email);

    @Transactional
    @Modifying
    @Query(value = "INSERT INTO users (id, name, users.lastname) " +
            "VALUES (:id, :name, :lastname )", nativeQuery = true)
    void saveUser(
            @Param("id") String id,
            @Param("name") String name,
            @Param("lastname") String lastname
    );

    @Transactional
    @Modifying
    @Query(value = "INSERT INTO login (id, login.email, login.password) " +
            "VALUES (:id, :email, :password )", nativeQuery = true)
    void saveLogin(
            @Param("id") String id,
            @Param("email") String email,
            @Param("password") String password
    );



}

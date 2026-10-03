package com.springsecurity.demo.repository;

import com.springsecurity.demo.entity.Role;
import com.springsecurity.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(String name);

}

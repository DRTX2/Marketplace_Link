package com.gpis.marketplace_link.repositories;

import com.gpis.marketplace_link.entities.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Role findByName(String name);
    boolean existsByName(String name);

    boolean existsByNameIn(Collection<String> names);
}

package com.gpis.marketplace_link.security.seeders;

import com.gpis.marketplace_link.entities.Role;
import com.gpis.marketplace_link.entities.User;
import com.gpis.marketplace_link.repository.RoleRepository;
import com.gpis.marketplace_link.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Configuration
public class RoleSeeder implements CommandLineRunner {

    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public RoleSeeder(RoleRepository roleRepository,
                      UserRepository userRepository,
                      PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        ensureRole("ROLE_ADMIN");
        ensureRole("ROLE_MODERATOR");
        ensureRole("ROLE_SELLER");
        ensureRole("ROLE_BUYER");

        final String adminEmail = "admin@hotmail.com";
        final String adminUsername = "admin";
        final String adminPhone = "+593999000111";
        final String adminCedula = "0000000000";

        if (userRepository.existsByEmail(adminEmail)
                || userRepository.existsByUsername(adminUsername)
                || userRepository.existsByPhone(adminPhone)
                || userRepository.existsByCedula(adminCedula)) {
            return;
        }

        User admin = new User();
        admin.setEmail(adminEmail);
        admin.setUsername(adminUsername);
        admin.setPassword(passwordEncoder.encode("admin"));
        admin.setFirstName("Admin");
        admin.setLastName("System");
        admin.setPhone(adminPhone);
        admin.setCedula(adminCedula);

        Role roleAdmin = roleRepository.findByName("ROLE_ADMIN");
        admin.setRoles(Set.of(roleAdmin));

        userRepository.save(admin);
    }

    private void ensureRole(String name) {
        Role existing = roleRepository.findByName(name);
        if (existing != null) return;
        Role r = new Role();
        r.setName(name);
        roleRepository.save(r);
    }
}

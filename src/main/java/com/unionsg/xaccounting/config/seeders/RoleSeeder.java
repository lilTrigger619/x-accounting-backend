package com.unionsg.xaccounting.config.seeders;

import com.unionsg.xaccounting.entity.User.Role;
import com.unionsg.xaccounting.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class RoleSeeder implements CommandLineRunner{

    private final RoleRepository roleRepository;

    public RoleSeeder(RoleRepository roleRepository){
        this.roleRepository = roleRepository;
    }


    @Override
    public void run(String... args) {

        seedRole("ADMIN", "System administrator with full access");
        seedRole("ACCOUNTANT", "Can manage accounting operations");
        seedRole("STAFF", "Limited operational access");

    }

    private void seedRole(String name, String description) {

        roleRepository.findByName(name)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(name);
                    role.setDescription(description);
                    return roleRepository.save(role);
                });
    }
}

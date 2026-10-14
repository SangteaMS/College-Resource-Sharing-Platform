package edu.collegehub;

import edu.collegehub.accounts.AccountRole;
import edu.collegehub.accounts.AppUser;
import edu.collegehub.accounts.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/** Creates the configured first administrator without changing existing accounts. */
@Component
public class DemoDataInitializer implements ApplicationRunner {
    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;
    private final String adminName;

    public DemoDataInitializer(AppUserRepository users, PasswordEncoder passwordEncoder,
                               @Value("${collegehub.seed.admin-email:}") String adminEmail,
                               @Value("${collegehub.seed.admin-password:}") String adminPassword,
                               @Value("${collegehub.seed.admin-name:Demo Administrator}") String adminName) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.adminName = adminName;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedAdmin();
    }

    @Transactional
    void seedAdmin() {
        String email = adminEmail == null ? "" : adminEmail.trim().toLowerCase(Locale.ROOT);
        String password = adminPassword == null ? "" : adminPassword;
        if (email.isBlank() && password.isBlank()) return;
        if (email.isBlank() || password.isBlank()) {
            throw new IllegalStateException("Set both COLLEGEHUB_ADMIN_EMAIL and COLLEGEHUB_ADMIN_PASSWORD to create the local administrator.");
        }
        if (password.length() < 12) {
            throw new IllegalStateException("COLLEGEHUB_ADMIN_PASSWORD must be at least 12 characters when creating the first admin.");
        }
        AppUser existing = users.findByEmailIgnoreCase(email).orElse(null);
        if (existing != null) {
            if (existing.getRole() != AccountRole.ADMIN) {
                throw new IllegalStateException("COLLEGEHUB_ADMIN_EMAIL already belongs to a student account. Choose a different email for the administrator; the existing account was not changed.");
            }
            return;
        }
        users.save(new AppUser(email, passwordEncoder.encode(password), adminName, AccountRole.ADMIN));
    }
}

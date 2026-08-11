package de.mimosa_dev.MealPlanner.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-01/FR-02: registration is gated by a valid, unused invite code; passwords are hashed with
 * BCrypt, never stored or compared in plaintext.
 */
@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final InviteCodeRepository inviteCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            AppUserRepository appUserRepository,
            InviteCodeRepository inviteCodeRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.appUserRepository = appUserRepository;
        this.inviteCodeRepository = inviteCodeRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public String register(String email, String password, String inviteCode) {
        InviteCode invite = inviteCodeRepository.findByCode(inviteCode)
                .filter(code -> !code.isUsed())
                .orElseThrow(InvalidInviteCodeException::new);
        if (appUserRepository.findByEmail(email).isPresent()) {
            throw new EmailAlreadyRegisteredException(email);
        }

        AppUser user = appUserRepository.save(new AppUser(email, passwordEncoder.encode(password)));
        invite.markUsed(user.getId());
        inviteCodeRepository.save(invite);

        return jwtService.generate(user.getId());
    }

    public String login(String email, String password) {
        AppUser user = appUserRepository.findByEmail(email).orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return jwtService.generate(user.getId());
    }
}

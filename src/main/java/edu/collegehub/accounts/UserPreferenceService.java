package edu.collegehub.accounts;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserPreferenceService {
    private final AppUserRepository users;
    public UserPreferenceService(AppUserRepository users) { this.users = users; }
    @Transactional(readOnly = true)
    public PreferenceIds get(Long userId) {
        if (userId == null) return new PreferenceIds(null, null);
        AppUser user = users.findById(userId).orElse(null);
        if (user == null || user.getPreferredProgramme() == null) return new PreferenceIds(null, null);
        return new PreferenceIds(user.getPreferredProgramme().getId(),
                user.getCurrentSemester() == null ? null : user.getCurrentSemester().getId());
    }
    public record PreferenceIds(Long programmeId, Long semesterId) { }
}

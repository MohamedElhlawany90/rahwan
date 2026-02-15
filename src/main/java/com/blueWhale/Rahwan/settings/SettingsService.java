package com.blueWhale.Rahwan.settings;

import com.blueWhale.Rahwan.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private final SettingsRepository repository;
    private final SettingsMapper mapper;

    private void checkAdmin(Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();

        if (!"admin".equalsIgnoreCase(user.getRole().name())) {
            throw new RuntimeException("Access denied");
        }
    }

    public SettingsDto create(SettingsForm form, Authentication authentication) {
        checkAdmin(authentication);
        Settings settings = mapper.toEntity(form);
        return mapper.toDto(repository.save(settings));
    }


    public SettingsDto update(SettingsForm form, Authentication authentication) {
        checkAdmin(authentication);

        Settings settings = repository.findById(form.getId())
                .orElseThrow(() -> new RuntimeException("Settings not found"));

        mapper.updateEntityFromForm(form, settings);
        return mapper.toDto(repository.save(settings));
    }


    public SettingsDto getById(String id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new RuntimeException("Settings not found"));
    }

    public List<SettingsDto> getAll() {
        return repository.findAll()
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    public void delete(String id, Authentication authentication) {
        checkAdmin(authentication);
        repository.deleteById(id);
    }

}

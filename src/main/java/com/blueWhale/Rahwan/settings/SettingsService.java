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

        if (!user.isAdmin()) {
            throw new RuntimeException("Access denied");
        }
    }

    public SettingsDto create(SettingsForm form, Authentication authentication) {
//        checkAdmin(authentication);
        Settings settings = mapper.toEntity(form);

        settings.setPrivacyAndPolicy(
                "**Privacy and Policy**\n\n" +
                        "Last updated: 2025-01-01\n\n" +
                        "1. **Information We Collect**\n" +
                        "We collect information you provide directly to us, such as your name, email address, " +
                        "phone number, and shipping address when you create an account or place an order.\n\n" +
                        "2. **How We Use Your Information**\n" +
                        "We use the information we collect to process your orders, send order confirmations, " +
                        "provide customer support, and improve our services.\n\n" +
                        "3. **Sharing of Information**\n" +
                        "We do not sell, trade, or rent your personal information to third parties. " +
                        "We may share your data with trusted service providers who assist us in operating our platform.\n\n" +
                        "4. **Data Security**\n" +
                        "We implement industry-standard security measures to protect your personal data " +
                        "from unauthorized access, alteration, disclosure, or destruction.\n\n" +
                        "5. **Cookies**\n" +
                        "Our platform uses cookies to enhance your browsing experience and analyze site traffic. " +
                        "You may disable cookies through your browser settings.\n\n" +
                        "6. **Your Rights**\n" +
                        "You have the right to access, correct, or delete your personal information at any time " +
                        "by contacting us through the Contact Us page.\n\n" +
                        "7. **Changes to This Policy**\n" +
                        "We reserve the right to update this Privacy Policy at any time. " +
                        "Changes will be posted on this page with an updated revision date.\n\n" +
                        "8. **Contact**\n" +
                        "If you have any questions about this Privacy Policy, please reach out to our support team."
        );

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
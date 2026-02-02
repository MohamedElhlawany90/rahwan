package com.blueWhale.Rahwan.settings;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService service;

    @PostMapping
    public ResponseEntity<SettingsDto> create(
            @RequestBody SettingsForm form,
            Authentication authentication) {

        return ResponseEntity.ok(service.create(form, authentication));
    }


    @PutMapping
    public ResponseEntity<SettingsDto> update(
            @RequestBody SettingsForm form,
            Authentication authentication) {

        return ResponseEntity.ok(service.update(form, authentication));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SettingsDto> getById(@PathVariable String id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<SettingsDto>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, Authentication authentication) {
        service.delete(id, authentication);
        return ResponseEntity.noContent().build();
    }

}

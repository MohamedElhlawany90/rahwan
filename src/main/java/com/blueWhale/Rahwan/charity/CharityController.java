package com.blueWhale.Rahwan.charity;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/charities")
@RequiredArgsConstructor
public class CharityController {

    private final CharityService charityService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CharityDto> createCharity(@Valid @ModelAttribute CharityForm form) {
        try {
            CharityDto created = charityService.createCharity(form);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/allActiveCharities")
    public ResponseEntity<List<CharityDto>> getAllActiveCharities() {
        List<CharityDto> charities = charityService.getAllActiveCharities();
        return ResponseEntity.ok(charities);
    }

    @GetMapping("/all")
    public ResponseEntity<List<CharityDto>> getAllCharities() {
        List<CharityDto> charities = charityService.getAllCharities();
        return ResponseEntity.ok(charities);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CharityDto> getCharityById(@PathVariable Long id) {
        try {
            CharityDto charity = charityService.getCharityById(id);
            return ResponseEntity.ok(charity);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CharityDto> updateCharity(
            @PathVariable Long id,
            @Valid @ModelAttribute CharityForm form) {
        try {
            CharityDto updated = charityService.updateCharity(id, form);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCharity(@PathVariable Long id) {
        try {
            charityService.deleteCharity(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PutMapping("/{id}/reactivate")
    public ResponseEntity<CharityDto> reactivateCharity(@PathVariable Long id) {
        try {
            CharityDto reactivated = charityService.reactivateCharity(id);
            return ResponseEntity.ok(reactivated);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }
}
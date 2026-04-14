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

    /**
     * Create Charity
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CharityDto> createCharity(
            @Valid @ModelAttribute CharityForm form
    ) throws IOException {

        CharityDto created = charityService.createCharity(form);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(created);
    }

    /**
     * Get All Active Charities
     */
    @GetMapping("/active")
    public ResponseEntity<List<CharityDto>> getAllActiveCharities() {
        return ResponseEntity.ok(
                charityService.getAllActiveCharities()
        );
    }

    /**
     * Get All Charities (Admin)
     */
    @GetMapping
    public ResponseEntity<List<CharityDto>> getAllCharities() {
        return ResponseEntity.ok(
                charityService.getAllCharities()
        );
    }

    /**
     * Get Charity By id
     */
    @GetMapping("/{id}")
    public ResponseEntity<CharityDto> getCharityById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                charityService.getCharityById(id)
        );
    }

    /**
     * Update Charity
     */
    @PutMapping(
            value = "/{id}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<CharityDto> updateCharity(
            @PathVariable Long id,
            @Valid @ModelAttribute CharityForm form
    ) throws IOException {

        CharityDto updated = charityService.updateCharity(id, form);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete Charity (Soft Delete)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCharity(
            @PathVariable Long id
    ) {
        charityService.deleteCharity(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reactivate Charity
     */
    @PutMapping("/{id}/reactivate")
    public ResponseEntity<CharityDto> reactivateCharity(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                charityService.reactivateCharity(id)
        );
    }
}

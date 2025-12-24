package com.blueWhale.Rahwan.advertisement;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/advertisements")
@RequiredArgsConstructor
public class AdvertisementController {

    private final AdvertisementService service;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<AdvertisementDto> create(
            @ModelAttribute AdvertisementForm form,
            @RequestPart(value = "photo", required = false) MultipartFile photo) {

        return ResponseEntity.ok(service.create(form, photo));
    }


    @GetMapping
    public ResponseEntity<List<AdvertisementDto>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    public ResponseEntity<AdvertisementDto> update(
            @PathVariable String id,
            @ModelAttribute AdvertisementForm form,
            @RequestPart(value = "photo", required = false) MultipartFile photo
    ) {
        return ResponseEntity.ok(service.update(id, form, photo));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

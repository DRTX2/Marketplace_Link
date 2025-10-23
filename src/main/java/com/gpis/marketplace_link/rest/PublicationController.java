package com.gpis.marketplace_link.rest;

import com.gpis.marketplace_link.dto.publication.request.PublicationCreateRequest;
import com.gpis.marketplace_link.dto.publication.request.PublicationUpdateRequest;
import com.gpis.marketplace_link.dto.publication.response.PublicationResponse;
import com.gpis.marketplace_link.dto.publication.response.PublicationSummaryResponse;
import com.gpis.marketplace_link.services.publications.PublicationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/publications")
public class PublicationController {

    private final PublicationService service;

    public PublicationController(PublicationService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Page<PublicationSummaryResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) List<Long> categoryIds,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon,
            @RequestParam(required = false) Double distanceKm
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publicationDate"));

        Page<PublicationSummaryResponse> response = service.getAll(
                pageable, categoryIds, minPrice, maxPrice, lat, lon, distanceKm
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PublicationResponse> getOne(@PathVariable Long id){

        PublicationResponse response = service.getById(id);

        return ResponseEntity.ok(response);

    }

    @PreAuthorize("hasRole('SELLER')")
    @GetMapping("/by-vendor")
    public ResponseEntity<Page<PublicationSummaryResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) List<Long> categoryIds,
          @RequestParam Long vendorId
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publicationDate"));

        Page<PublicationSummaryResponse> response = service.getAllByVendor(
                pageable,categoryIds,vendorId
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('SELLER')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PublicationResponse> create(@Valid @ModelAttribute PublicationCreateRequest request){
        PublicationResponse response = service.create(request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('SELLER')")
    @PutMapping(value = "/{id}",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PublicationResponse> update(@PathVariable Long id, @Valid @ModelAttribute PublicationUpdateRequest request){
        PublicationResponse response = service.update(id,request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('SELLER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id){
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

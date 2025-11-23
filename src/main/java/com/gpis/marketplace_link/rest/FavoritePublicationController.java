package com.gpis.marketplace_link.rest;

import com.gpis.marketplace_link.dto.publication.response.FavoritePublicationResponse;
import com.gpis.marketplace_link.security.service.SecurityService;
import com.gpis.marketplace_link.services.publications.FavoritePublicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FavoritePublicationController {

    private final SecurityService securityService;
    private final FavoritePublicationService favoritePublicationService;

    @PostMapping("/publications/{publicationId}/favorite")
    public ResponseEntity<FavoritePublicationResponse> addFavorite(
            @PathVariable Long publicationId) {
        Long userId = securityService.getCurrentUserId();
        FavoritePublicationResponse response = favoritePublicationService.addFavorite(userId, publicationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/publications/{publicationId}/favorite")
    public ResponseEntity<Void> removeFavorite(
            @PathVariable Long publicationId) {
        Long userId = securityService.getCurrentUserId();
        favoritePublicationService.removeFavorite(userId, publicationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/favorites")
    public ResponseEntity<Page<FavoritePublicationResponse>> getUserFavorites(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy
    ){
        Long userId = securityService.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortBy));
        Page<FavoritePublicationResponse> favorites = favoritePublicationService.getUserFavorites(userId, pageable);
        return ResponseEntity.ok(favorites);
    }

    @GetMapping("/publications/{publicationId}/favorite/check")
    public ResponseEntity<Boolean> isFavorite(
            @PathVariable Long publicationId) {
        Long userId = securityService.getCurrentUserId();
        boolean isFavorite = favoritePublicationService.isFavorite(userId, publicationId);
        return ResponseEntity.ok(isFavorite);
    }
}

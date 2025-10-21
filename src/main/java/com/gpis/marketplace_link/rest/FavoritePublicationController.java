package com.gpis.marketplace_link.rest;

import com.gpis.marketplace_link.dto.publication.response.FavoritePublicationResponse;
import com.gpis.marketplace_link.services.publications.FavoritePublicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FavoritePublicationController {

    private final FavoritePublicationService favoritePublicationService;

     // api/publications/{id}/favorite?userId={userId}
    @PostMapping("/publications/{publicationId}/favorite")
    public ResponseEntity<FavoritePublicationResponse> addFavorite(
            @PathVariable Long publicationId,
            @RequestParam Long userId) {

        FavoritePublicationResponse response = favoritePublicationService.addFavorite(userId, publicationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // api/publications/{id}/favorite?userId={userId}
    @DeleteMapping("/publications/{publicationId}/favorite")
    public ResponseEntity<Void> removeFavorite(
            @PathVariable Long publicationId,
            @RequestParam Long userId) {

        favoritePublicationService.removeFavorite(userId, publicationId);
        return ResponseEntity.noContent().build();
    }

    // api/users/{id}/favorites
    @GetMapping("/users/{userId}/favorites")
    public ResponseEntity<List<FavoritePublicationResponse>> getUserFavorites(@PathVariable Long userId) {
        List<FavoritePublicationResponse> favorites = favoritePublicationService.getUserFavorites(userId);
        return ResponseEntity.ok(favorites);
    }

    // api/publications/{id}/favorite/check?userId={userId}
    @GetMapping("/publications/{publicationId}/favorite/check")
    public ResponseEntity<Boolean> isFavorite(
            @PathVariable Long publicationId,
            @RequestParam Long userId) {

        boolean isFavorite = favoritePublicationService.isFavorite(userId, publicationId);
        return ResponseEntity.ok(isFavorite);
    }
}

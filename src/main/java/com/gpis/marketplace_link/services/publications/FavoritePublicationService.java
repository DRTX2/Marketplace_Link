package com.gpis.marketplace_link.services.publications;

import com.gpis.marketplace_link.dto.publication.response.FavoritePublicationResponse;
import com.gpis.marketplace_link.entities.FavoritePublication;
import com.gpis.marketplace_link.entities.Publication;
import com.gpis.marketplace_link.entities.User;
import com.gpis.marketplace_link.exceptions.business.publications.FavoriteAlreadyExistsException;
import com.gpis.marketplace_link.exceptions.business.publications.FavoriteNotFoundException;
import com.gpis.marketplace_link.exceptions.business.publications.PublicationNotFoundException;
import com.gpis.marketplace_link.exceptions.business.users.UserNotFoundException;
import com.gpis.marketplace_link.mappers.FavoritePublicationMapper;
import com.gpis.marketplace_link.repositories.FavoritePublicationRepository;
import com.gpis.marketplace_link.repositories.PublicationRepository;
import com.gpis.marketplace_link.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoritePublicationService {

    private final FavoritePublicationRepository favoritePublicationRepository;
    private final UserRepository userRepository;
    private final PublicationRepository publicationRepository;
    private final FavoritePublicationMapper favoritePublicationMapper;

    /**
     * Marca una publicación como favorita para un usuario
     */
    @Transactional
    public FavoritePublicationResponse addFavorite(Long userId, Long publicationId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuario con ID " + userId + " no encontrado"));

        Publication publication = publicationRepository.findById(publicationId)
                .orElseThrow(() -> new PublicationNotFoundException("Publicación con ID " + publicationId + " no encontrada"));

        if (favoritePublicationRepository.existsByUserIdAndPublicationId(userId, publicationId)) {
            throw new FavoriteAlreadyExistsException("Esta publicación ya está marcada como favorita");
        }

        FavoritePublication favorite = new FavoritePublication();
        favorite.setUser(user);
        favorite.setPublication(publication);

        FavoritePublication savedFavorite = favoritePublicationRepository.save(favorite);

        return favoritePublicationMapper.toResponse(savedFavorite);
    }

    @Transactional
    public void removeFavorite(Long userId, Long publicationId) {
        FavoritePublication favorite = favoritePublicationRepository.findByUserIdAndPublicationId(userId, publicationId)
                .orElseThrow(() -> new FavoriteNotFoundException("Esta publicación no está en favoritos"));

        favoritePublicationRepository.delete(favorite);
    }

    @Transactional(readOnly = true)
    public List<FavoritePublicationResponse> getUserFavorites(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("Usuario con ID " + userId + " no encontrado");
        }

        List<FavoritePublication> favorites = favoritePublicationRepository.findAllByUserIdOrderByCreatedAtDesc(userId);

        return favorites.stream()
                .map(favoritePublicationMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean isFavorite(Long userId, Long publicationId) {
        return favoritePublicationRepository.existsByUserIdAndPublicationId(userId, publicationId);
    }
}

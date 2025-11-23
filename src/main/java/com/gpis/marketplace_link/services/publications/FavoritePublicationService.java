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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

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

        // Buscar cualquier registro, incluso si está marcado como deleted (consulta nativa)
        Optional<FavoritePublication> anyOpt = favoritePublicationRepository.findAnyByUserIdAndPublicationId(userId, publicationId);

        if (anyOpt.isPresent()) {
            FavoritePublication existing = anyOpt.get();
            // Si el registro existe y no está borrado, es un duplicado
            if (existing.getDeleted() == null || !existing.getDeleted()) {
                throw new FavoriteAlreadyExistsException("Esta publicación ya está marcada como favorita");
            }

            // Está marcado como borrado (soft-deleted): restaurar
            int updated = favoritePublicationRepository.restoreByUserIdAndPublicationId(userId, publicationId);
            if (updated > 0) {
                // Recuperar el registro restaurado (ahora visible para JPA)
                FavoritePublication restored = favoritePublicationRepository.findByUserIdAndPublicationId(userId, publicationId)
                        .orElse(null);
                if (restored != null) {
                    return favoritePublicationMapper.toResponse(restored);
                }
            }
            // Si por alguna razón no se recuperó, continuamos para crear uno nuevo (aunque esto no debería pasar)
        }

        // No existe: crear uno nuevo
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
    public Page<FavoritePublicationResponse> getUserFavorites(Long userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("Usuario con ID " + userId + " no encontrado");
        }

        Page<FavoritePublication> favoritesPage = favoritePublicationRepository.findByUserId(userId, pageable);

        return favoritesPage.map(favoritePublicationMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public boolean isFavorite(Long userId, Long publicationId) {
        return favoritePublicationRepository.existsByUserIdAndPublicationId(userId, publicationId);
    }

    @Transactional
    public void removeFavoritesByPublicationId(Long publicationId) {
        favoritePublicationRepository.softDeleteAllByPublicationId(publicationId);
    }

    @Transactional
    public void restoreFavoritesByPublicationId(Long publicationId) {
        favoritePublicationRepository.restoreAllByPublicationId(publicationId);
    }
}

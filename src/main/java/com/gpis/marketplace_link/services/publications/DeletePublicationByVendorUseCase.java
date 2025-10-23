package com.gpis.marketplace_link.services.publications;

import com.gpis.marketplace_link.entities.Publication;
import com.gpis.marketplace_link.repositories.PublicationRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@AllArgsConstructor
@Service
public class DeletePublicationByVendorUseCase {
    private final PublicationRepository repository;
    private final PublicationService service;


    @Transactional
    public void softDelete(Long userId) {
        List<Publication> publications = repository.findAllByVendorIdAndDeletedAtIsNull(userId);
        for (Publication publication : publications) {
            service.delete(publication.getId());
        }
    }
}

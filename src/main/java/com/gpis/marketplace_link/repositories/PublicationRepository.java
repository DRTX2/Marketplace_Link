package com.gpis.marketplace_link.repositories;

import com.gpis.marketplace_link.entities.Publication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PublicationRepository  extends JpaRepository<Publication, Long>, JpaSpecificationExecutor<Publication> {

    List<Publication> findAllByVendorIdAndDeletedAtIsNull(Long vendorId);

    @Query("""
    select p from Publication p
    left join fetch p.images
    where p.id = :id
    """)
    Optional<Publication> findByIdWithImages(@Param("id") Long id);
}

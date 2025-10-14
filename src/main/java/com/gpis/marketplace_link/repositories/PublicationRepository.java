package com.gpis.marketplace_link.repositories;

import com.gpis.marketplace_link.entities.Publication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PublicationRepository  extends JpaRepository<Publication, Long>, JpaSpecificationExecutor<Publication> {
}

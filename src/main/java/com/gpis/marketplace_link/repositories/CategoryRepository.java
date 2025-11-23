package com.gpis.marketplace_link.repositories;

import com.gpis.marketplace_link.entities.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository  extends JpaRepository<Category, Long> {

}

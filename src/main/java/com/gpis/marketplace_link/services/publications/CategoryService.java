package com.gpis.marketplace_link.services.publications;

import com.gpis.marketplace_link.entities.Category;
import com.gpis.marketplace_link.repositories.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository repository;

    public CategoryService(CategoryRepository repository) {
        this.repository = repository;
    }

    public List<Category> getAll(){

        return repository.findAll();
    }
}

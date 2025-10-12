package com.gpis.marketplace_link.services;

import com.gpis.marketplace_link.entities.Category;
import com.gpis.marketplace_link.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

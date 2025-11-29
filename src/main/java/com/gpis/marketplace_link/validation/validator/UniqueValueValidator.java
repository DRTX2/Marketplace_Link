package com.gpis.marketplace_link.validation.validator;

import com.gpis.marketplace_link.validation.annotation.UniqueValue;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

@Component
public class UniqueValueValidator implements ConstraintValidator<UniqueValue, Object> {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private HttpServletRequest request; // Proxy request-scoped

    private Class<?> entityClass;
    private String field;
    private boolean ignoreCase;
    private String pathVariable;
    private String idField;

    @Override
    public void initialize(UniqueValue ann) {
        this.entityClass  = ann.entity();
        this.field        = ann.field();
        this.ignoreCase   = ann.ignoreCase();
        this.pathVariable = ann.pathVariable();
        this.idField      = ann.idField();

        if (this.entityClass == null) {
            throw new IllegalArgumentException("@UniqueValue: 'entity' is required");
        }
        if (this.field == null || this.field.isBlank()) {
            throw new IllegalArgumentException("@UniqueValue: 'field' is required");
        }
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) return true;
        if (value instanceof String s && s.isBlank()) return true;

        Long currentId = extractIdFromPath();

        StringBuilder jpql = new StringBuilder();
        jpql.append("select count(e) from ")
                .append(entityClass.getName())
                .append(" e where ");

        if (ignoreCase && value instanceof String) {
            jpql.append("lower(e.").append(field).append(") = lower(:val)");
        } else {
            jpql.append("e.").append(field).append(" = :val");
        }

        if (currentId != null) {
            jpql.append(" and e.").append(idField).append(" <> :id");
        }

        var q = em.createQuery(jpql.toString(), Long.class)
                .setParameter("val", value);

        if (currentId != null) {
            q.setParameter("id", currentId);
        }

        Long count = q.getSingleResult();
        return count == 0;
    }

    @SuppressWarnings("unchecked")
    private Long extractIdFromPath() {
        Object attr = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (!(attr instanceof Map<?, ?> map)) return null;
        Object raw = map.get(pathVariable);
        if (raw == null) return null;
        try {
            return Long.valueOf(raw.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}

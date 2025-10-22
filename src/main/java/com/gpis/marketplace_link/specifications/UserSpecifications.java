package com.gpis.marketplace_link.specifications;

import com.gpis.marketplace_link.entities.Role;
import com.gpis.marketplace_link.entities.User;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class UserSpecifications {

    /**
     * Creates a specification to filter users by multiple criteria
     *
     * @param includeDeleted If true, includes soft-deleted users; if false, only active users
     * @param roleNames List of role names to filter by (e.g., ["ROLE_ADMIN", "ROLE_MODERATOR"])
     * @param searchTerm Search term to match against username, email, firstName, lastName, or cedula
     * @return A combined Specification
     */
    public static Specification<User> filterUsers(Boolean includeDeleted, List<String> roleNames, String searchTerm) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter by deleted status
            if (includeDeleted != null && !includeDeleted) {
                // Only non-deleted users
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.isNull(root.get("deleted")),
                        criteriaBuilder.isFalse(root.get("deleted"))
                ));
            }
            // If includeDeleted is true or null, we don't add any filter for deleted status

            // Filter by roles
            if (roleNames != null && !roleNames.isEmpty()) {
                Join<User, Role> rolesJoin = root.join("roles", JoinType.INNER);
                predicates.add(rolesJoin.get("name").in(roleNames));
            }

            // Search term filter
            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                String likePattern = "%" + searchTerm.trim().toLowerCase() + "%";
                Predicate searchPredicate = criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("username")), likePattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), likePattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), likePattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), likePattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("cedula")), likePattern)
                );
                predicates.add(searchPredicate);
            }

            // Ensure distinct results when joining with roles
            if (query != null) {
                query.distinct(true);
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}


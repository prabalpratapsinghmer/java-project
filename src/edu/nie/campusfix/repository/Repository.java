package edu.nie.campusfix.repository;

import java.util.List;
import java.util.Optional;

/**
 * Generic repository interface demonstrating Java Generics and Abstraction.
 *
 * @param <T> Domain entity type
 * @param <ID> Entity identifier type
 */
public interface Repository<T, ID> {
    Optional<T> findById(ID id);
    List<T> findAll();
    T save(T entity);
    boolean deleteById(ID id);
    boolean existsById(ID id);
}

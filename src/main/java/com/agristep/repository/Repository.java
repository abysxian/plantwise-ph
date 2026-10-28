package com.agristep.repository;

import java.util.List;
import java.util.Optional;

/** The small set of operations every repository offers. Implemented with JDBC in the persistence package. */
public interface Repository<T, ID> {
    T save(T entity);

    Optional<T> findById(ID id);

    List<T> findAll();

    long count();
}

package com.agristep.repository;

import com.agristep.domain.Crop;

import java.util.List;
import java.util.Optional;

public interface CropRepository extends Repository<Crop, Long> {
    Optional<Crop> findByCode(String code);

    List<Crop> findAllByOrderByNameAsc();
}

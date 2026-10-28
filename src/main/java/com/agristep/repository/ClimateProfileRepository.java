package com.agristep.repository;

import com.agristep.domain.ClimateProfile;

import java.util.Optional;

public interface ClimateProfileRepository extends Repository<ClimateProfile, Long> {
    Optional<ClimateProfile> findByLocationKey(String locationKey);
}

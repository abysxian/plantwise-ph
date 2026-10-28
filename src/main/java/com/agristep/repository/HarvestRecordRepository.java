package com.agristep.repository;

import com.agristep.domain.HarvestRecord;

import java.util.List;

public interface HarvestRecordRepository extends Repository<HarvestRecord, Long> {
    List<HarvestRecord> findTop10ByOrderByHarvestedOnDescIdDesc();
}

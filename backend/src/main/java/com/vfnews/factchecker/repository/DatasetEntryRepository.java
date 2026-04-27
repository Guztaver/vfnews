package com.vfnews.factchecker.repository;

import com.vfnews.factchecker.domain.DatasetEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DatasetEntryRepository extends JpaRepository<DatasetEntry, Long> {
}

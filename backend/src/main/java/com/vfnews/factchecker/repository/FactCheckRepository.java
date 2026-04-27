package com.vfnews.factchecker.repository;

import com.vfnews.factchecker.domain.FactCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FactCheckRepository extends JpaRepository<FactCheck, Long> {
}

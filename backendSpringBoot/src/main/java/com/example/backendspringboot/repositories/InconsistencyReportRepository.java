package com.example.backendspringboot.repositories;


import com.example.backendspringboot.model.InconsistencyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InconsistencyReportRepository extends JpaRepository<InconsistencyReport, Long> {}
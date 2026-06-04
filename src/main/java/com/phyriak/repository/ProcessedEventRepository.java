package com.phyriak.repository;

import com.phyriak.repository.model.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProcessedEventRepository
        extends JpaRepository<ProcessedEvent, UUID> {
}
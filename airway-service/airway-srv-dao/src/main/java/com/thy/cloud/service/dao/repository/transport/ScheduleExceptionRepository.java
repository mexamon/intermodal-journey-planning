package com.thy.cloud.service.dao.repository.transport;

import com.thy.cloud.data.jpa.repository.GenericRepository;
import com.thy.cloud.service.dao.entity.transport.ScheduleException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ScheduleExceptionRepository extends GenericRepository<ScheduleException, UUID> {

    List<ScheduleException> findByEdgeId(UUID edgeId);

    List<ScheduleException> findByEdgeIdAndExceptionDate(UUID edgeId, LocalDate date);
}

package org.javaibm.eventfeedbackanalyzer.repository.h2;

import org.javaibm.eventfeedbackanalyzer.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {
}

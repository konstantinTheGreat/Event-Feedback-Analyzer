package org.javaibm.eventfeedbackanalyzer.repository.h2;

import org.javaibm.eventfeedbackanalyzer.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findByEvent_Id(Long eventId);
}

package com.petmatch.backend.repository;

import com.petmatch.backend.entity.Appointment;
import com.petmatch.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByRequesterOrRecipientOrderByMeetingTimeDesc(User requester, User recipient);
}

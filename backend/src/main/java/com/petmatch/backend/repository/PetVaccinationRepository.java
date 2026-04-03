package com.petmatch.backend.repository;

import com.petmatch.backend.entity.PetVaccination;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PetVaccinationRepository extends JpaRepository<PetVaccination, Long> {
    List<PetVaccination> findByPetIdOrderByVaccinatedDateDesc(Long petId);
}

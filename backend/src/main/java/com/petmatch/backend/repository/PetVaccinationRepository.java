package com.petmatch.backend.repository;

import com.petmatch.backend.entity.PetVaccination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PetVaccinationRepository extends JpaRepository<PetVaccination, Long> {
    List<PetVaccination> findByPetIdOrderByVaccinatedDateDesc(Long petId);
}

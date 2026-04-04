package com.petmatch.backend.repository;

import com.petmatch.backend.entity.PetVaccination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PetVaccinationRepository extends JpaRepository<PetVaccination, Long> {

    List<PetVaccination> findByPetIdOrderByVaccinatedDateDesc(Long petId);

    long countByPetId(Long petId);

    /** Tìm theo ID và petId để tránh cross-user access */
    Optional<PetVaccination> findByIdAndPetId(Long id, Long petId);

    /** Xóa an toàn – chỉ xóa nếu thuộc đúng pet */
    void deleteByIdAndPetId(Long id, Long petId);
}

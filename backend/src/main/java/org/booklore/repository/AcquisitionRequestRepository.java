package org.booklore.repository;

import org.booklore.model.entity.AcquisitionRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AcquisitionRequestRepository extends JpaRepository<AcquisitionRequestEntity, Long> {

    List<AcquisitionRequestEntity> findAllByOrderByCreatedAtDesc();

    List<AcquisitionRequestEntity> findAllByRequestedByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<AcquisitionRequestEntity> findByIdAndRequestedByUserId(Long id, Long userId);
}

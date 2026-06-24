package org.booklore.repository;

import org.booklore.model.entity.AcquisitionProviderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AcquisitionProviderRepository extends JpaRepository<AcquisitionProviderEntity, Long> {

    Optional<AcquisitionProviderEntity> findByProviderKey(String providerKey);

    boolean existsByProviderKey(String providerKey);

    List<AcquisitionProviderEntity> findAllByEnabledTrue();
}

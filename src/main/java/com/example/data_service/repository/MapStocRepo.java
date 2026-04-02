package com.example.data_service.repository;

import com.example.data_service.model.MapStocOptim;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional

public interface MapStocRepo extends JpaRepository<MapStocOptim,Long> {

    @Query("select m from MapStoc m where TRIM(m.idIntern) = TRIM(:idA)")
    List<MapStocOptim> findByCodProdus(@Param("idA") String idA);
}

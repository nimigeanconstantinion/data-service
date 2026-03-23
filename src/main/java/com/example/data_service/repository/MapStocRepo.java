package com.example.data_service.repository;

import com.example.data_service.model.MapStocOptim;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional

public interface MapStocRepo extends JpaRepository<MapStocOptim,Long> {

    @Query(value = "select m from MapStoc m where TRIM(m.idIntern)=?1")
    Optional<MapStocOptim> findByCodProdus(String idA);

}

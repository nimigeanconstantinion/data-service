package com.example.data_service.repository;

import com.example.data_service.model.MapStocOptim;
import com.example.data_service.model.MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<MessageEntity, String> {

    @Query(value = "select m from MessageEntity m where trim(m.action)=?1")
    Optional<MapStocOptim> findCreateMessage(String action);


    @Query(value = "SELECT * FROM message_entity WHERE TRIM(payload::jsonb->>'id_intern') = TRIM(:idA)", nativeQuery = true)
    List<MessageEntity> findProductByIDA(@Param("idA") String idA);

}

package com.example.data_service;

import com.example.data_service.model.MapStocOptim;
import com.example.data_service.model.MessageAction;
import com.example.data_service.model.MessageEvent;
import com.example.data_service.repository.MapStocRepo;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
class DataServiceIT extends AbstractIntegrationTest {

    @Autowired
    MapStocRepo mapStocRepo;

    @Test
    void consuma_eveniment_si_salveaza_in_db() {
        // ARRANGE - construim un produs si il impachetam intr-un eveniment CREATED
        MapStocOptim produs = new MapStocOptim();
        produs.setIdIntern("ART-001");
        produs.setArticol("Surubelnita");
        produs.setCategorie("Scule");
        produs.setGrupa("Manuale");
        produs.setFurniz("Furnizor SRL");
        produs.setNr_zile(7);

        MessageEvent event = new MessageEvent();
        event.setId("evt-001");
        event.setAction(MessageAction.CREATED);
        event.setPayload(produs);
        event.setTimestamp(Instant.now());

        KafkaTemplate<String, Object> template = new KafkaTemplate<>(producerFactory());

        // ACT - publicam evenimentul pe topic; consumerul aplicatiei il preia
        template.send("product-topic", event.getId(), event);
        template.flush();

        // ASSERT - asteptam pana cand consumerul a salvat produsul in DB
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            List<MapStocOptim> gasite = mapStocRepo.findByCodProdus("ART-001");
            assertThat(gasite).hasSize(1);
            assertThat(gasite.get(0).getArticol()).isEqualTo("Surubelnita");
            assertThat(gasite.get(0).getNr_zile()).isEqualTo(7);
        });
    }

    private ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(props);
    }
}

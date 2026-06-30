package com.example.data_service;

import net.bytebuddy.utility.dispatcher.JavaDispatcher;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Testcontainers
class DataServiceIT {
    @JavaDispatcher.Container
    static KafkaContainer kafka =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));
    @Container static MySQLContainer<?> mysql =
            new MySQLContainer<>("mysql:8.0").withDatabaseName("micro_db");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        r.add("spring.datasource.url",      mysql::getJdbcUrl);
        r.add("spring.datasource.username", mysql::getUsername);
        r.add("spring.datasource.password", mysql::getPassword);
    }

    @Test void consuma_eveniment_si_salveaza_in_db() {
        // produce un MessageEvent pe product-topic -> asteapta -> assert ca produsul e in repo
    }
}

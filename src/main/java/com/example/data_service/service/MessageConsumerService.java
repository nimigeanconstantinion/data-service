package com.example.data_service.service;

import com.example.data_service.model.MapStocOptim;
import com.example.data_service.model.MessageAction;
import com.example.data_service.model.MessageEntity;
import com.example.data_service.model.MessageEvent;
import com.example.data_service.repository.MapStocRepo;
import com.example.data_service.repository.MessageRepository;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Instant;
import static net.logstash.logback.argument.StructuredArguments.*;

@Service
public class MessageConsumerService {
  private final MessageRepository repository;
  private final MapStocService mapStocService;
  private static final Logger log = LoggerFactory.getLogger(MessageConsumerService.class);

  public MessageConsumerService(MessageRepository repository,MapStocService mapStocService) {

    this.repository = repository;
    this.mapStocService=mapStocService;
  }

  @Transactional
  @KafkaListener(topics = "${app.kafka.topic}")
  public void handle(MessageEvent event) {
    System.out.println("Kafka consumer received: " + event);
    if (event == null || event.getId() == null) {
      log.error("null event received",
              keyValue("eventType", "CONSUMER_MESSAGE"),
              value("ERROR", event));
      return;
    }

    Gson gson = new Gson(); // Or use new GsonBuilder().create();
    MapStocOptim product = gson.fromJson(event.getPayload(), MapStocOptim.class); // deserializes json into target2

    if (MessageAction.DELETED.equals(event.getAction())) {
      try {
        mapStocService.delMapStoc(product.getIdIntern());
        log.info("DELETE SUCCES",
                keyValue("deleteSUCCES", "DELETE"),
                value("product", product));


      }catch (Exception e){
        log.error("DELETE ERROR",
                keyValue("deleteERROR", e.getMessage()),
                value("product", product));
      }


    }else if (MessageAction.UPDATED.equals(event.getAction())) {
      try {
        mapStocService.updMapStoc(product);
        log.info("UPDATE SUCCES",
                keyValue("updateSucces", "UPDATE"),
                value("product", product));

      }catch (Exception e){
        log.error("UPDATE ERROR",
                keyValue("deleteERROR", e.getMessage()),
                value("product", product));

      }
    } else if (MessageAction.CREATED.equals(event.getAction())) {
      try {
        mapStocService.addMapStoc(product);
        log.info("CREATE SUCCES",
                keyValue("createSucces", "CREATE"),
                value("product", product));

      }catch (Exception e){
        log.error("CREATE ERROR",
                keyValue("createERROR", e.getMessage()),
                value("product", product));


      }
    }


//
//    MessageEntity entity = new MessageEntity(
//        event.getId(),
//        event.getAction().name(),
//        event.getPayload(),
//        event.getTimestamp() != null ? event.getTimestamp() : Instant.now());
//    repository.save(entity);
  }
}

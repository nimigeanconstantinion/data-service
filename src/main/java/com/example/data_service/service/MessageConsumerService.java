package com.example.data_service.service;

import com.example.data_service.model.MapStocOptim;
import com.example.data_service.model.MessageAction;
import com.example.data_service.model.MessageEntity;
import com.example.data_service.model.MessageEvent;
import com.example.data_service.repository.MapStocRepo;
import com.example.data_service.repository.MessageRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static net.logstash.logback.argument.StructuredArguments.*;

@Service
public class MessageConsumerService {
//  private MessageRepository repository;
  private MapStocService mapStocService;
  private static final Logger log = LoggerFactory.getLogger(MessageConsumerService.class);
  private MessageRepository messageRepository;

  public MessageConsumerService(MapStocService mapStocService, MessageRepository messageRepository) {

    this.mapStocService=mapStocService;
    this.messageRepository = messageRepository;
  }

  @Transactional
  @KafkaListener(topics = "product-topic")
  public void handle(MessageEvent event)  {
    if (event == null || event.getId() == null) {
      log.error("null event received",
              keyValue("eventType", "CONSUMER_MESSAGE"),
              value("ERROR", event));
      return;
    }
//
    Gson gson = new Gson(); // Or use new GsonBuilder().create();
    MessageEntity mesaj= new MessageEntity();
    mesaj.setId(event.getId());
    mesaj.setAction(event.getAction().name());
    mesaj.setPayload(gson.toJson(event.getPayload()));
    mesaj.setUpdatedAt(event.getTimestamp());
    MapStocOptim mesProduct=gson.fromJson(mesaj.getPayload(),MapStocOptim.class);
    mesProduct.setId(0L);
    if (MessageAction.DELETED.equals(event.getAction())) {
      List<MessageEntity> mesaje=messageRepository.findAll();

      try {

        mapStocService.delMapStoc(mesProduct.getIdIntern());
        log.info("DELETE SUCCES",
                keyValue("deleteSUCCES", "DELETE"),
                value("product", mesProduct));


      }catch (Exception e){
        log.error("DELETE ERROR",
                keyValue("deleteERROR", e.getMessage()),
                value("product", mesProduct));
      }

    }else if (MessageAction.UPDATED.equals(event.getAction())) {
      try {
        mapStocService.updMapStoc(mesProduct);
        log.info("UPDATE SUCCES",
                keyValue("updateSucces", "UPDATE"),
                value("product", mesProduct));

      }catch (Exception e){
        log.error("UPDATE ERROR",
                keyValue("deleteERROR", e.getMessage()),
                value("product", mesProduct));

      }
    } else if (MessageAction.CREATED.equals(event.getAction())) {
      try {
        List<MessageEntity> listaMesaje=messageRepository.findAll();
        if(listaMesaje.stream().filter(p->p.getId().equals(mesaj.getId())&&p.getAction().equals("CREATED")).collect(Collectors.toList()).size()==0){

             try{
               mapStocService.addMapStoc(mesProduct);
               log.info("PRODUCT_CREATED_SUCCES",
                       keyValue("PRODUCT_CREATED", mesProduct));

             }catch (Exception e){

             }
             messageRepository.save(mesaj);
            log.info("MESSAGE_CREATED_SUCCES",
                  keyValue("MESSAGE_CREATED", mesaj));

        }else{
          log.error("DATA_CREATED_ERR",
                  keyValue("MESSAGE_EXISTS", mesProduct));

        }

      }catch (Exception e){
        log.error("CREATE ERROR",
                keyValue("createERROR", e.getMessage()));


      }
    }


  }
}

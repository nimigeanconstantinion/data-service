package com.example.data_service.web;

import com.example.data_service.model.MapStocOptim;
//import com.example.data_service.rabbitMQListener.MyMessageListener;
import com.example.data_service.repository.MessageRepository;
import com.example.data_service.service.MapStocService;
import com.example.data_service.service.MessageConsumerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/command")
//@CrossOrigin
@Slf4j
public class DataController {

    private MapStocService mapStocService;
    private MessageConsumerService consumerService;

    public DataController(MapStocService mapStocService, MessageConsumerService consumerService) {

        this.mapStocService = mapStocService;
        this.consumerService=consumerService;

    }

//    @ResponseStatus(HttpStatus.OK)
//    @PostMapping("/add")
//    public ResponseEntity<MapStocOptim> addMapArt(@RequestBody MapStocOptim mapstoc){
//       try {
//
//           MapStocOptim newProd=repository
//            mapStocService.addMapStoc(mapstoc);
//            MapStocOptim amp=mapStocService.getByID(mapstoc.getIdIntern());
//
//            return ResponseEntity.ok(amp);
//       }catch (RuntimeException e){
//            throw new RuntimeException(e.getMessage());
//       }
//    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/getallmap")
    public ResponseEntity<List<MapStocOptim>> getAllMapArt(){
            try{
                List<MapStocOptim> mpp=mapStocService.getAllMapStoc();
                return ResponseEntity.ok(mpp);
            }catch (RuntimeException e){
                throw new RuntimeException("Eroare");
            }
//        return ResponseEntity.ok(mapStocService.getAllMapStoc());
    }

//
//    @ResponseStatus(HttpStatus.OK)
//    @DeleteMapping("/del/{idP}")
//
//    public ResponseEntity<Boolean> delMapArt(@PathVariable String idP){
////        MyMessage myMessage=myMessageListener.receiveMessage(MyMessage my);
//
////                return ResponseEntity.ok(true);
//                return ResponseEntity.ok(mapStocService.delMapStoc(idP));
//    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/getbyidp/{idP}")
    public ResponseEntity<MapStocOptim> getMapArt(@PathVariable String idP) throws RuntimeException{
        try {
            return ResponseEntity.ok(mapStocService.getByID(idP));
        }catch (RuntimeException e){
            throw e;

        }


    }

//    @ResponseStatus(HttpStatus.OK)
//    @PostMapping("/update")
//    public ResponseEntity<MapStocOptim> updateMapArt(@RequestBody MapStocOptim mapstoc){
//        log.info("COMM_UPDATE:"+mapstoc.toString());
//        log.info("Updateeeeeeee");
////        return ResponseEntity.ok(mapstoc);
//       return ResponseEntity.ok(mapStocService.updMapStoc(mapstoc));
//    }

//    @ResponseStatus(HttpStatus.OK)
//    @PostMapping("/bulk")
//    public ResponseEntity<Boolean> saveBulkList(@RequestBody List<MapStocOptim> lista){
////        return ResponseEntity.ok(true);
//        return ResponseEntity.ok(mapStocService.saveBulk(lista));
//    }

}

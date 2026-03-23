package com.example.data_service.service;

import com.example.data_service.model.MapStocOptim;
import com.example.data_service.repository.MapStocRepo;
import com.example.data_service.repository.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
@Service
public class MapStocServiceImpl implements MapStocService{
    private MapStocRepo mapStocRepo;
    private final MessageRepository repository;

    private static final Logger log = LoggerFactory.getLogger(MapStocServiceImpl.class);

    public MapStocServiceImpl(MapStocRepo mapStocRepo,MessageRepository messageRepository) {
        this.mapStocRepo = mapStocRepo;
        this.repository = messageRepository;
    }


    public MapStocOptim getByID(String ida){
        Optional<MapStocOptim> om=mapStocRepo.findByCodProdus(ida);
        if(om.isPresent()){
            return om.get();
        }
        throw new RuntimeException("Eroare din Service MapStocService");
    }

    @Override
    public void addMapStoc(MapStocOptim mp) {

        try{
            log.info("cmdAdd: "+mp.toString());
            Optional<MapStocOptim> optP=mapStocRepo.findByCodProdus(mp.getIdIntern().trim());
            if(optP.isPresent()){
                    throw(new RuntimeException("Eroare din Service MapStocService produ existent"));
            }else {
                mapStocRepo.save(mp);

            }
//            return mapStocRepo.findByCodProdus(mp.getIdIntern()).get();
        }catch (RuntimeException e){
            log.error("cmdUpdErr: "+mp.toString());

            throw new RuntimeException("Nu am reusit add din service impl!!");
        }

    }

    @Override
    public boolean delMapStoc(String idp) {
        Optional<MapStocOptim> mp=mapStocRepo.findByCodProdus(idp);
        if(mp.isEmpty()){
            log.error("cmdDelErrNoCode: "+mp.toString());

            throw new RuntimeException("Produsul cu cod-ul indicat nu exista!!");
        }
        log.info("cmdDel: "+mp.toString());
        mapStocRepo.delete(mp.get());
        return true;
    }

    @Override
    public MapStocOptim updMapStoc(MapStocOptim mp) {
      Optional<MapStocOptim> op=mapStocRepo.findByCodProdus(mp.getIdIntern().trim());
      if(op.isEmpty()){
          log.error("cmdUpdNoCode:"+mp.getIdIntern());
          throw new RuntimeException("Maparea cu codul dat , nu exista!!");
      }
      try{
          MapStocOptim mpp=op.get();
          mpp.setNr_zile(mp.getNr_zile());
          log.info("UPDATE!!");
//          log.info("commUpd-",op.toString());
          log.info("cmdUpd: "+mpp.toString());

          return mapStocRepo.saveAndFlush(mp);

      }catch (RuntimeException e){
          log.error("cmdUpdErr: "+mp.toString());

        throw new RuntimeException("Nu am reusit UPdate!!");
      }
    }

    @Override
    public List<MapStocOptim> getAllMapStoc() {
        try {
            return mapStocRepo.findAll();
        }catch (RuntimeException e){
            throw new RuntimeException("Nu a putut lua lista  de produse!");
        }

    }

    @Override
    public boolean saveBulk(List<MapStocOptim> lista) {
        try {
            log.info("cmdUpdBulk: "+lista.size());
            mapStocRepo.saveAllAndFlush(lista);
            return true;
        }catch (RuntimeException e){
            log.error("cmdUpdErrBulk: "+lista.size());
            throw new RuntimeException("Nu am putut salva produsele noi!");
        }
    }


}

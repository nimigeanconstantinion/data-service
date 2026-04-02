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
        List<MapStocOptim> lista=mapStocRepo.findByCodProdus(ida);
        if(lista.size()>0){
            return lista.get(0);
        }
        throw new RuntimeException("Eroare din Service MapStocService");
    }

    @Override
    public void addMapStoc(MapStocOptim mp) {

        try{
            log.info("cmdAdd: "+mp.toString());
            List<MapStocOptim> optP=mapStocRepo.findByCodProdus(mp.getIdIntern().trim());
            if(optP.size()>0){
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
        List<MapStocOptim> lmp=mapStocRepo.findByCodProdus(idp);
        if(lmp.size()==0){
            log.error("cmdDelErrNoCode: "+idp);

            throw new RuntimeException("Produsul cu cod-ul indicat nu exista!!");
        }
        log.info("cmdDel: "+lmp.get(0).toString());
        log.info("DEL_PROD",lmp.get(0));
        mapStocRepo.delete(lmp.get(0));
        return true;
    }

    @Override
    public MapStocOptim updMapStoc(MapStocOptim mp) {
      List<MapStocOptim> lop=mapStocRepo.findByCodProdus(mp.getIdIntern().trim());
      if(lop.size()==0){
          log.error("cmdUpdNoCode:"+mp.getIdIntern());
          throw new RuntimeException("Maparea cu codul dat , nu exista!!");
      }
      try{
          MapStocOptim mpp=lop.get(0);
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

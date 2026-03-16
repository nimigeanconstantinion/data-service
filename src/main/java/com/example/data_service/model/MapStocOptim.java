package com.example.data_service.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Data
@Entity(name = "MapStoc")
@Table(name = "map_stoc_optim")
@Setter
@Getter
public class MapStocOptim implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO,generator = "map_art_generator")
    @SequenceGenerator(name = "map_art_generator",initialValue = 1,allocationSize = 1)
    private long id;

    @Column(name="id_intern")
    private String idIntern;

    @Column(name = "articol")
    private String articol;

    @Column(name = "categorie")
    private String categorie;

    @Column(name = "grupa")
    private String grupa;

    @Column(name = "idFurn")
    private int id_furn;

    @Column(name = "furniz")
    private String furniz;

    @Column(name = "nrZile")
    private int nr_zile;

    @Override
    public String toString() {
        return "MapStocOptim{" +
                "id=" + id +
                ", idIntern='" + idIntern + '\'' +
                ", articol='" + articol + '\'' +
                ", categorie='" + categorie + '\'' +
                ", grupa='" + grupa + '\'' +
                ", id_furn=" + id_furn +
                ", furniz='" + furniz + '\'' +
                ", nr_zile=" + nr_zile +
                '}';
    }
}

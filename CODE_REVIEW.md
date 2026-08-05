# Code review — data-service (2026-08-05)

> Primul review pe acest repo. Context: data-service e LIVE în `business` (chart `microservice`, tag `06e667d`), consumă `product-topic` și scrie în `micro_db`. Review-ul se citește împreună cu cel de la `importer-service` — cele două servicii sunt copii una alteia și au aceleași defecte de config, plus un contract între ele care nu ține.

## Sumar

| # | Sev | Fișier | Problemă |
|---|---|---|---|
| H1 | 🔴 | `MapStocServiceImpl.java:38-44` | sincronizarea e „insert o dată, apoi niciodată update" — modificările de la sursă nu ajung în `micro_db` |
| H2 | 🔴 | `MapStocServiceImpl.java:76-82` | `updMapStoc` salvează obiectul greșit → INSERT duplicat în loc de UPDATE |
| H3 | 🟡 | `model/MapStocOptim.java:21-22` | `id_intern` = cheie de business, dar fără constrângere de unicitate |
| H4 | 🟡 | `application.yaml:38` | `enable-auto-commit: true` + catch-all → mesajul e pierdut definitiv la orice eroare |
| H5 | 🟡 | `application.yaml:29` | `ddl-auto: update` în cluster |
| H6 | 🟡 | `application.yaml:20`, `application-argo.yaml:23-28` | aceleași capcane de config ca la importer |
| H7 | 🟢 | `MessageConsumerService.java:52-56` | `MessageEntity` construit și niciodată salvat |
| H8 | 🟢 | `files/realm-export.json:31,49` | „secrete" placeholder într-un repo public |
| H9 | 🟢 | `.log` comise în git | 3 fișiere de log în istoric, `.gitignore` fără `*.log` |
| H10 | 🟢 | `MessageConsumerService.java:51-57` | Jackson → Gson → Gson pentru același obiect |

CI/CD-ul e identic cu cel de la importer și la fel de bine făcut (`ci.yml` — SHA imutabil, `cd-bump` cu guard și idempotență). Nu am observații pe el.

---

## 🔴 H1 — Sincronizarea nu sincronizează: „insert o dată, apoi niciodată"

Lanțul complet, capăt la capăt:

1. `importer-service` publică **toate** rândurile la fiecare 10 minute, fiecare ca `CREATED` (`MapStocOptimImportScheduler:25-31`)
2. `MessageConsumerService.java:111` → ramura `CREATED` → `mapStocService.addMapStoc(mesProduct)`
3. `MapStocServiceImpl.java:38-44`:

```java
List<MapStocOptim> optP = mapStocRepo.findByCodProdus(mp.getIdIntern().trim());
if (optP.size() > 0) {
    throw (new RuntimeException("Eroare din Service MapStocService produ existent"));
} else {
    mapStocRepo.save(mp);
}
```

4. excepția e prinsă în consumer (`MessageConsumerService.java:115-119`) și logată ca `SYNC ERROR`

**Ce înseamnă asta în practică:** primul ciclu populează `micro_db`. Al doilea ciclu, și toate cele de după, produc **doar erori**. Dacă în ERP se schimbă `nr_zile` la un articol, sau furnizorul, sau denumirea — modificarea nu ajunge niciodată în `micro_db`. Nu pentru că lipsește codul de update: pentru că importer-ul spune „CREATED", iar consumer-ul răspunde „îl am deja" și oprește acolo.

Vestea bună: nu se acumulează duplicate (asta era ipoteza din review-ul importer-ului, Q2 — s-a dovedit greșită). Vestea proastă e mai rea: **datele sunt tăcut învechite**, și singurul semnal e un log de eroare la fiecare 10 minute, pe fiecare articol. La 500 de articole înseamnă 500 de linii `SYNC ERROR` la fiecare rulare, adică ~72.000 pe zi în Elasticsearch — zgomot care îngroapă exact erorile reale.

**Mecanismul de dedesubt.** Ai un flux de sincronizare (starea completă, retrimisă periodic), dar l-ai modelat ca flux de evenimente (fapte care s-au întâmplat o dată). Un `CREATED` care se repetă la fiecare 10 minute nu e un eveniment — e o afirmație despre starea curentă. Operația corespunzătoare nu e `INSERT`, ci **upsert**: „adu rândul ăsta în starea asta, indiferent dacă există sau nu". De-asta consumer-ul trebuie să fie *idempotent*: primind același mesaj de 100 de ori, rezultatul din DB trebuie să fie identic cu cel de la primul.

### Fix

Cheia de business e `id_intern`. Pune-i unicitate în DB (vezi H3) și înlocuiește insert-ul cu upsert:

```java
@Override
public void addMapStoc(MapStocOptim mp) {
    List<MapStocOptim> existente = mapStocRepo.findByCodProdus(mp.getIdIntern().trim());
    if (existente.isEmpty()) {
        mapStocRepo.save(mp);
        return;
    }
    MapStocOptim existent = existente.get(0);
    mp.setId(existent.getId());        // aceeasi identitate => UPDATE, nu INSERT
    mapStocRepo.save(mp);
}
```

Diferența esențială: `mp.setId(existent.getId())` — atâta timp cât `id` e 0, Hibernate consideră obiectul *nou* și face INSERT. Cu id-ul rândului existent, același `save()` devine UPDATE. Nu e o chestiune de metodă (`save` vs `update`), ci de identitate a obiectului.

---

## 🔴 H2 — `updMapStoc` salvează obiectul greșit

`MapStocServiceImpl.java:76-82`
```java
MapStocOptim mpp = lop.get(0);      // randul din DB
mpp.setNr_zile(mp.getNr_zile());    // modifici obiectul incarcat...
log.info("cmdUpd: " + mpp.toString());
return mapStocRepo.saveAndFlush(mp); // ...si salvezi ALT obiect (mp)
```

Două bug-uri într-o singură linie:

1. `mpp` (rândul real, cu id-ul lui) e modificat și apoi **aruncat** — modificarea nu ajunge nicăieri, pentru că `mpp` e detașat (interogarea și-a închis tranzacția), deci dirty checking-ul nu-l vede.
2. `mp` are `id = 0` — setat explicit în consumer la `MessageConsumerService.java:58` (`mesProduct.setId(0L)`). Pentru un `long` primitiv, 0 e „valoarea de neexistent", deci `saveAndFlush(mp)` face **INSERT** cu id generat nou.

Rezultat: un UPDATE creează un rând duplicat și lasă originalul neatins.

E dormant azi — importer-ul nu emite niciodată `UPDATED`, doar `CREATED`. Dar e fix capcana care se declanșează în momentul în care repari H1 și începi să trimiți evenimente de update. Repară-le împreună.

---

## Before / After (critice)

| # | Acum | Cum ar trebui |
|---|---|---|
| H1 | `if (optP.size() > 0) { throw new RuntimeException("produ existent"); }`<br>`else { mapStocRepo.save(mp); }` | `if (existente.isEmpty()) { mapStocRepo.save(mp); return; }`<br>`mp.setId(existente.get(0).getId());`<br>`mapStocRepo.save(mp);` |
| H2 | `MapStocOptim mpp = lop.get(0);`<br>`mpp.setNr_zile(mp.getNr_zile());`<br>`return mapStocRepo.saveAndFlush(mp);` | `MapStocOptim mpp = lop.get(0);`<br>`mpp.setNr_zile(mp.getNr_zile());`<br>`return mapStocRepo.saveAndFlush(mpp);`<br>(salvezi entitatea încărcată, care are id-ul real) |

**Verify după fix:**
```bash
# 1. o rulare completa de scheduler, fara erori
kubectl -n business logs deploy/data-service | grep -c "SYNC ERROR"     # trebuie 0

# 2. idempotenta: numarul de randuri NU creste intre doua cicluri de 10 min
kubectl -n data exec -it moco-mysql-0 -- mysql -u... micro_db \
  -e "SELECT COUNT(*), COUNT(DISTINCT id_intern) FROM map_stoc_optim;"
#    cele doua numere trebuie sa fie EGALE (si sa ramana egale peste 20 min)

# 3. propagarea unei modificari: schimba nr_zile in sursa, asteapta 10 min, verifica
```

---

## 🟡 Importante

**H3 — `id_intern` e cheia de business, dar nimic nu o apără.**
`model/MapStocOptim.java:21-22` — coloană simplă, fără `unique = true`. Unicitatea e verificată în Java (`findByCodProdus` apoi `save`), adică un read-then-write fără protecție: cu o singură replică merge, cu două (sau la un rebalance de consumer group) doi handleri pot citi „nu există" simultan și insera amândoi. Constrângerea în DB e singura care ține indiferent de câte instanțe rulează — și, în plus, e ce face upsert-ul din H1 sigur.

```java
@Column(name = "id_intern", unique = true, nullable = false)
```
(atenție: adaugă-o după ce cureți eventualele duplicate deja existente în `micro_db`)

**H4 — offset comis chiar și când procesarea eșuează.**
`application.yaml:38` — `enable-auto-commit: true`, iar consumer-ul prinde absolut toate excepțiile (`MessageConsumerService.java:115`). Combinația înseamnă: Kafka marchează mesajul ca procesat, indiferent ce s-a întâmplat. La o eroare tranzitorie — MySQL repornit de MOCO, o secundă de rețea — mesajul e pierdut definitiv, fără retry și fără dead-letter topic. Azi te salvează faptul că importer-ul retrimite tot la 10 minute; asta e o plasă de siguranță accidentală, nu o decizie. Când treci la publicare pe delta (recomandarea din importer M6), plasa dispare.

**H5 — `ddl-auto: update` în cluster.**
`application.yaml:29` și `application-argo.yaml:38`. Hibernate modifică schema în producție la fiecare pornire, iar userul are `GRANT ALL PRIVILEGES` (din `mysql-init-job.yaml`), deci chiar poate. `update` nu șterge și nu modifică tipuri — deci într-o zi o să ai coloane orfane pe care nu le mai folosește nimeni, și o schimbare de tip care pur și simplu nu se aplică, tăcut. În cluster: `validate` (schema e creată de un job de migrare sau de Flyway); `update` rămâne pentru local.

**H6 — aceleași capcane de config ca la importer-service.**
Nu sunt bug-uri noi, sunt aceleași bug-uri de două ori — pentru că fișierele sunt copiate între servicii:
- `application.yaml:20` — `expected-issuer` hardcodat, singura proprietate fără `${}` → totul depinde de `SPRING_PROFILES_ACTIVE=argo`
- `application-argo.yaml:23-28` — bloc `keycloak:` mort, cu `${KEYCLOAK_CLIENT_SECRET}` fără default și fără nicio clasă care să-l citească (plus comentariul „# ADAUGĂ ASTA AICI:" rămas din copy-paste)
- `application.yaml` vs `application-argo.yaml` — ~90% identice
- `application-helm.yaml` încă există (la importer l-ai șters deja) — mort

Când repari ceva în unul din cele două servicii, verifică imediat dacă același lucru e și în celălalt. Asta e costul real al copy-paste-ului între microservicii: nu duplicarea codului, ci duplicarea greșelilor, cu decalaj în timp.

---

## 🟢 Cleanups

- **H7** — `MessageConsumerService.java:52-56`: `MessageEntity mesaj` e construit complet (id, action, payload, timestamp) și **nu e salvat niciodată** — `messageRepository.save(mesaj)` e comentat la `:102`. Tabela `messages` rămâne goală, deci nu ai nici audit, nici o bază pentru deduplicare. Ori o folosești (ar fi exact ce-ți trebuie pentru idempotență: „am mai văzut id-ul ăsta?"), ori scoți entitatea și repository-ul. Tot acolo, `:60` — `messageRepository.findAll()` pe ramura DELETE încarcă toată tabela într-o variabilă pe care n-o citește nimeni.
- **H8** — `files/realm-export.json:31,49`: `"secret": "your-secret-key-here-change-in-production"` și `"secret": "my-secret-keycloak"`, plus useri cu parole. Sunt placeholder-e, dar repo-ul e public și un scanner nu face diferența — iar realm-ul real trăiește acum în `ms-gitops`. Fișierul e istorie, șterge-l.
- **H9** — `command-service.log`, `data-service.log`, `logs/data-service.log` sunt **comise în git** (verificat cu `git ls-files`). Primul e chiar de la alt serviciu. `.gitignore` n-are `*.log`. Adaugă-l și scoate-le din tracking (`git rm --cached`).
- **H10** — `MessageConsumerService.java:51-57`: payload-ul e deserializat de Jackson (`JsonDeserializer` din config), apoi re-serializat cu Gson (`gson.toJson`), apoi re-deserializat tot cu Gson în `MapStocOptim`. Trei conversii și două biblioteci de JSON pentru un obiect care putea veni tipizat de la început (`MessageEvent<MapStocOptim>` sau un simplu cast). Fiecare conversie e o ocazie de a pierde un câmp fără să afli.
- **H11** — excepții care înghit cauza: `MapStocServiceImpl.java:46-50` prinde `RuntimeException e` și aruncă un `RuntimeException("Nu am reusit add din service impl!!")` **fără** să paseze `e`. Mai sus, consumer-ul loghează doar `e.getMessage()`. Rezultat: când chiar pică ceva real (constrângere DB, conexiune pierdută), în Kibana vezi „Nu am reusit add din service impl!!" și zero stack trace. Minim: `throw new RuntimeException("...", e)` și `log.error("SYNC ERROR", e)`.

---

## Q&A

**Q1.** Importer-ul retrimite tot catalogul la fiecare 10 minute. Din perspectiva consumer-ului, ce diferență e între „un eveniment că s-a creat un produs" și „o afirmație despre cum arată produsul acum"? Care dintre cele două descrie ce trimite de fapt importer-ul, și ce operație de DB îi corespunde?

**Q2.** În `updMapStoc` încarci `mpp` din DB, îi modifici `nr_zile`, dar salvezi `mp`. Dacă în loc de asta ai fi salvat `mpp` — de ce ar fi funcționat, deși `mp` și `mpp` au aceleași câmpuri? Ce anume face diferența pentru Hibernate?

**Q3.** Ai `enable-auto-commit: true` și un `try/catch` care prinde tot. Dacă MySQL e indisponibil 30 de secunde, ce se întâmplă cu mesajele consumate în intervalul ăla, și cum ai afla că s-au pierdut?

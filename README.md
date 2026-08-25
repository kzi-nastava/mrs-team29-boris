# Click&Drive – mobilna aplikacija

Projekat se sastoji od:

- `backendSpringBoot` – Spring Boot REST/WebSocket backend;
- `MobilnaAplikacijaTim29` – Android mobilna aplikacija.

## Preuslovi

Potrebno je instalirati:

- Java JDK **17** za backend;
- Android Studio sa Android SDK-om i emulatorom ili fizičkim Android telefonom;
- PostgreSQL na portu `5432`;
- MailDev za lokalno presretanje e-mailova.

Android projekat koristi Gradle Wrapper (`Gradle 9.5.0`) i Android Gradle Plugin `9.3.1`. Android aplikacija se kompajlira sa SDK 37, zahteva najmanje Android API 30, a Java source/target podešavanja su 11. Za Android Studio je preporučen JDK 17.

## 1. PostgreSQL

Backend po podrazumevanim podešavanjima očekuje sledeće kredencijale:

| Podešavanje | Vrednost |
|---|---|
| Host | `localhost` |
| Port | `5432` |
| Baza | `mobilneappdb` |
| Korisnik | `postgres` |
| Lozinka | `testpassword` |

Ako PostgreSQL već postoji, napraviti bazu:

```sql
CREATE DATABASE mobilneappdb;
```

Za novu lokalnu PostgreSQL instancu može se koristiti Docker:

```bash
docker run --name mobilne-postgres \
  -e POSTGRES_DB=mobilneappdb \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=testpassword \
  -p 5432:5432 \
  -d postgres
```

Podešavanja se nalaze u `backendSpringBoot/src/main/resources/application.properties`. Ako se koriste drugi host, port, baza, korisnik ili lozinka, promeniti `spring.datasource.*` vrednosti u tom fajlu.

> Razvojna konfiguracija koristi `spring.jpa.hibernate.ddl-auto=create`. To znači da Hibernate pri pokretanju kreira šemu baze iz početka. Ne koristiti ovu konfiguraciju za produkciju sa važnim podacima.

## 2. MailDev

MailDev je potreban zato što aplikacija šalje aktivacione linkove, linkove za reset lozinke i linkove za registraciju vozača. U razvojnom okruženju se e-mailovi ne šalju stvarnom mail serveru, već se prikazuju u MailDev-u.

Pokretanje preko Docker-a:

```bash
docker run --name mobilne-maildev \
  -p 1080:1080 \
  -p 1025:1025 \
  -d maildev/maildev
```

MailDev web interfejs je dostupan na [http://localhost:1080](http://localhost:1080), a SMTP server na `localhost:1025`. SMTP korisničko ime i lozinka nisu potrebni.

Ako je MailDev već kreiran, ponovo ga pokrenuti komandom:

```bash
docker start mobilne-maildev
```

## 3. Pokretanje backend-a

Iz root direktorijuma projekta:

```bash
cd backendSpringBoot
./mvnw spring-boot:run
```

Na Windows-u koristiti:

```bat
cd backendSpringBoot
mvnw.cmd spring-boot:run
```

Backend se pokreće na portu `8080`. Ako je start uspešan, aplikacija je dostupna na `http://localhost:8080`. Swagger dokumentacija je dostupna na [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html).

Za pokretanje testova:

```bash
cd backendSpringBoot
./mvnw test
```

Prilikom prvog starta backend automatski ubacuje demo korisnike, vozila, cene i početne vožnje u bazu.

## 4. Povezivanje mobilne aplikacije sa backend-om

Adresa backend-a je upisana u:

`MobilnaAplikacijaTim29/app/src/main/java/com/example/mobilnaaplikacijatim29/data/api/ApiClient.java`

Trenutna vrednost je:

```java
private static final String BACKEND_BASE_URL = "http://172.20.10.2:8080/";
```

Ako drugi korisnik pokreće backend na svom računaru, treba da promeni samo IP adresu u ovoj konstanti. Port ostaje `8080`, osim ako se backend posebno ne podesi drugačije.

### Android emulator

Ako backend radi na istom računaru na kom radi standardni Android emulator, koristiti:

```java
private static final String BACKEND_BASE_URL = "http://10.0.2.2:8080/";
```

`10.0.2.2` je adresa host računara iz perspektive Android emulatora.

### Fizički telefoni preko Wi-Fi hotspot-a i Wireless debugging-a

Za testiranje sa fizičkim telefonima može se koristiti sledeća postavka:

1. Na jednom telefonu uključiti Wi-Fi hotspot.
2. Na taj hotspot povezati laptop na kom se pokreće backend.
3. Na isti hotspot povezati oba Android telefona na kojima će aplikacija biti instalirana.
4. Na oba Android telefona uključiti **Developer options** i **Wireless debugging**.
5. U Android Studio-u upariti telefone preko opcije **Pair Devices Using Wi-Fi**. Telefoni će se zatim pojaviti kao uređaji za pokretanje aplikacije.

Backend radi na laptopu, zato u `BACKEND_BASE_URL` treba upisati IPv4 adresu laptopa na hotspot mreži, a ne IP adresu telefona na kom je hotspot uključen. Na primer:

```java
private static final String BACKEND_BASE_URL = "http://192.168.1.25:8080/";
```

IP adresu laptopa na hotspot mreži pronaći komandom `ip addr` na Linux-u, `ipconfig` na Windows-u ili `ifconfig` na macOS-u. Ne koristiti `localhost`, jer bi se tada telefon povezivao sam sa sobom. Ako povezivanje ne radi, proveriti da firewall dozvoljava dolazne konekcije na port `8080` i da hotspot ne koristi opciju koja međusobno izoluje povezane uređaje.

Wireless debugging služi za instaliranje i pokretanje aplikacije iz Android Studio-a. Za komunikaciju aplikacije sa backend-om i dalje je potrebno da svi uređaji budu povezani na isti hotspot i da aplikacija koristi IP adresu laptopa.

## 5. Pokretanje Android aplikacije

Otvoriti direktorijum `MobilnaAplikacijaTim29` u Android Studio-u, sačekati Gradle sync i pokrenuti aplikaciju na emulatoru ili povezanom telefonu.

Pokretanje iz terminala:

```bash
cd MobilnaAplikacijaTim29
./gradlew installDebug
```

APK se može pronaći u `app/build/outputs/apk/debug/`. Za fizičke telefone uključiti **Wireless debugging**, a za emulator napraviti uređaj sa API 30 ili novijim.

## 6. Demo nalozi

Backend pri prvom pokretanju kreira sledeće naloge:

| Uloga | E-mail | Lozinka |
|---|---|---|
| Administrator | `admin@demo.com` | `admin123` |
| Putnik | `passenger@demo.com` | `passenger123` |
| Putnik | `passenger2@demo.com` | `passenger2123` |
| Putnik | `ana.putnik@demo.com` | `ana123` |
| Putnik | `milica.putnik@demo.com` | `milica123` |
| Vozač | `driver@demo.com` | `driver123` |
| Vozači | `driver2@demo.com` – `driver5@demo.com` | `driver123` |

## Najčešći problemi

- **Connection refused na PostgreSQL:** proveriti da PostgreSQL radi na portu `5432` i da se kredencijali poklapaju sa `application.properties`.
- **E-mail se ne pojavljuje:** proveriti da MailDev radi i da su otvoreni portovi `1025` i `1080`.
- **Aplikacija ne može da dohvati backend:** proveriti `BACKEND_BASE_URL`, da su telefon i računar na istoj mreži i da backend radi na portu `8080`.
- **Promena IP adrese nije dovoljna za web klijente:** backend CORS/WebSocket konfiguracija trenutno dozvoljava `http://localhost:4200`; ako se koristi drugi web origin, treba promeniti dozvoljeni origin u odgovarajućim backend konfiguracijama.

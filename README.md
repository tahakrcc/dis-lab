# Köprü — Diş Laboratuvarı ↔ Klinik Platformu

Köprü, diş laboratuvarları ile klinikler arasındaki çok-çoka iş akışını, fiyatlandırmayı, vaka durum takibini ve cari hesap hareketlerini yöneten güvenli bir backend platformudur.

---

## 1. Mimari Kararlar ve Gerekçeleri

### 1.1 Veritabanı Seviyesinde Hasta Gizliliği (Row Level Security - RLS)
- **Gerekçe:** Klinik ile laboratuvar arasındaki en kritik gizlilik kuralı, gerçek hasta kimliğinin (ad, telefon, TC/doğum tarihi) laboratuvara hiçbir koşulda sızmamasıdır. Laboratuvar yalnızca vaka üzerindeki `hasta_rumuzu` (takma ad) bilgisini görür.
- **Uygulama:**
  - `case` tablosunda `patient_id` alanı **kesinlikle bulunmaz**.
  - Hasta kimlikleri `patient` tablosunda, ilişki ise `case_patient_link` tablosunda saklanır.
  - PostgreSQL seviyesinde `FORCE ROW LEVEL SECURITY` aktif edilmiştir. `patient` ve `case_patient_link` tabloları için yazılan RLS politikaları yalnızca kliniğe ait kullanıcıların okumasına izin verir. Uygulama katmanında yapılacak bir kod veya sorgu hatasında dahi veritabanı seviyesinde lab kullanıcılarına 0 satır döner.
  - API katmanında MapStruct ile ayrılmış `CaseLabMapper` ve `CaseClinicMapper` kullanılarak lab yanıtlarında hasta nesneleri hiçbir şekilde serialize edilmez.

### 1.2 Çift DB Rolü ve Tenant Bağlamı (TenantContext & TenantAspect)
- **Gerekçe:** Superuser veya `BYPASSRLS` yetkisine sahip bir veritabanı kullanıcısı RLS politikalarını baypas eder.
- **Uygulama:**
  - Şema oluşturma ve Flyway migration'ları `postgres` (veya `kopru_owner`) yetkili kullanıcısıyla çalıştırılır.
  - Spring Boot uygulaması kısıtlı `kopru_app` kullanıcısı ile veritabanına bağlanır (`BYPASSRLS` yetkisi yoktur).
  - HikariCP bağlantı havuzu kullandığından, her HTTP isteğinde JWT'den çözülen kullanıcı kimliği `TenantContext` (`ThreadLocal<UUID>`) içine yazılır.
  - `@Transactional` metodların etrafında çalışan `TenantAspect` (`@Order(HIGHEST_PRECEDENCE + 10)`), her transaction'ın İLK sorgusu olarak `SELECT set_config('app.user_id', :uid, true)` (yerel işlem bayrağıyla) çalıştırarak RLS'i tetikler.

### 1.3 Flyway Tek Doğruluk Kaynağıdır (Single Source of Truth)
- `spring.jpa.hibernate.ddl-auto=validate` olarak ayarlanmıştır; Hibernate şema üretemez ve değiştiremez.
- Tüm PostgreSQL native enum'lar, exclusion constraint'ler (tarih çakışması engelleme), audit trigger'ları ve RLS politikaları el yazımı `V1`..`V6` Flyway migration dosyalarındadır.

### 1.4 Tek Durum Makinesi (CaseStateMachine) ve Versiyonlu Onay
- `case.durum` alanı kod tabanının başka hiçbir yerinde doğrudan değiştirilemez.
- Mutabakat yarışı versiyon numarasıyla (`fiyat_versiyon`) çözülür. Fiyat her değiştiğinde veya karşı teklif verildiğinde `fiyat_versiyon` artırılır ve önceki onaylar geçersizleşir.
- Her durum geçişi aynı transaction içinde `case_event` tablosuna değişmez (immutable) denetim kaydı yazar.

### 1.5 Tek Doğruluk Kaynaklı Cari Hesap (Ledger)
- Ayrı bir `payment` tablosu yoktur; borç (`BORC`), tahsilat (`TAHSILAT`) ve düzeltmeler (`DUZELTME`) tek bir `ledger_entry` tablosunda tutulur.
- Vaka teslimatında borç oluşturulurken vaka `SELECT ... FOR UPDATE` ile kilitlenir; partial unique index sayesinde aynı vakaya mükerrer borç yazılamaz (`409 ALREADY_DELIVERED`).
- Bakiye sorguları `security_invoker = true` olan `balance` PostgreSQL view'ı üzerinden RLS kuralları korunarak hesaplanır.

---

## 2. Gereksinimler

- **Java:** 21 (örn. Eclipse Adoptium / Azul Zulu 21)
- **Maven:** 3.9+ (veya proje dizinindeki `mvnw.cmd`)
- **Docker & Docker Compose** (PostgreSQL 16 için)

---

## 3. Ortam Değişkenleri

Uygulama `.env` veya ortam değişkenleri ile yapılandırılabilir:

| Değişken | Varsayılan | Açıklama |
|---|---|---|
| `DB_HOST` | `localhost` | PostgreSQL sunucu adresi |
| `DB_PORT` | `5432` | PostgreSQL portu |
| `DB_NAME` | `kopru` | Veritabanı adı |
| `DB_OWNER_USER` | `postgres` | Flyway migration kullanıcısı |
| `DB_OWNER_PASSWORD` | `postgres` | Flyway migration parolası |
| `DB_APP_USER` | `kopru_app` | Uygulama bağlantı kullanıcısı (RLS kısıtlı) |
| `DB_APP_PASSWORD` | `kopru_app_pass` | Uygulama bağlantı parolası |
| `JWT_SECRET` | `Y2hhbmdl...` | JWT imzalama anahtarı (min 256 bit Base64) |

---

## 4. Hızlı Kurulum ve Çalıştırma

### 1. Veritabanını Başlatın
```bash
docker compose up -d
```
> Bu komut PostgreSQL 16'yı başlatır ve `docker/init.sql` ile `kopru_app` kısıtlı kullanıcısını ve izinlerini otomatik kurar.

### 2. Migration ve Uygulamayı Başlatın
```bash
# Windows PowerShell
$env:JAVA_HOME = "C:\Users\temel\.jdks\azul-21.0.10"
mvn spring-boot:run
```

Uygulama ayağa kalktığında Flyway otomatik olarak `V1`..`V6` migration'larını çalıştırır ve seed verilerini yükler.

---

## 5. Seed Verisi ve Test Kullanıcıları

Sistem başlangıçta aşağıdaki hazır verilerle gelir (tüm kullanıcıların parolası: `password123`):

| Email | Organizasyon | Rol |
|---|---|---|
| `labadmin@dentart.com` | DentArt Laboratuvar (LAB) | `LAB_ADMIN` |
| `teknisyen@dentart.com` | DentArt Laboratuvar (LAB) | `LAB_TEKNISYEN` |
| `admin@incidis.com` | İnci Diş Kliniği (KLİNİK) | `KLINIK_ADMIN` |
| `hekim@incidis.com` | İnci Diş Kliniği (KLİNİK) | `HEKIM` |
| `asistan@incidis.com` | İnci Diş Kliniği (KLİNİK) | `ASISTAN` |

### Örnek Vakalar:
- `2026-000001`: `TASLAK` durumunda vaka
- `2026-000002`: `URETIMDE` durumunda vaka
- `2026-000003`: `TAMAMLANDI` durumunda vaka (teslime hazır)

---

## 6. API Endpoint Özeti

Tüm isteklerde `Authorization: Bearer <token>` ve organizasyon bağlamı için `X-Org-Id: <org-uuid>` header'ı kullanılır.

### Kimlik & Organizasyon
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`
- `GET  /api/v1/me`
- `POST /api/v1/organizations`
- `GET  /api/v1/organizations/{id}`
- `POST /api/v1/organizations/{id}/members`

### Ortaklık & Katalog
- `POST /api/v1/partnerships`
- `GET  /api/v1/partnerships`
- `PATCH /api/v1/partnerships/{id}`
- `GET  /api/v1/service-items`
- `POST /api/v1/service-items`
- `PATCH /api/v1/service-items/{id}`
- `GET  /api/v1/partnerships/{id}/prices`
- `PUT  /api/v1/partnerships/{id}/prices`

### Hastalar (Yalnızca Klinik)
- `GET  /api/v1/patients`
- `POST /api/v1/patients`

### Vaka & Kargo Takibi
- `POST /api/v1/cases`
- `GET  /api/v1/cases`
- `GET  /api/v1/cases/{id}`
- `PATCH /api/v1/cases/{id}/items`
- `POST /api/v1/cases/{id}/transitions`
- `GET  /api/v1/cases/{id}/events`
- `POST /api/v1/cases/{id}/shipments`
- `PATCH /api/v1/shipments/{id}`

### Cari Hesap & Bakiye
- `GET  /api/v1/partnerships/{id}/ledger` (Hareketler + `balance` view bakiyesi)
- `POST /api/v1/partnerships/{id}/payments`
- `POST /api/v1/partnerships/{id}/adjustments`

---

## 7. Testleri Çalıştırma

Tüm RLS izolasyon ve iş kuralı testleri **Testcontainers** ile gerçek PostgreSQL 16 üzerinde koşar:

```bash
mvn test
```

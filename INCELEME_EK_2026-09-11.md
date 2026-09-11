# Köprü — Kod İnceleme Ek Raporu

*Tarih: 11.09.2026 · Önceki rapor (`KOPRU_KOD_INCELEME_RAPORU.md`, 09.09.2026) V1–V6 + `src/main` + testleri kapsıyordu. Bu ek; o rapordan sonra eklenen kodu (V7–V13, admin/message/attachment domainleri), frontend'i ve derleme durumunu kapsar.*

## Derleme / Build Durumu
- **Frontend:** `tsc --noEmit` **temiz** (0 hata). Tipler backend DTO alan adlarıyla uyumlu (`access`/`refresh`, `metin`, vb.).
- **Backend:** Cihazda yalnızca Java 11 ve `mvn` yok; proje Java 21 istediğinden burada derlenemedi. Statik/elle inceleme yapıldı. `pom.xml`, Lombok/MapStruct annotation-processor zinciri ve `lombok.version` (Spring Boot parent'tan) tutarlı — derleme engeli görünmüyor.

## Bulgular (kritiklik sırasına göre)

### 1. [ORTA] RLS INSERT ihlali yanlış exception ile yakalanıyor → 403 yerine 500
`MessageService.sendMessage` ve `AttachmentService.upload` içinde, kullanıcının üyesi olmadığı bir vakaya yazma girişimi için `DataIntegrityViolationException` yakalanıp 403 dönülmek isteniyor:

```java
} catch (DataIntegrityViolationException ex) {
    throw ApiException.forbidden("Bu vakaya mesaj gonderme yetkiniz yok.");
}
```

Ancak PostgreSQL RLS `WITH CHECK` ihlali **SQLSTATE 42501**'dir. Bu, Hibernate/Spring tarafından `InvalidDataAccessResourceUsageException` (veya `PermissionDeniedDataAccessException`) olarak çevrilir — `DataIntegrityViolationException` **değil**. Dolayısıyla catch bloğu tetiklenmez; hata `GlobalExceptionHandler.handleGeneral(Exception)` üzerinden **HTTP 500** olarak döner.
- İlgili: `MessageService.java`, `AttachmentService.java`.
- Yan etki: `handleGeneral` ham `ex.getMessage()`'ı istemciye döndürüyor (iç detay/SQL sızıntısı) ve 500'ü `ErrorCode.VALIDATION_ERROR` ile etiketliyor.
- Öneri: `GlobalExceptionHandler`'a `org.springframework.dao.DataAccessException` (veya spesifik olarak `InvalidDataAccessResourceUsageException`/`PermissionDeniedDataAccessException`) için 403 döndüren bir handler eklemek; `handleGeneral`'ı jenerik "sunucu hatası" mesajıyla döndürüp iç detayı yalnızca loglamak.
- Not: Veri sızıntısı yok (RLS yazmayı yine engelliyor); sorun yanlış HTTP kodu + iç mesaj sızıntısı.

### 2. [ORTA] `POST /partnerships/{id}/payments` rol kısıtı taşımıyor
(Önceki raporda 5.2, **hâlâ geçerli**.) `LedgerController.createAdjustment` `@PreAuthorize("hasAnyRole('LAB_ADMIN','KLINIK_ADMIN')")` ile korunurken `createPayment` (tahsilat) hiçbir rol kısıtı taşımıyor — ortaklığa üye herkes (ör. `ASISTAN`, `LAB_TEKNISYEN`) tahsilat kaydı girebilir. Kasıtlı değilse aynı rol kısıtı eklenmeli.

### 3. [ORTA] `CaseStateMachine.getUserRolesInCase()` aktif org bağlamını yok sayıyor + `findAll()`
(Önceki raporda 5.1, **hâlâ geçerli.**)
```java
List<Membership> memberships = membershipRepository.findAll(); // tüm üyelikler belleğe
```
- `X-Org-Id` (`TenantContext.getOrgId()`) dikkate alınmıyor; kullanıcının ortaklığın **her iki tarafındaki** rolleri toplanıyor. Hem lab hem klinikte üyeliği olan kullanıcı, hangi org başlığıyla gelirse gelsin iki tarafın yetkilerini birden kazanır.
- `findAll()` tüm üyelikleri çekip Java'da filtreliyor — `findByUserId...` ile hedefli sorgu doğru ve ölçeklenebilir olur.

### 4. [DÜŞÜK] Üretim öncesi: depodaki varsayılan sırlar ve DEBUG log
(Önceki raporda 5.3.) `application.yml`'de çalışan varsayılan `JWT_SECRET`, `docker-compose.yml`'de `postgres/postgres`, `init.sql`'de `kopru_app_pass`. Ayrıca `logging.level.org.hibernate.SQL: DEBUG` ve `tr.kopru: DEBUG` üretimde kapatılmalı. Hepsi ortam değişkeninden gelmeli ve varsayılan JWT anahtarı değiştirilmeli.

### 5. [DÜŞÜK] Vaka oluşturma ve kargo için rol kısıtı yok
`DentalCaseController`'daki hiçbir uçta `@PreAuthorize` yok (create/items/shipments dâhil). RLS görünürlüğü ortaklıkla sınırlıyor ama örneğin bir `LAB_TEKNISYEN` vaka oluşturabilir/kargo kaydı girebilir. `gonder`/onay gibi geçişler state machine'de rol ile korunuyor; ancak oluşturma/kargo iş akışına göre rol kısıtı gerekebilir.

### 6. [DÜŞÜK] Yeni özellikler test kapsamı dışında
Testler yalnızca `BusinessLogicAndConstraintTest` + `RlsIsolationTest`. Sonradan eklenen **admin paneli, mesajlaşma, ekler ve tahsilat yetkisi** için test yok. Özellikle (1) ve (2)'yi doğrulayacak testler eklenmesi önerilir.

### 7. [DÜŞÜK] `getActiveOrgType()` X-Org-Id yoksa KLINIK'e düşüyor
`DentalCaseService.getActiveOrgType()`, `TenantContext.getOrgId()` null iken `OrgTipi.KLINIK` varsayıyor. X-Org-Id göndermeyen bir LAB kullanıcısı klinik-biçimli yanıt alır (RLS sayesinde hasta verisi gelmez, sızıntı yok — ama yanıt şekli yanlış). Uçlar X-Org-Id'yi zorunlu kılabilir veya org tipi belirsizse hata dönebilir.

## Küçük notlar
- `uploadAttachment` (frontend) `apiFetch`'i değil doğrudan `fetch` kullandığından **401'de otomatik refresh yok**; yükleme sırasında access token dolarsa istek başarısız olur.
- Liste uçlarında N+1 (klinik tarafında vaka başına `findByCaseIdWithPatient`) — önceki rapor 5.4, geçerli.
- Ek dosya içeriği `bytea` olarak tamamen bellekte tutuluyor (10MB limit ile) — demo için kabul edilebilir, üretimde nesne deposu düşünülebilir.

## Sonuç
Proje sağlam ve olgun; **"proje çalışmıyor" düzeyinde bir hata yok.** Frontend derlemesi temiz. En değerli düzeltmeler sırasıyla: **(1)** RLS-INSERT hata yönetimini 403'e bağlamak + `handleGeneral`'da iç mesaj sızıntısını kesmek, **(2)** `payments` yetkilendirmesi, **(3)** rol çözümünü aktif org bağlamına bağlamak.

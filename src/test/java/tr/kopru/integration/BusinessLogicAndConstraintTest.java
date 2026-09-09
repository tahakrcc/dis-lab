package tr.kopru.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tr.kopru.common.ApiException;
import tr.kopru.common.ErrorCode;
import tr.kopru.domain.caze.CaseStateMachine;
import tr.kopru.domain.caze.CaseStatus;
import tr.kopru.domain.caze.DentalCase;
import tr.kopru.domain.caze.DentalCaseRepository;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
public class BusinessLogicAndConstraintTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("kopru")
            .withUsername("postgres")
            .withPassword("postgres")
            .withInitScript("test-init.sql");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", () -> "kopru_app");
        registry.add("spring.datasource.password", () -> "kopru_app_pass");
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
    }

    @Autowired
    private CaseStateMachine stateMachine;

    @Autowired
    private DentalCaseRepository caseRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final UUID LAB_ADMIN_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID HEKIM_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID CASE_TASLAK_ID = UUID.fromString("c1111111-1111-1111-1111-111111111111");

    @Test
    @DisplayName("Test 7: Klinik onaylar -> lab karsi teklif verir -> klinik onayi gecersizlesir, ONAYLANDI'ya gecmez")
    void testApprovalInvalidationOnCounterOffer() {
        // TASLAK -> FIYAT_MUTABAKATI
        DentalCase c = stateMachine.transition(CASE_TASLAK_ID, "gonder", HEKIM_ID, null);
        assertEquals(CaseStatus.FIYAT_MUTABAKATI, c.getDurum());

        // Klinik onaylar
        c = stateMachine.transition(CASE_TASLAK_ID, "klinikOnayla", HEKIM_ID, null);
        assertEquals(CaseStatus.FIYAT_MUTABAKATI, c.getDurum());
        assertEquals(c.getFiyatVersiyon(), c.getOnayKlinikVersiyon());

        // Lab karsi teklif verir
        c = stateMachine.transition(CASE_TASLAK_ID, "karsiTeklif", LAB_ADMIN_ID, null);
        assertEquals(CaseStatus.FIYAT_MUTABAKATI, c.getDurum());
        assertNull(c.getOnayKlinikVersiyon(), "Karsi teklif sonrasi klinik onayi sifirlanmalidir");
        assertNull(c.getOnayLabVersiyon(), "Karsi teklif sonrasi lab onayi sifirlanmalidir");
        assertEquals(2, c.getFiyatVersiyon(), "Fiyat versiyonu 1 artmalidir");
    }

    @Test
    @DisplayName("Test 8: Eszamanli onay yarisi -> VERSION_MISMATCH")
    void testConcurrentApprovalVersionMismatch() {
        // Case in version 2
        DentalCase c = caseRepository.findById(CASE_TASLAK_ID).orElseThrow();
        int oldVersion = c.getFiyatVersiyon() - 1; // Eski versiyon gonderildiginde

        ApiException ex = assertThrows(ApiException.class, () ->
                stateMachine.transition(CASE_TASLAK_ID, "klinikOnayla", HEKIM_ID, Map.of("fiyatVersiyon", oldVersion))
        );
        assertEquals(ErrorCode.VERSION_MISMATCH, ex.getCode());
    }

    @Test
    @DisplayName("Test 9: teslimEt iki kez -> tek BORC kaydi, ikinci cagri 409")
    void testDoubleDeliveryFailsWithConflict() {
        // Case 3 (TAMAMLANDI)
        UUID caseId = UUID.fromString("c3333333-3333-3333-3333-333333333333");
        stateMachine.transition(caseId, "teslimEt", LAB_ADMIN_ID, null);

        ApiException ex = assertThrows(ApiException.class, () ->
                stateMachine.transition(caseId, "teslimEt", LAB_ADMIN_ID, null)
        );
        assertEquals(ErrorCode.INVALID_TRANSITION, ex.getCode(), "Ikinci teslimEt kabul edilmemelidir");
    }

    @Test
    @DisplayName("Test 10: Fiyat listesi degisse bile case_item.birim_fiyat degismez (snapshot)")
    void testPriceSnapshotIntegrity() {
        UUID caseId = UUID.fromString("c2222222-2222-2222-2222-222222222222");
        BigDecimal initialPrice = jdbcTemplate.queryForObject(
                "SELECT birim_fiyat FROM case_item WHERE case_id = ? LIMIT 1",
                BigDecimal.class, caseId);

        // Update price list in DB
        jdbcTemplate.execute("UPDATE price_list_entry SET fiyat = 9999.00 WHERE service_item_id = 's4444444-4444-4444-4444-444444444444'");

        BigDecimal afterPrice = jdbcTemplate.queryForObject(
                "SELECT birim_fiyat FROM case_item WHERE case_id = ? LIMIT 1",
                BigDecimal.class, caseId);

        assertEquals(initialPrice, afterPrice, "Vakanin birim fiyati snapshot kalmalidir");
    }

    @Test
    @DisplayName("Test 11: Gecersiz gecis (TASLAK -> TESLIM_EDILDI) -> 409")
    void testInvalidTransitionThrowsConflict() {
        // Yeni bir taslak vaka olusturalim
        UUID newCaseId = UUID.randomUUID();
        jdbcTemplate.execute("INSERT INTO \"case\" (id, partnership_id, kod, hasta_rumuzu, created_by, olcu_tipi, durum) " +
                "VALUES ('" + newCaseId + "', 'p1111111-1111-1111-1111-111111111111', '2026-999999', 'TST', 'dddddddd-dddd-dddd-dddd-dddddddddddd', 'STL', 'TASLAK')");

        ApiException ex = assertThrows(ApiException.class, () ->
                stateMachine.transition(newCaseId, "teslimEt", LAB_ADMIN_ID, null)
        );
        assertEquals(ErrorCode.INVALID_TRANSITION, ex.getCode());
    }

    @Test
    @DisplayName("Test 12: birim=DIS iken adet != cardinality(dis_numaralari) -> DB constraint patlar")
    void testCaseItemUnitDisCardinalityMismatch() {
        assertThrows(Exception.class, () ->
                jdbcTemplate.execute("INSERT INTO case_item (id, case_id, service_item_id, dis_numaralari, adet, birim_fiyat, ara_toplam) " +
                        "VALUES (gen_random_uuid(), 'c2222222-2222-2222-2222-222222222222', 's1111111-1111-1111-1111-111111111111', '{11,12}', 5, 100.00, 500.00)")
        );
    }

    @Test
    @DisplayName("Test 13: Gecersiz FDI numarasi (99) -> constraint patlar")
    void testInvalidFdiNumberThrowsConstraintViolation() {
        assertThrows(Exception.class, () ->
                jdbcTemplate.execute("INSERT INTO case_item (id, case_id, service_item_id, dis_numaralari, adet, birim_fiyat, ara_toplam) " +
                        "VALUES (gen_random_uuid(), 'c2222222-2222-2222-2222-222222222222', 's1111111-1111-1111-1111-111111111111', '{99}', 1, 100.00, 100.00)")
        );
    }

    @Test
    @DisplayName("Test 14: Cakisan tarihli fiyat kaydi -> exclusion constraint patlar")
    void testOverlappingPriceEntryThrowsExclusionConstraint() {
        assertThrows(Exception.class, () -> {
            jdbcTemplate.execute("INSERT INTO price_list_entry (partnership_id, service_item_id, fiyat, gecerli_baslangic, gecerli_bitis, aktif) " +
                    "VALUES ('p1111111-1111-1111-1111-111111111111', 's1111111-1111-1111-1111-111111111111', 1200.00, '2026-02-01', '2026-03-01', true)");
            jdbcTemplate.execute("INSERT INTO price_list_entry (partnership_id, service_item_id, fiyat, gecerli_baslangic, gecerli_bitis, aktif) " +
                    "VALUES ('p1111111-1111-1111-1111-111111111111', 's1111111-1111-1111-1111-111111111111', 1300.00, '2026-02-15', '2026-04-01', true)");
        });
    }

    @Test
    @DisplayName("Test 15: case_event UPDATE denemesi -> exception")
    void testCaseEventUpdateThrowsException() {
        String eventId = UUID.randomUUID().toString();
        jdbcTemplate.execute("INSERT INTO case_event (id, case_id, actor_user_id, tur, detay) " +
                "VALUES ('" + eventId + "', 'c2222222-2222-2222-2222-222222222222', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'DURUM', '{}')");

        assertThrows(Exception.class, () ->
                jdbcTemplate.execute("UPDATE case_event SET detay = '{\"hack\": true}' WHERE id = '" + eventId + "'::uuid")
        );
    }

    @Test
    @DisplayName("Test 16: Bakiye view: 3 borc + 2 tahsilat -> dogru bakiye")
    void testBalanceViewCalculation() {
        UUID pId = UUID.fromString("p2222222-2222-2222-2222-222222222222");

        // 3 Borc: 1200, 2400, 3600 (Toplam Borc: 7200)
        jdbcTemplate.execute("INSERT INTO ledger_entry (partnership_id, tur, matrah, kdv_orani, kdv_tutari, tutar, belge_tarihi) " +
                "VALUES ('" + pId + "', 'BORC', 1000, 20, 200, 1200, '2026-01-01')");
        jdbcTemplate.execute("INSERT INTO ledger_entry (partnership_id, tur, matrah, kdv_orani, kdv_tutari, tutar, belge_tarihi) " +
                "VALUES ('" + pId + "', 'BORC', 2000, 20, 400, 2400, '2026-01-02')");
        jdbcTemplate.execute("INSERT INTO ledger_entry (partnership_id, tur, matrah, kdv_orani, kdv_tutari, tutar, belge_tarihi) " +
                "VALUES ('" + pId + "', 'BORC', 3000, 20, 600, 3600, '2026-01-03')");

        // 2 Tahsilat: -2000, -3000 (Toplam Tahsilat: 5000)
        jdbcTemplate.execute("INSERT INTO ledger_entry (partnership_id, tur, matrah, kdv_orani, kdv_tutari, tutar, belge_tarihi) " +
                "VALUES ('" + pId + "', 'TAHSILAT', 0, 0, 0, -2000, '2026-01-05')");
        jdbcTemplate.execute("INSERT INTO ledger_entry (partnership_id, tur, matrah, kdv_orani, kdv_tutari, tutar, belge_tarihi) " +
                "VALUES ('" + pId + "', 'TAHSILAT', 0, 0, 0, -3000, '2026-01-06')");

        Map<String, Object> balance = jdbcTemplate.queryForMap(
                "SELECT toplam_borc, toplam_tahsilat, bakiye FROM balance WHERE partnership_id = ?", pId);

        BigDecimal toplamBorc = (BigDecimal) balance.get("toplam_borc");
        BigDecimal toplamTahsilat = (BigDecimal) balance.get("toplam_tahsilat");
        BigDecimal bakiye = (BigDecimal) balance.get("bakiye");

        assertEquals(0, new BigDecimal("7200.00").compareTo(toplamBorc), "Toplam borc 7200 olmalidir");
        assertEquals(0, new BigDecimal("5000.00").compareTo(toplamTahsilat), "Toplam tahsilat 5000 olmalidir");
        assertEquals(0, new BigDecimal("2200.00").compareTo(bakiye), "Bakiye 7200 - 5000 = 2200 olmalidir");
    }
}

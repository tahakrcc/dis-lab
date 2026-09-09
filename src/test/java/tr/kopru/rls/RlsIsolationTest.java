package tr.kopru.rls;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
@SpringBootTest
public class RlsIsolationTest {

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
    private JdbcTemplate jdbcTemplate;

    private static final String CLINIC_A_USER = "dddddddd-dddd-dddd-dddd-dddddddddddd"; // Hekim (Inci)
    private static final String CLINIC_B_USER = "cccccccc-cccc-cccc-cccc-cccccccccccc";
    private static final String LAB_USER = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"; // Lab Admin (DentArt)

    @Test
    @DisplayName("Test 6: app.user_id set edilmeden sorgu -> 0 satir (hata degil, bos)")
    void testNoUserContextReturnsZeroRows() {
        // Clear user_id
        jdbcTemplate.execute("SELECT set_config('app.user_id', '', false)");

        List<Map<String, Object>> cases = jdbcTemplate.queryForList("SELECT * FROM \"case\"");
        assertEquals(0, cases.size(), "app.user_id bosken RLS 0 satir dondurmelidir");

        List<Map<String, Object>> patients = jdbcTemplate.queryForList("SELECT * FROM patient");
        assertEquals(0, patients.size(), "app.user_id bosken patient 0 satir dondurmelidir");
    }

    @Test
    @DisplayName("Test 2: Lab kullanicisi patient tablosunu sorgular -> 0 satir")
    void testLabUserCannotSeePatient() {
        jdbcTemplate.execute("SELECT set_config('app.user_id', '" + LAB_USER + "', false)");
        List<Map<String, Object>> patients = jdbcTemplate.queryForList("SELECT * FROM patient");
        assertEquals(0, patients.size(), "Lab kullanicisi patient tablosunu HICBIR KOSULDA goremez");
    }

    @Test
    @DisplayName("Test 3: Lab kullanicisi case_patient_link tablosunu sorgular -> 0 satir")
    void testLabUserCannotSeeCasePatientLink() {
        jdbcTemplate.execute("SELECT set_config('app.user_id', '" + LAB_USER + "', false)");
        List<Map<String, Object>> links = jdbcTemplate.queryForList("SELECT * FROM case_patient_link");
        assertEquals(0, links.size(), "Lab kullanicisi case_patient_link tablosunu HICBIR KOSULDA goremez");
    }

    @Test
    @DisplayName("Test 4: Lab X baska bir Lab Y'nin service_item'larini goremez")
    void testLabCannotSeeOtherLabCatalog() {
        // Add Lab Y
        String labYId = UUID.randomUUID().toString();
        jdbcTemplate.execute("INSERT INTO organization (id, tip, ad) VALUES ('" + labYId + "', 'LAB', 'Diger Lab')");
        jdbcTemplate.execute("INSERT INTO service_item (lab_id, ad, birim) VALUES ('" + labYId + "', 'Gizli Urun', 'ADET')");

        // Lab X user queries
        jdbcTemplate.execute("SELECT set_config('app.user_id', '" + LAB_USER + "', false)");
        List<Map<String, Object>> items = jdbcTemplate.queryForList(
                "SELECT * FROM service_item WHERE lab_id = '" + labYId + "'::uuid");

        assertEquals(0, items.size(), "Lab baska bir labin service_item'larini goremez");
    }

    @Test
    @DisplayName("Test 5: Pasif ortaklik uzerinden veri erisimi -> 0 satir")
    void testPassivePartnershipIsInvisible() {
        String pPassive = UUID.randomUUID().toString();
        jdbcTemplate.execute("INSERT INTO partnership (id, lab_id, clinic_id, durum) " +
                "VALUES ('" + pPassive + "', '11111111-1111-1111-1111-111111111111', '33333333-3333-3333-3333-333333333333', 'PASIF') " +
                "ON CONFLICT (lab_id, clinic_id) DO UPDATE SET durum = 'PASIF'");

        jdbcTemplate.execute("SELECT set_config('app.user_id', '" + LAB_USER + "', false)");
        List<Map<String, Object>> partnerships = jdbcTemplate.queryForList(
                "SELECT * FROM partnership WHERE id = '" + pPassive + "'::uuid");

        assertEquals(0, partnerships.size(), "Pasif ortaklik RLS tarafindan filtrelenmelidir");
    }

    @Test
    @DisplayName("Test 1: Klinik kullanicisi baska klinigin bagimsiz vakalarini goremez")
    void testClinicCannotSeeOtherClinicCases() {
        // Create user and clinic C
        String clinicCId = UUID.randomUUID().toString();
        String userCId = UUID.randomUUID().toString();
        jdbcTemplate.execute("INSERT INTO organization (id, tip, ad) VALUES ('" + clinicCId + "', 'KLINIK', 'Klinik C')");
        jdbcTemplate.execute("INSERT INTO app_user (id, ad, email, parola_hash, kullanici_adi) VALUES ('" + userCId + "', 'Dr C', 'c@c.com', 'hash', 'drc')");
        jdbcTemplate.execute("INSERT INTO membership (user_id, org_id, rol) VALUES ('" + userCId + "', '" + clinicCId + "', 'HEKIM')");

        // User C queries existing cases of Clinic A
        jdbcTemplate.execute("SELECT set_config('app.user_id', '" + userCId + "', false)");
        List<Map<String, Object>> cases = jdbcTemplate.queryForList("SELECT * FROM \"case\"");

        assertEquals(0, cases.size(), "Klinik baska bir klinigin vakalarini goremez");
    }
}

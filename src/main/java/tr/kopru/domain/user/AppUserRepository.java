package tr.kopru.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * Pre-auth login lookup. app_user tablosu FORCE RLS altinda oldugundan ve
     * login aninda henuz app.user_id set edilmedigi icin normal sorgu 0 satir doner.
     * SECURITY DEFINER app.find_login fonksiyonu yalnizca ilgili kullanici adina ait
     * (id, parola_hash) satirini dar kapsamda dondurur.
     *
     * @return her satir: [0]=UUID id, [1]=String parola_hash
     */
    @Query(value = "SELECT id, parola_hash FROM app.find_login(CAST(:kullaniciAdi AS citext))",
            nativeQuery = true)
    List<Object[]> findLoginRaw(@Param("kullaniciAdi") String kullaniciAdi);

    /**
     * Uye ekleme icin kullanici adiyla id cozumu. Hedef kullanici henuz org uyesi
     * olmadiginda app_user RLS altinda gorunmez; app.find_user_id_by_username
     * SECURITY DEFINER fonksiyonu id'yi dar kapsamda dondurur.
     */
    @Query(value = "SELECT id FROM app.find_user_id_by_username(CAST(:kullaniciAdi AS citext))",
            nativeQuery = true)
    List<UUID> findUserIdByUsername(@Param("kullaniciAdi") String kullaniciAdi);
}

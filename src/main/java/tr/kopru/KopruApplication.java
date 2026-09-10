package tr.kopru;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
// Transaction advice'ını dış katman (order 0) yap ki RLS bağlamını kuran
// TenantAspect (order 100) işlemin İÇİNDE, aynı bağlantıda çalışsın.
@EnableTransactionManagement(order = 0)
public class KopruApplication {

    public static void main(String[] args) {
        SpringApplication.run(KopruApplication.class, args);
    }
}

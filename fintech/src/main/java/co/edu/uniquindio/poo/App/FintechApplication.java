package co.edu.uniquindio.poo.App;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "co.edu.uniquindio.poo")
@EnableScheduling
public class FintechApplication {

    public static void main(String[] args) {
        SpringApplication.run(FintechApplication.class, args);
    }

}
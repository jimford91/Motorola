package ford.james.motorola;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class MotorolaApplication {

	public static void main(String[] args) {
		SpringApplication.run(MotorolaApplication.class, args);
	}

}

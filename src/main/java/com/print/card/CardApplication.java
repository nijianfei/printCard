package com.print.card;

import com.print.card.jna.DllLoadIn;
import com.print.card.jna.KeyHook;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Map;

@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties
public class CardApplication {

	public static void main(String[] args) {
		Map<String, String> env = System.getenv();
//		for (String envName : env.keySet()) {
//			System.out.format("%s=%s%n", envName, env.get(envName));
//		}
		try {
			KeyHook keyHook = KeyHook.instance;
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			DllLoadIn instance1 = DllLoadIn.instance;
		} catch (Exception e) {
			e.printStackTrace();
		}
		SpringApplicationBuilder builder = new SpringApplicationBuilder(CardApplication.class);
		builder.headless(false).run(args);
//		SpringApplication.run(CardApplication.class, args);

	}

}

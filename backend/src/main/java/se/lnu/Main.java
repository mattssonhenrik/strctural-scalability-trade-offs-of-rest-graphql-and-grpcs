package se.lnu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import se.lnu.data.Datagenerator;

@SpringBootApplication
public class Main {
	public static void main(String[] args) {
		SpringApplication.run(Main.class, args);
		System.out.println("HELLO ERIK");
		Datagenerator gen = new Datagenerator(2, 2, 2, 42);
		gen.printStats();
		try {
			String json = gen.generateJson();
			System.out.println(json);
		} catch (Exception e) {
			System.err.println(e);
		}

		

	}

}

package cn.org.autumn;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;


@SpringBootApplication
@MapperScan(basePackages = {"cn.org.autumn.modules.*.dao","cn.org.autumn.table.dao",})
public class AutumnApplication extends SpringBootServletInitializer {

	public static void main(String[] args) {
		SpringApplication.run(AutumnApplication.class, args);
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(AutumnApplication.class);
	}
}

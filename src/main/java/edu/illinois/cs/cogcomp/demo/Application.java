package edu.illinois.cs.cogcomp.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Created by ctsai12 on 4/25/16.
 */
@SpringBootApplication
public class Application {
    private static Logger logger = LoggerFactory.getLogger(Application.class);
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Application.class);

        if(args.length == 0){
            logger.error("A configuration file is required (specify it as the first argument)");
            System.exit(-1);
        }
        DemoController.config = args[0];
        app.run(args);
    }
}

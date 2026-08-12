package de.mimosa_dev.MealPlanner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// @EnableScheduling: step 12's TelegramNotificationScheduler (FR-81 spoilage/shopping-reminder
// checks) is the first user of Spring's @Scheduled anywhere in this codebase.
@SpringBootApplication
@EnableScheduling
public class MealPlannerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MealPlannerApplication.class, args);
    }

}

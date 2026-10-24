package com.metashield;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * MetaShield: Intelligent Privacy Framework for Metadata Security and Risk-Based Removal
 * Developed at Malla Reddy University
 * Authors: Charan Konda (Roll No: 2511CS040113) & J. Naga Phani (Roll No: 2511CS040132)
 * Batch: CS BETA - 17
 */
@SpringBootApplication
@EnableScheduling
public class MetaShieldApplication {

    public static void main(String[] args) {
        SpringApplication.run(MetaShieldApplication.class, args);
    }
}

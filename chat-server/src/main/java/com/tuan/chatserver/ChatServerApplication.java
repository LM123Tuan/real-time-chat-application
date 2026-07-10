package com.tuan.chatserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Điểm khởi động chính của ứng dụng Chat Server.
 *
 * <p>Spring Boot sẽ khởi tạo ApplicationContext, thực hiện Component Scan,
 * Auto Configuration và khởi động Embedded Tomcat.</p>
 *
 * @author Tuan
 */

@SpringBootApplication
public class ChatServerApplication {
    /**
     * Khởi động ứng dụng Spring Boot.
     *
     * @param args các tham số truyền từ dòng lệnh
     */
    public static void main(String[] args) {
        SpringApplication.run(ChatServerApplication.class, args);
    }
}
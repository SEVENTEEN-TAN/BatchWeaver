package com.batchweaver.demo.service.impl;

import com.batchweaver.demo.service.MockMailSender;
import org.springframework.stereotype.Component;

/**
 * 日志式 Mock 邮件发送服务
 * <p>
 * 仅通过日志输出邮件内容，不发送真实邮件
 */
@Component
public class LogOnlyMockMailSender implements MockMailSender {

    @Override
    public void sendSuccess(String subject, String body) {
        System.out.println("==============================================");
        System.out.println("[SUCCESS MAIL]");
        System.out.println("Subject: " + subject);
        System.out.println("Body: " + body);
        System.out.println("==============================================");
    }

    @Override
    public void sendFailure(String subject, String body) {
        System.out.println("==============================================");
        System.out.println("[FAILURE MAIL]");
        System.out.println("Subject: " + subject);
        System.out.println("Body: " + body);
        System.out.println("==============================================");
    }

    @Override
    public void sendPartial(String subject, String body) {
        System.out.println("==============================================");
        System.out.println("[PARTIAL SUCCESS MAIL]");
        System.out.println("Subject: " + subject);
        System.out.println("Body: " + body);
        System.out.println("==============================================");
    }
}

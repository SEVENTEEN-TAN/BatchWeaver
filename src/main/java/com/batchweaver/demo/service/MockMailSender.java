package com.batchweaver.demo.service;

/**
 * Mock 邮件发送服务接口
 * <p>
 * 用于 Job5 复杂工作流中的邮件通知
 */
public interface MockMailSender {

    /**
     * 发送成功邮件
     *
     * @param subject 邮件主题
     * @param body    邮件内容
     */
    void sendSuccess(String subject, String body);

    /**
     * 发送失败邮件
     *
     * @param subject 邮件主题
     * @param body    邮件内容
     */
    void sendFailure(String subject, String body);

    /**
     * 发送部分成功邮件
     *
     * @param subject 邮件主题
     * @param body    邮件内容
     */
    void sendPartial(String subject, String body);
}

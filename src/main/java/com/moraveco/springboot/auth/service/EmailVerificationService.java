package com.moraveco.springboot.auth.service;

import com.moraveco.springboot.auth.entity.Login;
import com.moraveco.springboot.auth.entity.Register;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailVerificationService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendVerificationEmail(Register user, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String verificationUrl = "http://localhost:3000/verify-email?token=" + token;

            helper.setTo(user.getEmail());
            helper.setSubject("Verify Your Email Address");
            helper.setText(buildVerificationEmailHtml(user.getName(), verificationUrl), true);

            mailSender.send(message);
        } catch (MessagingException e) {
            System.err.println("Failed to send verification email: " + e.getMessage());
            // Fallback to simple email
            sendSimpleVerificationEmail(user, token);
        }
    }

    private void sendSimpleVerificationEmail(Register user, String token) {
        String verificationUrl = "http://localhost:3000/verify-email?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Verify Your Email Address");
        message.setText("Hello " + user.getName() + ",\n\n" +
                "Thank you for registering! Please verify your email address by clicking the link below:\n\n" +
                verificationUrl + "\n\n" +
                "This link will expire in 24 hours.\n\n" +
                "If you did not create an account, please ignore this email.\n\n" +
                "Best regards,\nYour App Team");

        mailSender.send(message);
    }

    public void sendPasswordResetEmail(Login user, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String resetUrl = "http://localhost:3000/reset-password?token=" + token;

            helper.setTo(user.getEmail());
            helper.setSubject("Reset Your Password");
            helper.setText(buildPasswordResetEmailHtml(resetUrl), true);

            mailSender.send(message);
        } catch (MessagingException e) {
            System.err.println("Failed to send password reset email: " + e.getMessage());
            // Fallback to simple email
            sendSimplePasswordResetEmail(user, token);
        }
    }

    private void sendSimplePasswordResetEmail(Login user, String token) {
        String resetUrl = "http://localhost:3000/reset-password?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Reset Your Password");
        message.setText("Hello,\n\n" +
                "You requested to reset your password. Click the link below to reset it:\n\n" +
                resetUrl + "\n\n" +
                "This link will expire in 1 hour.\n\n" +
                "If you did not request a password reset, please ignore this email.\n\n" +
                "Best regards,\nYour App Team");

        mailSender.send(message);
    }

    private String buildVerificationEmailHtml(String name, String verificationUrl) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                ".container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                ".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }" +
                ".content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }" +
                ".button { display: inline-block; padding: 12px 30px; background: #667eea; color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }" +
                ".footer { text-align: center; margin-top: 20px; color: #777; font-size: 12px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<h1>Welcome to Our App!</h1>" +
                "</div>" +
                "<div class='content'>" +
                "<p>Hello " + name + ",</p>" +
                "<p>Thank you for registering! We're excited to have you on board.</p>" +
                "<p>To complete your registration and verify your email address, please click the button below:</p>" +
                "<div style='text-align: center;'>" +
                "<a href='" + verificationUrl + "' class='button'>Verify Email Address</a>" +
                "</div>" +
                "<p>Or copy and paste this link into your browser:</p>" +
                "<p style='word-break: break-all; color: #667eea;'>" + verificationUrl + "</p>" +
                "<p><strong>This link will expire in 24 hours.</strong></p>" +
                "<p>If you did not create an account, please ignore this email.</p>" +
                "</div>" +
                "<div class='footer'>" +
                "<p>Best regards,<br>Your App Team</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    private String buildPasswordResetEmailHtml(String resetUrl) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                ".container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                ".header { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }" +
                ".content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }" +
                ".button { display: inline-block; padding: 12px 30px; background: #f5576c; color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }" +
                ".warning { background: #fff3cd; border-left: 4px solid #ffc107; padding: 10px; margin: 20px 0; }" +
                ".footer { text-align: center; margin-top: 20px; color: #777; font-size: 12px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<h1>Reset Your Password</h1>" +
                "</div>" +
                "<div class='content'>" +
                "<p>Hello,</p>" +
                "<p>You requested to reset your password. Click the button below to create a new password:</p>" +
                "<div style='text-align: center;'>" +
                "<a href='" + resetUrl + "' class='button'>Reset Password</a>" +
                "</div>" +
                "<p>Or copy and paste this link into your browser:</p>" +
                "<p style='word-break: break-all; color: #f5576c;'>" + resetUrl + "</p>" +
                "<div class='warning'>" +
                "<strong>⚠️ Important:</strong> This link will expire in 1 hour for security reasons." +
                "</div>" +
                "<p>If you did not request a password reset, please ignore this email. Your password will remain unchanged.</p>" +
                "</div>" +
                "<div class='footer'>" +
                "<p>Best regards,<br>Your App Team</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
}
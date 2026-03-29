package com.example.backendspringboot.services;

import com.example.backendspringboot.model.Driver;
import com.example.backendspringboot.model.EmailDetails;
import com.example.backendspringboot.model.Passenger;
import com.example.backendspringboot.services.interfaces.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender javaMailSender;

    @Value("${spring.mail.username}") private String sender;

    @Override
    public String sendsSimpleMail(EmailDetails details) {
        try {
            // Creating a simple mail message
            SimpleMailMessage mailMessage = new SimpleMailMessage();

            mailMessage.setFrom(sender);
            mailMessage.setTo(details.getRecipient());
            mailMessage.setText(details.getMsgBody());
            mailMessage.setSubject(details.getSubject());

            // Sending the email
            javaMailSender.send(mailMessage);
            return "Mail Sent Successfully...";
        }
        catch (Exception e) {
            return "Error while Sending Mail";
        }
    }

    @Override
    public String sendMailWithAttachment(EmailDetails details) {
        // Creating a MimeMessage
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper mimeMessageHelper;

        try {
            mimeMessageHelper = new MimeMessageHelper(mimeMessage, true);
            mimeMessageHelper.setFrom(sender);
            mimeMessageHelper.setTo(details.getRecipient());
            mimeMessageHelper.setText(details.getMsgBody());
            mimeMessageHelper.setSubject(details.getSubject());

            // Adding attachment
            FileSystemResource file = new FileSystemResource(new File(details.getAttachment()));

            mimeMessageHelper.addAttachment(file.getFilename(), file);

            // Sending mail
            javaMailSender.send(mimeMessage);
            return "Mail sent successfully";
        }
        catch(MessagingException e) {
            return "Error while sending mail";
        }
    }

    @Override
    public void sendActivationEmail(Passenger passenger) {
        try {
            String activationLink = "http://localhost:4200/activate-account?token=" + passenger.getActivationToken();
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(passenger.getEmail());
            helper.setSubject("Activate your account");
            helper.setText(
                    "<p>Hello " + passenger.getName() + ",</p>" +
                            "<p>Please click the link below to activate your account (valid for 24h):</p>" +
                            "<a href=\"" + activationLink + "\">Activate Account</a>",
                    true
            );
            javaMailSender.send(message);
            System.out.println(activationLink);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send activation email", e);
        }
    }

    @Override
    public void sendDriverRegistrationEmail(Driver driver, String registrationLink) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(driver.getEmail());
            helper.setSubject("Complete your registration");
            helper.setText(
                    "<p>Hello " + driver.getName() + ",</p>" +
                            "<p>Welcome to ClickAndDrive! Please complete your registration by setting your password:</p>" +
                            "<p><a href=\"" + registrationLink + "\">Complete Registration</a></p>" +
                            "<p>If the button does not work, open this address:<br/>" +
                            "<a href=\"" + registrationLink + "\">" + registrationLink + "</a></p>" +
                            "<p>This link will expire in 24 hours.</p>" +
                            "<p>Best regards,<br/>ClickAndDrive Team</p>",
                    true
            );
            javaMailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send driver registration email", e);
        }
    }
}

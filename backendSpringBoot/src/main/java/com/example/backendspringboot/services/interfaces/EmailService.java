package com.example.backendspringboot.services.interfaces;

import com.example.backendspringboot.model.Driver;
import com.example.backendspringboot.model.EmailDetails;
import com.example.backendspringboot.model.Passenger;

public interface EmailService {
    // Method to send a simple mail
    String sendsSimpleMail(EmailDetails details);

    // Send mail with attachment
    String sendMailWithAttachment(EmailDetails details);
    void sendActivationEmail(Passenger passenger);
    // Mail for completing registration process
    void sendDriverRegistrationEmail(Driver driver, String registrationLink);
    void sendRideTrackingEmail(String recipient, String subject,
                               String message, String trackingLink);
}

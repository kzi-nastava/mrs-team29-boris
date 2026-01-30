package com.example.demo.services.interfaces;

import com.example.demo.model.Driver;
import com.example.demo.model.EmailDetails;
import com.example.demo.model.Passenger;

public interface EmailService {
    // Method to send a simple mail
    String sendsSimpleMail(EmailDetails details);

    // Send mail with attachment
    String sendMailWithAttachment(EmailDetails details);
    void sendActivationEmail(Passenger passenger);
    // Mail for completing registration process
    void sendDriverRegistrationEmail(Driver driver, String registrationLink);
}

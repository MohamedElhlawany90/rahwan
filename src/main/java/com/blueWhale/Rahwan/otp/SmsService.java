//package com.blueWhale.Rahwan.otp;
//
//import com.twilio.Twilio;
//import com.twilio.rest.api.v2010.account.Message;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//@Service
//public class SmsService {
//
//    @Value("${twilio.account.sid}")
//    private String accountSid;
//
//    @Value("${twilio.auth.token}")
//    private String authToken;
//
//    @Value("${twilio.phone.number}")
//    private String twilioPhoneNumber;
//
//    public void sendSms(String phoneNumber, String message) {
//        try {
//            Twilio.init(accountSid, authToken);
//            Message.creator(
//                    new com.twilio.type.PhoneNumber("+" + phoneNumber),
//                    new com.twilio.type.PhoneNumber(twilioPhoneNumber),
//                    message
//            ).create();
//        } catch (Exception e) {
//            System.err.println("Failed to send SMS: " + e.getMessage());
//        }
//    }
//}
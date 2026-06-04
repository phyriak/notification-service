package com.phyriak.newsletter;

import com.phyriak.mapper.NotificationMapper;
import com.phyriak.model.NotificationEntity;
import com.phyriak.repository.NotificationRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class NewsLetterListener {

    private final JavaMailSender mailSender;
    private final NotificationMapper notificationMapper;
    private final NotificationRepository notificationRepository;

    @Async("taskExecutor")
    @EventListener
    public void handleNewsletterSignup(NewsLetterSignupEvent event) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        System.out.println("Sending welcome email to: " + event.getEmail());
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        // helper.setFrom("piotr_hyriak@op.pl");
        helper.setTo(event.getEmail());
        helper.setSubject("Welcome!");
        helper.setText("Thanks for signing up.");
        mailSender.send(message);
        //save to db
        NotificationEntity entity = notificationMapper.toEntity(event);
     //   notificationRepository.save(entity);
    }
}

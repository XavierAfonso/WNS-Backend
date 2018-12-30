package ch.heigvd.wns.controller;

import ch.heigvd.wns.model.mongo.Followers;
import ch.heigvd.wns.model.mongo.Notification;
import ch.heigvd.wns.model.mongo.User;
import ch.heigvd.wns.repository.mongo.NotificationRepository;
import ch.heigvd.wns.repository.mongo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @RequestMapping(value = "{user_id}", method = { RequestMethod.GET }, produces = "application/json")
    public @ResponseBody
    List<Notification> getNotification(@PathVariable String user_id) {
        Optional<User> u = userRepository.findById(user_id);
        if (!u.isPresent()) {
            return null;
        }
        return notificationRepository.findByRecipient(u.get());
    }

}

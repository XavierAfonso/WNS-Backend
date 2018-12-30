package ch.heigvd.wns.repository.mongo;

import ch.heigvd.wns.model.mongo.Followers;
import ch.heigvd.wns.model.mongo.Notification;
import ch.heigvd.wns.model.mongo.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.io.Serializable;
import java.util.List;

public interface NotificationRepository extends PagingAndSortingRepository<Notification, Serializable> {
    List<Notification> findByRecipient(User recipient);
    List<Notification> findBySender(User user);

    List<Notification> findAll();
}

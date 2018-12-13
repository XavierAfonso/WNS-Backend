package ch.heigvd.wns.repository.mongo;

import ch.heigvd.wns.model.User;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends PagingAndSortingRepository<User, Serializable> {
    User findByEmail(String email);
    Optional<User> findById(String id);
    List<User> findAll();
}
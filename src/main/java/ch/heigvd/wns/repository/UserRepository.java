package ch.heigvd.wns.repository;

import ch.heigvd.wns.model.User;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends ElasticsearchRepository<User, String> {

    User findByEmail(String email);
    Optional<User> findById(Long id);
    List<User> findAll();
}
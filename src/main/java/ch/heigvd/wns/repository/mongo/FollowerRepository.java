package ch.heigvd.wns.repository.mongo;

import ch.heigvd.wns.model.mongo.Followers;
import ch.heigvd.wns.model.mongo.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.io.Serializable;
import java.util.List;

public interface FollowerRepository extends PagingAndSortingRepository<Followers, Serializable> {
    List<Followers> findByFrom(User u, Pageable pageable);
    List<Followers> findByTo(User u, Pageable pageable);
    Followers findByFromAndTo(User from, User to);
    List<Followers> findByFrom(User u);

    List<Followers> findAll();
}

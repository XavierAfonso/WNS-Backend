package ch.heigvd.wns.repository.mongo;

import ch.heigvd.wns.model.mongo.Followers;
import ch.heigvd.wns.model.mongo.Post;
import ch.heigvd.wns.model.mongo.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.io.Serializable;
import java.util.List;

public interface PostRepository extends PagingAndSortingRepository<Post, Serializable> {
    List<Post> findByCreatedByOrderByCreatedDateDesc(User user);
    List<Post> findByBookId(String bookId);

    List<Post> findAll();
}

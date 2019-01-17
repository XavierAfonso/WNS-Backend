package ch.heigvd.wns.repository.elasticsearch;

import ch.heigvd.wns.model.elasticsearch.Book;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends ElasticsearchRepository<Book, String> {
    List<Book> findAllByAuthorIdOrderByCreatedDateDesc(String authorId);
    List<Book> findAllByTagsLike(String[] tags);
    List<Book> findByIdIn(List<String> ids);

}
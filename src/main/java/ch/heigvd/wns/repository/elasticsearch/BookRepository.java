package ch.heigvd.wns.repository.elasticsearch;

import ch.heigvd.wns.model.Book;
import ch.heigvd.wns.model.User;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends ElasticsearchRepository<Book, String> {

}
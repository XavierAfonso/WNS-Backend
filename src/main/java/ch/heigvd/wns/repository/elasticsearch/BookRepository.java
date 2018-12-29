package ch.heigvd.wns.repository.elasticsearch;

import ch.heigvd.wns.model.elasticsearch.Book;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookRepository extends ElasticsearchRepository<Book, String> {

}
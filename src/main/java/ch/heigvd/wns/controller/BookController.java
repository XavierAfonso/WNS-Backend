package ch.heigvd.wns.controller;

import ch.heigvd.wns.dto.PostDTO;
import ch.heigvd.wns.model.elasticsearch.Book;
import ch.heigvd.wns.model.mongo.User;
import ch.heigvd.wns.repository.elasticsearch.BookRepository;
import ch.heigvd.wns.repository.mongo.FollowerRepository;
import ch.heigvd.wns.repository.mongo.UserRepository;
import ch.heigvd.wns.security.jwt.AuthenticatedUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHost;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/books")
public class BookController {

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FollowerRepository followerRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @RequestMapping(method = { RequestMethod.POST }, produces = "application/json")
    public @ResponseBody
    ResponseEntity create(@RequestBody PostDTO postDTO) {
        AuthenticatedUser auth = (AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication();

        // Create the Book object
        Book book = new Book();
        UUID uuid = UUID.randomUUID();
        book.setId(uuid.toString());
        book.setTitle(postDTO.getTitle());
        book.setPostDescription(postDTO.getPostDescription());
        book.setBookContent(postDTO.getBookContent());
        book.setAuthorId(auth.getName());
        book.setTags(postDTO.getTags());

        // Convert to JSON
        ObjectMapper mapper = new ObjectMapper();
        String bookJson = "";
        try {
            bookJson = mapper.writeValueAsString(book);
        } catch (JsonProcessingException e) {
            System.out.println(e);
        }

        // Make the custom request for calling the pipeline
        Request request = new Request("PUT", "wns/books/" + uuid + "?pipeline=attachment");
        request.setJsonEntity(bookJson);
        // TODO: change static localhost by the ENV variables
        RestClient restClient = RestClient.builder(
                new HttpHost("localhost", 9200, "http"),
                new HttpHost("localhost", 9201, "http")).build();
        try {
            System.out.println(request.getEndpoint());
            Response response = restClient.performRequest(request);
            // Check if we indexed right the file
            if (response.getStatusLine().getStatusCode() == 201) {
                return new ResponseEntity(HttpStatus.CREATED);
            } else {
                return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return new ResponseEntity(HttpStatus.CREATED);
    }

    @RequestMapping(value = "{book_id}", method = { RequestMethod.GET }, produces = "application/json")
    public @ResponseBody
    Book getBook(@PathVariable("book_id") String bookId) {
        Optional<Book> book = bookRepository.findById(bookId);
        return book.isPresent() ? book.get() : null;
    }

    @RequestMapping(method = { RequestMethod.GET }, produces = "application/json")
    public @ResponseBody
    List<Book> getUserBooks(@RequestParam("id_user") String id) {
        AuthenticatedUser auth = (AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByEmail(auth.getName());
        if (user == null) {
            return null;
        }
        return bookRepository.findAllByAuthorIdOrderByCreatedDateDesc(user.getEmail());
    }

    @RequestMapping(value = "wall/{id_user}", method = { RequestMethod.GET }, produces = "application/json")
    public @ResponseBody
    List<Book> wall(@RequestParam("id_user") String id) {
        // Get one or two latest post of following users
        return null;
    }

    @RequestMapping(value = "search", method = { RequestMethod.GET }, produces = "application/json")
    public @ResponseBody
    List<Book> search(@RequestParam("tags") Optional<String[]> tags, @RequestParam("sentence") Optional<String> sentence) {
        // Get one or two latest post of following users
        return null;
    }



}
package ch.heigvd.wns.controller;

import ch.heigvd.wns.dto.PostDTO;
import ch.heigvd.wns.dto.SearchQuery;
import ch.heigvd.wns.model.elasticsearch.Book;
import ch.heigvd.wns.model.mongo.User;
import ch.heigvd.wns.repository.elasticsearch.BookRepository;
import ch.heigvd.wns.repository.mongo.FollowerRepository;
import ch.heigvd.wns.repository.mongo.UserRepository;
import ch.heigvd.wns.security.jwt.AuthenticatedUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.elasticsearch.index.query.QueryBuilders.*;
import org.apache.http.HttpHost;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.index.query.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.util.*;

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

    private final static String APP_PDF_FOLDER = System.getProperty("user.home") + File.separator + "wns" + File.separator + "PDF" + File.separator;


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

    @RequestMapping(value = "{book_id}/pdf", method = { RequestMethod.GET }, produces = "application/pdf")
    public ResponseEntity<InputStreamResource> getPdfBook(@PathVariable("book_id") String id)
            throws IOException {

        Optional<Book> optionalBook = bookRepository.findById(id);

        if (optionalBook.isPresent()) {
            Book book = optionalBook.get();

            File directory = new File(APP_PDF_FOLDER);
            if (! directory.exists()){
                directory.mkdirs();
                // If you require it to make the entire directory path including parents,
                // use directory.mkdirs(); here instead.
            }
            File pdfFile = new File(APP_PDF_FOLDER + book.getTitle());

            int contentLength = 0;
            if (!pdfFile.exists() && pdfFile.createNewFile()) {
                FileOutputStream fos = new FileOutputStream(pdfFile.getPath());
                Base64.Decoder decoder = Base64.getDecoder();
                fos.write(decoder.decode(book.getBookContent()));
                fos.close();
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/pdf"));
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, PUT");
            headers.add("Access-Control-Allow-Headers", "Content-Type");
            headers.add("Content-Disposition", "filename=" + book.getTitle());
            headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
            headers.add("Pragma", "no-cache");
            headers.add("Expires", "0");

            headers.setContentLength(pdfFile.length());
            InputStream stream = new FileInputStream(pdfFile);

            ResponseEntity<InputStreamResource> response = new ResponseEntity<InputStreamResource>(
                    new InputStreamResource(stream), headers, HttpStatus.OK);
            return response;
        }
        return null;
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
    List<Book> search(@RequestBody SearchQuery searchQuery) {
        String key = "";
        String content = "";
        Boolean fuzzyQuery = false;

        if (searchQuery.getBookContent() != null) {
            key = "attachment.content";
            content =  searchQuery.getBookContent();
            fuzzyQuery = true;
        } else if (searchQuery.getTags() != null) {
            key = "tags";
            content =  Arrays.toString(searchQuery.getTags());
        } else if (searchQuery.getPostDescription() != null) {
            key = "postDescription";
            content =  searchQuery.getPostDescription();
            fuzzyQuery = true;
        } else if (searchQuery.getTitle() != null) {
            key = "title";
            content =  searchQuery.getTitle();
            fuzzyQuery = true;
        } else {
            return null;
        }

        if (fuzzyQuery) {
            QueryBuilder qb = fuzzyQuery(
                    key,
                    content
            );
            NativeSearchQuery nativeSearchQuery = new NativeSearchQueryBuilder()
                    .withQuery(qb)
                    .build();
            return elasticsearchTemplate.queryForList(nativeSearchQuery, Book.class);
        } else {
            QueryBuilder qb = matchQuery(
                    key,
                    content
            ).operator(Operator.AND); // The operator flag can be set to or or and to control the boolean clauses (defaults to or).
            NativeSearchQuery nativeSearchQuery = new NativeSearchQueryBuilder()
                    .withQuery(qb)
                    .build();
            return elasticsearchTemplate.queryForList(nativeSearchQuery, Book.class);
        }
    }
}
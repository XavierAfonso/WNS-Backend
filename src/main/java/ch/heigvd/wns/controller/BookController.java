package ch.heigvd.wns.controller;

import ch.heigvd.wns.dto.PostDTO;
import ch.heigvd.wns.dto.SearchQuery;
import ch.heigvd.wns.model.elasticsearch.Book;
import ch.heigvd.wns.model.mongo.Followers;
import ch.heigvd.wns.model.mongo.User;
import ch.heigvd.wns.repository.elasticsearch.BookRepository;
import ch.heigvd.wns.repository.mongo.FollowerRepository;
import ch.heigvd.wns.repository.mongo.UserRepository;
import ch.heigvd.wns.security.jwt.AuthenticatedUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.elasticsearch.index.query.QueryBuilders.*;
import org.apache.http.HttpHost;
import org.apache.lucene.analysis.CharArrayMap;
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
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

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

    @RequestMapping(method = { RequestMethod.DELETE }, produces = "application/json")
    public @ResponseBody
    ResponseEntity deleteBook(@RequestParam("id_book") String id) {
        AuthenticatedUser auth = (AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByEmail(auth.getName());
        if (user == null) {
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        Optional<Book> book = bookRepository.findById(id);
        if (book.isPresent()) {
            Book _book = book.get();
            bookRepository.delete(_book);
            File pdfFile = new File(APP_PDF_FOLDER + _book.getTitle());

            if (pdfFile.exists()) {
                pdfFile.delete();
                // If you require it to make the entire directory path including parents,
                // use directory.mkdirs(); here instead.
            }
            return new ResponseEntity(HttpStatus.OK);
        }
        return new ResponseEntity(HttpStatus.BAD_REQUEST);
    }

    @RequestMapping(method = { RequestMethod.GET }, produces = "application/json")
    public @ResponseBody
    List<Book> getUserBooks(@RequestParam("id_user") String id) {
        User user = userRepository.findByEmail(id);
        if (user == null) {
            return null;
        }
        return bookRepository.findAllByAuthorIdOrderByCreatedDateDesc(user.getEmail());
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
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(value = "search", method = { RequestMethod.GET }, produces = "application/json")
    public @ResponseBody
    List<Book> search(@RequestBody SearchQuery searchQuery) {
        System.out.println(searchQuery);
        Boolean isSearchable = false;
        Map<String, String> hashmap = new HashMap<>();

        if (searchQuery.getBookContent() != null) {
            hashmap.put("attachment.content", searchQuery.getBookContent());
            isSearchable = true;
        }
        if (searchQuery.getTags() != null) {
            hashmap.put("tags", Arrays.toString(searchQuery.getTags()));
            isSearchable = true;
        }
        if (searchQuery.getPostDescription() != null) {
            hashmap.put("postDescription", searchQuery.getPostDescription());
            isSearchable = true;
        }
        if (searchQuery.getTitle() != null) {
            hashmap.put("title", searchQuery.getTitle());
            isSearchable = true;
        }

        if (isSearchable) {
            BoolQueryBuilder query = QueryBuilders.boolQuery();
            try {
                System.out.println(hashmap);
                for (String key : hashmap.keySet()) {
                    query.must(QueryBuilders.matchQuery(key, hashmap.get(key)));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println(query);
            NativeSearchQuery nativeSearchQuery = new NativeSearchQueryBuilder()
                    .withQuery(query)
                    .build();
            return elasticsearchTemplate.queryForList(nativeSearchQuery, Book.class);
        } else {
            return null;
        }
    }

    @RequestMapping(value = "like/{book_id}", method = { RequestMethod.POST }, produces = "application/json")
    public @ResponseBody
    ResponseEntity<Book> likeABook(@PathVariable("book_id") String bookId) {
        AuthenticatedUser auth = (AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication();
        Optional<Book> optionalBook = bookRepository.findById(bookId);
        if (optionalBook.isPresent()) {
            User user = userRepository.findByEmail(auth.getName());
            user.addLike(bookId);
            userRepository.save(user);
            return new ResponseEntity(HttpStatus.CREATED);
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

}
package ch.heigvd.wns.controller;

import ch.heigvd.wns.model.elasticsearch.Book;
import ch.heigvd.wns.model.mongo.Followers;
import ch.heigvd.wns.model.mongo.Notification;
import ch.heigvd.wns.model.mongo.User;
import ch.heigvd.wns.repository.elasticsearch.BookRepository;
import ch.heigvd.wns.repository.mongo.FollowerRepository;
import ch.heigvd.wns.repository.mongo.UserRepository;
import ch.heigvd.wns.security.jwt.AccountCredentials;
import ch.heigvd.wns.security.jwt.AuthenticatedUser;
import com.mongodb.MongoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FollowerRepository followerRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @RequestMapping(value = "signup", method = { RequestMethod.POST }, produces = "application/json")
    public @ResponseBody
    ResponseEntity<User> signup(@RequestBody AccountCredentials creds) {
        if (userRepository.findByEmail(creds.getUsername()) != null) {
            return ResponseEntity.status(400).body(null);
        }
        User user = new User();
        user.setEmail(creds.getUsername());
        user.setPassword(bCryptPasswordEncoder.encode(creds.getPassword()));
        user.setFirstname(creds.getFirstname());
        user.setLastname(creds.getLastname());
        user.setUsername(creds.getRealUsername());
        user.setRoles(new String[] {"USER"});
        userRepository.save(user);
        return ResponseEntity.ok(user);
    }

    @RequestMapping(value = "me", method = { RequestMethod.GET }, produces = "application/json")
    public @ResponseBody
    ResponseEntity<User> me() {
        AuthenticatedUser auth = (AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByEmail(auth.getName());
        if (user == null) {
            return ResponseEntity.status(400).body(null);
        }
        return ResponseEntity.ok(user);
    }

    @RequestMapping(value = "{id_user}", method = { RequestMethod.GET }, produces = "application/json")
    public @ResponseBody
    ResponseEntity<User> getSingleUser(@PathVariable("id_user") String id) {
        User user = userRepository.findByEmail(id);
        if (user == null) {
            return ResponseEntity.status(400).body(null);
        }
        return ResponseEntity.ok(user);
    }

    // Followers section
    // Finish pagination
    @RequestMapping(value = "{id}/followers", method = { RequestMethod.GET }, produces = "application/json")
    public @ResponseBody
    List<User> followers(@PathVariable String id, @PathVariable("page_number") Optional<Integer> page_number) {
        Optional<User> u = userRepository.findById(id);
        if (!u.isPresent()) {
            return null;
        }
        User user = u.get();

        List<Followers> followers = followerRepository.findByTo(user, PageRequest.of(page_number.isPresent() ? page_number.get() : 0, 50));
        List<User> users = followers.stream().map(_f -> _f.getFrom()).distinct().collect(Collectors.toList());
        return users;
    }

    @RequestMapping(value = "{id}/followings", method = { RequestMethod.GET }, produces = "application/json")
    public @ResponseBody
    List<User> followings(@PathVariable String id, @PathVariable("page_number") Optional<Integer> page_number) {
        Optional<User> u = userRepository.findById(id);
        if (!u.isPresent()) {
            return null;
        }
        User user = u.get();
        List<Followers> followers = followerRepository.findByFrom(user, PageRequest.of(page_number.isPresent() ? page_number.get() : 0, 50));
        List<User> users = followers.stream().map(_f -> _f.getTo()).distinct().collect(Collectors.toList());
        return users;
    }

    @RequestMapping(value = "follow", method = { RequestMethod.POST }, produces = "application/json")
    public @ResponseBody
    ResponseEntity follow(@RequestParam String to) {
        AuthenticatedUser auth = (AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication();

        User follower = userRepository.findByEmail(auth.getName());
        User followed = userRepository.findByEmail(to);

        if (followed == null || follower == null) {
            return ResponseEntity.status(400).body(null);
        }

        try {
            Followers followers = new Followers(follower, followed);
            followerRepository.save(followers);

            Notification notification = new Notification(follower, followed, follower.getUsername() + " followed you !", "NEW_FOLLOWER");
        } catch (MongoException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Already exists");
        }

        return ResponseEntity.ok("Correctly followed");
    }

    @RequestMapping(value = "unfollow", method = { RequestMethod.POST }, produces = "application/json")
    public @ResponseBody
    ResponseEntity unfollow(@RequestParam String to) {
        AuthenticatedUser auth = (AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication();

        User follower = userRepository.findByEmail(auth.getName());
        User followed = userRepository.findByEmail(to);

        if (followed == null || follower == null) {
            return ResponseEntity.status(400).body(null);
        }

        Followers followers = followerRepository.findByFromAndTo(follower, followed);
        followerRepository.delete(followers);
        return ResponseEntity.ok("Correctly unfollowed.");
    }

    @RequestMapping(method = { RequestMethod.GET }, produces = "application/json")
    public @ResponseBody
    ResponseEntity<List<User>> list() {
        List<User> users = userRepository.findAll();
        return ResponseEntity.ok(users);
    }

    @RequestMapping(value = "likes", method = { RequestMethod.GET }, produces = "application/json")
    public @ResponseBody
    List<Book> booksLiked(@RequestParam String id_user) {
        User user = userRepository.findByEmail(id_user);
        return bookRepository.findByIdIn(user.getLikes());
    }

    @RequestMapping(value = "wall/{id_user}", method = { RequestMethod.GET }, produces = "application/json")
    public @ResponseBody
    List<Book> wall(@PathVariable("id_user") String id) {
        // Get one or two latest post of following users
        List<Followers> followings = followerRepository.findByFrom(userRepository.findByEmail(id));
        List<String> followingsIds = followings
                .stream()
                .map(f -> f.getTo().getEmail())
                .distinct()
                .collect(Collectors.toList());
        List<Book> wall = bookRepository.findByAuthorIdInOrderByCreatedDateDesc(followingsIds);
        return wall;
    }
}
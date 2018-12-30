package ch.heigvd.wns.controller;

import ch.heigvd.wns.model.mongo.Followers;
import ch.heigvd.wns.model.mongo.Post;
import ch.heigvd.wns.model.mongo.User;
import ch.heigvd.wns.repository.mongo.FollowerRepository;
import ch.heigvd.wns.repository.mongo.PostRepository;
import ch.heigvd.wns.repository.mongo.UserRepository;
import ch.heigvd.wns.security.jwt.AccountCredentials;
import com.mongodb.MongoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/posts")
/**
 * Create a POST
 * Get a POST
 * List POST of one user
 * Get the wall of one user
 */
public class PostController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FollowerRepository followerRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @RequestMapping(method = { RequestMethod.POST }, produces = "application/json")
    public @ResponseBody
    ResponseEntity<User> create(@RequestBody AccountCredentials creds) {
        return null;
    }

    @RequestMapping(value = "{post_id}", method = { RequestMethod.GET }, produces = "application/json")
    public @ResponseBody
    Post getPost(@PathVariable("post_id") String postId) {
        Optional<Post> post = postRepository.findById(postId);
        return post.isPresent() ? post.get() : null;
    }

    @RequestMapping(method = { RequestMethod.GET }, produces = "application/json")
    public @ResponseBody
    List<Post> getUserPosts(@RequestParam("id_user") String id) {
        User user = userRepository.findByEmail(id);
        if (user == null) {
            return null;
        }
        return postRepository.findByCreatedByDesc(user);
    }

    @RequestMapping(value = "wall/{id_user}", method = { RequestMethod.GET }, produces = "application/json")
    public @ResponseBody
    List<Post> wall(@RequestParam("id_user") String id) {
        // Get one or two latest post of following users
        return null;
    }

}
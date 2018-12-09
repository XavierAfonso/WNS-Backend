package ch.heigvd.wns.controller;

import ch.heigvd.wns.model.User;
import ch.heigvd.wns.repository.UserRepository;
import ch.heigvd.wns.security.jwt.AccountCredentials;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @RequestMapping(value = "signin", method = { RequestMethod.POST }, produces = "application/json")
    public @ResponseBody
    String signin(@RequestParam("username") String username,
                  @RequestParam("password") String password) {
        User user = new User();
        user.setEmail(username);
        user.setPassword(bCryptPasswordEncoder.encode(password));
        userRepository.save(user);
        return "ok";
    }

    @RequestMapping(value = "signup", method = { RequestMethod.POST }, produces = "application/json")
    public @ResponseBody
    ResponseEntity<User> signup(@RequestBody AccountCredentials creds) {
        User user = new User();
        user.setEmail(creds.getUsername());
        user.setPassword(bCryptPasswordEncoder.encode(creds.getPassword()));
        userRepository.save(user);
        return ResponseEntity.ok(user);
    }
}
package ch.heigvd.wns.controller;

import ch.heigvd.wns.model.User;
import ch.heigvd.wns.repository.UserRepository;
import ch.heigvd.wns.security.jwt.AccountCredentials;
import io.jsonwebtoken.lang.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @RequestMapping(value = "signup", method = { RequestMethod.POST }, produces = "application/json")
    public @ResponseBody
    ResponseEntity<User> signup(@RequestBody AccountCredentials creds) {
        User user = new User();
        user.setEmail(creds.getUsername());
        user.setPassword(bCryptPasswordEncoder.encode(creds.getPassword()));
        user.setRoles(new String[] {"USER"});
        userRepository.save(user);
        return ResponseEntity.ok(user);
    }
}
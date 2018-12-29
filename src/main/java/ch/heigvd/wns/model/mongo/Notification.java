package ch.heigvd.wns.model.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * There are several type of notification. Such as:
 *  - The author XX posted a book (to all Followers of this author)
 *  - The User YY liked your book
 *  - The User YY followed you
 *  - The User YY added a comment to your post
 *
 *  Improvements:
 *      - in order to improve the UX we can add a Notification Type for giving a better design depending on the case
 *        we ignore this part for the moment.
 */
@Document(collection = "notification")
public class Notification {

    @Id
    private String id;

    @DBRef
    private User sender;

    @DBRef
    private User recipient;



    private String content;

}

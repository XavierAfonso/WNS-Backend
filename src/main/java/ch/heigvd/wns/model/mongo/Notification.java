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

    // Can be a page, a persone or others things
    @DBRef
    private User sender;

    @DBRef
    private User recipient;

    private String content = "";

    private String type = "";

    private Boolean isRead = false;

    public Notification() {

    }

    public Notification(User sender, User recipient, String content, String type) {
        this.sender = sender;
        this.recipient = recipient;
        this.content = content;
        this.type = type;
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public User getRecipient() {
        return recipient;
    }

    public void setRecipient(User recipient) {
        this.recipient = recipient;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getRead() {
        return isRead;
    }

    public void setRead(Boolean read) {
        isRead = read;
    }
}

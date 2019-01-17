package ch.heigvd.wns.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;

public class PostDTO {

    @JsonProperty("post_description")
    private String postDescription;

    @JsonProperty("book_title")
    private String title;

    @JsonProperty("book_content")
    private String bookContent;

    @JsonProperty("author_id")
    private String authorId;

    @JsonProperty("tags")
    private String[] tags;

    public PostDTO() {

    }

    public PostDTO(String title, String postDescription, String bookContent, String authorId, String[] tags) {
        this.title = title;
        this.postDescription = postDescription;
        this.bookContent = bookContent;
        this.authorId = authorId;
        this.tags = tags;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPostDescription() {
        return postDescription;
    }

    public void setPostDescription(String postDescription) {
        this.postDescription = postDescription;
    }

    public String getBookContent() {
        return bookContent;
    }

    public void setBookContent(String bookContent) {
        this.bookContent = bookContent;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public String[] getTags() {
        return tags;
    }

    public void setTags(String[] tags) {
        this.tags = tags;
    }

    @Override
    public String toString() {
        return "PostDTO{" +
                "title='" + title + '\'' +
                ", postDescription='" + postDescription + '\'' +
                ", bookContent='" + bookContent + '\'' +
                ", tags=" + Arrays.toString(tags) +
                '}';
    }
}

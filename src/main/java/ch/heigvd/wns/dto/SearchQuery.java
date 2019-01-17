package ch.heigvd.wns.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;

public class SearchQuery {

    @JsonProperty("book_tags")
    private String[] tags;

    @JsonProperty("book_content")
    private String bookContent;

    @JsonProperty("book_title")
    private String title;

    @JsonProperty("book_post_description")
    private String postDescription;

    public SearchQuery() {
    }


    public String[] getTags() {
        return tags;
    }

    public void setTags(String[] tags) {
        this.tags = tags;
    }

    public String getBookContent() {
        return bookContent;
    }

    public void setBookContent(String bookContent) {
        this.bookContent = bookContent;
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

    @Override
    public String toString() {
        return "SearchQuery{" +
                "tags=" + Arrays.toString(tags) +
                ", bookContent='" + bookContent + '\'' +
                ", title='" + title + '\'' +
                ", postDescription='" + postDescription + '\'' +
                '}';
    }
}

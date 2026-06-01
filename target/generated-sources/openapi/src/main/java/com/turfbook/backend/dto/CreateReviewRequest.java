package com.turfbook.backend.dto;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * CreateReviewRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.6.0")
public class CreateReviewRequest implements Serializable {

  private static final long serialVersionUID = 1L;

  private Long bookingId;

  private Integer rating;

  private String comment;

  private Integer cleanliness;

  private Integer ground;

  private Integer staff;

  public CreateReviewRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CreateReviewRequest(Long bookingId, Integer rating, String comment, Integer cleanliness, Integer ground, Integer staff) {
    this.bookingId = bookingId;
    this.rating = rating;
    this.comment = comment;
    this.cleanliness = cleanliness;
    this.ground = ground;
    this.staff = staff;
  }

  public CreateReviewRequest bookingId(Long bookingId) {
    this.bookingId = bookingId;
    return this;
  }

  /**
   * Get bookingId
   * @return bookingId
  */
  @NotNull 
  @JsonProperty("bookingId")
  public Long getBookingId() {
    return bookingId;
  }

  public void setBookingId(Long bookingId) {
    this.bookingId = bookingId;
  }

  public CreateReviewRequest rating(Integer rating) {
    this.rating = rating;
    return this;
  }

  /**
   * Get rating
   * minimum: 1
   * maximum: 5
   * @return rating
  */
  @NotNull @Min(1) @Max(5) 
  @JsonProperty("rating")
  public Integer getRating() {
    return rating;
  }

  public void setRating(Integer rating) {
    this.rating = rating;
  }

  public CreateReviewRequest comment(String comment) {
    this.comment = comment;
    return this;
  }

  /**
   * Get comment
   * @return comment
  */
  @NotNull 
  @JsonProperty("comment")
  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public CreateReviewRequest cleanliness(Integer cleanliness) {
    this.cleanliness = cleanliness;
    return this;
  }

  /**
   * Get cleanliness
   * minimum: 1
   * maximum: 5
   * @return cleanliness
  */
  @NotNull @Min(1) @Max(5) 
  @JsonProperty("cleanliness")
  public Integer getCleanliness() {
    return cleanliness;
  }

  public void setCleanliness(Integer cleanliness) {
    this.cleanliness = cleanliness;
  }

  public CreateReviewRequest ground(Integer ground) {
    this.ground = ground;
    return this;
  }

  /**
   * Get ground
   * minimum: 1
   * maximum: 5
   * @return ground
  */
  @NotNull @Min(1) @Max(5) 
  @JsonProperty("ground")
  public Integer getGround() {
    return ground;
  }

  public void setGround(Integer ground) {
    this.ground = ground;
  }

  public CreateReviewRequest staff(Integer staff) {
    this.staff = staff;
    return this;
  }

  /**
   * Get staff
   * minimum: 1
   * maximum: 5
   * @return staff
  */
  @NotNull @Min(1) @Max(5) 
  @JsonProperty("staff")
  public Integer getStaff() {
    return staff;
  }

  public void setStaff(Integer staff) {
    this.staff = staff;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CreateReviewRequest createReviewRequest = (CreateReviewRequest) o;
    return Objects.equals(this.bookingId, createReviewRequest.bookingId) &&
        Objects.equals(this.rating, createReviewRequest.rating) &&
        Objects.equals(this.comment, createReviewRequest.comment) &&
        Objects.equals(this.cleanliness, createReviewRequest.cleanliness) &&
        Objects.equals(this.ground, createReviewRequest.ground) &&
        Objects.equals(this.staff, createReviewRequest.staff);
  }

  @Override
  public int hashCode() {
    return Objects.hash(bookingId, rating, comment, cleanliness, ground, staff);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreateReviewRequest {\n");
    sb.append("    bookingId: ").append(toIndentedString(bookingId)).append("\n");
    sb.append("    rating: ").append(toIndentedString(rating)).append("\n");
    sb.append("    comment: ").append(toIndentedString(comment)).append("\n");
    sb.append("    cleanliness: ").append(toIndentedString(cleanliness)).append("\n");
    sb.append("    ground: ").append(toIndentedString(ground)).append("\n");
    sb.append("    staff: ").append(toIndentedString(staff)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}


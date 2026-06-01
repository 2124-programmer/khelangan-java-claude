package com.turfbook.backend.dto;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import java.io.Serializable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * ReviewDto
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.6.0")
public class ReviewDto implements Serializable {

  private static final long serialVersionUID = 1L;

  private Long id;

  private Long bookingId;

  private Long venueId;

  private Long playerId;

  private String playerName;

  private Integer rating;

  private String comment;

  private Integer cleanliness;

  private Integer ground;

  private Integer staff;

  private String ownerReply;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate createdAt;

  public ReviewDto id(Long id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
  */
  
  @JsonProperty("id")
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public ReviewDto bookingId(Long bookingId) {
    this.bookingId = bookingId;
    return this;
  }

  /**
   * Get bookingId
   * @return bookingId
  */
  
  @JsonProperty("bookingId")
  public Long getBookingId() {
    return bookingId;
  }

  public void setBookingId(Long bookingId) {
    this.bookingId = bookingId;
  }

  public ReviewDto venueId(Long venueId) {
    this.venueId = venueId;
    return this;
  }

  /**
   * Get venueId
   * @return venueId
  */
  
  @JsonProperty("venueId")
  public Long getVenueId() {
    return venueId;
  }

  public void setVenueId(Long venueId) {
    this.venueId = venueId;
  }

  public ReviewDto playerId(Long playerId) {
    this.playerId = playerId;
    return this;
  }

  /**
   * Get playerId
   * @return playerId
  */
  
  @JsonProperty("playerId")
  public Long getPlayerId() {
    return playerId;
  }

  public void setPlayerId(Long playerId) {
    this.playerId = playerId;
  }

  public ReviewDto playerName(String playerName) {
    this.playerName = playerName;
    return this;
  }

  /**
   * Get playerName
   * @return playerName
  */
  
  @JsonProperty("playerName")
  public String getPlayerName() {
    return playerName;
  }

  public void setPlayerName(String playerName) {
    this.playerName = playerName;
  }

  public ReviewDto rating(Integer rating) {
    this.rating = rating;
    return this;
  }

  /**
   * Get rating
   * @return rating
  */
  
  @JsonProperty("rating")
  public Integer getRating() {
    return rating;
  }

  public void setRating(Integer rating) {
    this.rating = rating;
  }

  public ReviewDto comment(String comment) {
    this.comment = comment;
    return this;
  }

  /**
   * Get comment
   * @return comment
  */
  
  @JsonProperty("comment")
  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public ReviewDto cleanliness(Integer cleanliness) {
    this.cleanliness = cleanliness;
    return this;
  }

  /**
   * Get cleanliness
   * @return cleanliness
  */
  
  @JsonProperty("cleanliness")
  public Integer getCleanliness() {
    return cleanliness;
  }

  public void setCleanliness(Integer cleanliness) {
    this.cleanliness = cleanliness;
  }

  public ReviewDto ground(Integer ground) {
    this.ground = ground;
    return this;
  }

  /**
   * Get ground
   * @return ground
  */
  
  @JsonProperty("ground")
  public Integer getGround() {
    return ground;
  }

  public void setGround(Integer ground) {
    this.ground = ground;
  }

  public ReviewDto staff(Integer staff) {
    this.staff = staff;
    return this;
  }

  /**
   * Get staff
   * @return staff
  */
  
  @JsonProperty("staff")
  public Integer getStaff() {
    return staff;
  }

  public void setStaff(Integer staff) {
    this.staff = staff;
  }

  public ReviewDto ownerReply(String ownerReply) {
    this.ownerReply = ownerReply;
    return this;
  }

  /**
   * Get ownerReply
   * @return ownerReply
  */
  
  @JsonProperty("ownerReply")
  public String getOwnerReply() {
    return ownerReply;
  }

  public void setOwnerReply(String ownerReply) {
    this.ownerReply = ownerReply;
  }

  public ReviewDto createdAt(LocalDate createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  /**
   * Get createdAt
   * @return createdAt
  */
  @Valid 
  @JsonProperty("createdAt")
  public LocalDate getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDate createdAt) {
    this.createdAt = createdAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ReviewDto reviewDto = (ReviewDto) o;
    return Objects.equals(this.id, reviewDto.id) &&
        Objects.equals(this.bookingId, reviewDto.bookingId) &&
        Objects.equals(this.venueId, reviewDto.venueId) &&
        Objects.equals(this.playerId, reviewDto.playerId) &&
        Objects.equals(this.playerName, reviewDto.playerName) &&
        Objects.equals(this.rating, reviewDto.rating) &&
        Objects.equals(this.comment, reviewDto.comment) &&
        Objects.equals(this.cleanliness, reviewDto.cleanliness) &&
        Objects.equals(this.ground, reviewDto.ground) &&
        Objects.equals(this.staff, reviewDto.staff) &&
        Objects.equals(this.ownerReply, reviewDto.ownerReply) &&
        Objects.equals(this.createdAt, reviewDto.createdAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, bookingId, venueId, playerId, playerName, rating, comment, cleanliness, ground, staff, ownerReply, createdAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ReviewDto {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    bookingId: ").append(toIndentedString(bookingId)).append("\n");
    sb.append("    venueId: ").append(toIndentedString(venueId)).append("\n");
    sb.append("    playerId: ").append(toIndentedString(playerId)).append("\n");
    sb.append("    playerName: ").append(toIndentedString(playerName)).append("\n");
    sb.append("    rating: ").append(toIndentedString(rating)).append("\n");
    sb.append("    comment: ").append(toIndentedString(comment)).append("\n");
    sb.append("    cleanliness: ").append(toIndentedString(cleanliness)).append("\n");
    sb.append("    ground: ").append(toIndentedString(ground)).append("\n");
    sb.append("    staff: ").append(toIndentedString(staff)).append("\n");
    sb.append("    ownerReply: ").append(toIndentedString(ownerReply)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
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


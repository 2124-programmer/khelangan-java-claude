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
 * DisputeDto
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.6.0")
public class DisputeDto implements Serializable {

  private static final long serialVersionUID = 1L;

  private Long id;

  private Long bookingId;

  private Long playerId;

  private String playerName;

  private Long ownerId;

  private String ownerName;

  private String venueName;

  private String issue;

  private String status;

  private String resolvedNote;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate createdAt;

  public DisputeDto id(Long id) {
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

  public DisputeDto bookingId(Long bookingId) {
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

  public DisputeDto playerId(Long playerId) {
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

  public DisputeDto playerName(String playerName) {
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

  public DisputeDto ownerId(Long ownerId) {
    this.ownerId = ownerId;
    return this;
  }

  /**
   * Get ownerId
   * @return ownerId
  */
  
  @JsonProperty("ownerId")
  public Long getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(Long ownerId) {
    this.ownerId = ownerId;
  }

  public DisputeDto ownerName(String ownerName) {
    this.ownerName = ownerName;
    return this;
  }

  /**
   * Get ownerName
   * @return ownerName
  */
  
  @JsonProperty("ownerName")
  public String getOwnerName() {
    return ownerName;
  }

  public void setOwnerName(String ownerName) {
    this.ownerName = ownerName;
  }

  public DisputeDto venueName(String venueName) {
    this.venueName = venueName;
    return this;
  }

  /**
   * Get venueName
   * @return venueName
  */
  
  @JsonProperty("venueName")
  public String getVenueName() {
    return venueName;
  }

  public void setVenueName(String venueName) {
    this.venueName = venueName;
  }

  public DisputeDto issue(String issue) {
    this.issue = issue;
    return this;
  }

  /**
   * Get issue
   * @return issue
  */
  
  @JsonProperty("issue")
  public String getIssue() {
    return issue;
  }

  public void setIssue(String issue) {
    this.issue = issue;
  }

  public DisputeDto status(String status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * @return status
  */
  
  @JsonProperty("status")
  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public DisputeDto resolvedNote(String resolvedNote) {
    this.resolvedNote = resolvedNote;
    return this;
  }

  /**
   * Get resolvedNote
   * @return resolvedNote
  */
  
  @JsonProperty("resolvedNote")
  public String getResolvedNote() {
    return resolvedNote;
  }

  public void setResolvedNote(String resolvedNote) {
    this.resolvedNote = resolvedNote;
  }

  public DisputeDto createdAt(LocalDate createdAt) {
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
    DisputeDto disputeDto = (DisputeDto) o;
    return Objects.equals(this.id, disputeDto.id) &&
        Objects.equals(this.bookingId, disputeDto.bookingId) &&
        Objects.equals(this.playerId, disputeDto.playerId) &&
        Objects.equals(this.playerName, disputeDto.playerName) &&
        Objects.equals(this.ownerId, disputeDto.ownerId) &&
        Objects.equals(this.ownerName, disputeDto.ownerName) &&
        Objects.equals(this.venueName, disputeDto.venueName) &&
        Objects.equals(this.issue, disputeDto.issue) &&
        Objects.equals(this.status, disputeDto.status) &&
        Objects.equals(this.resolvedNote, disputeDto.resolvedNote) &&
        Objects.equals(this.createdAt, disputeDto.createdAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, bookingId, playerId, playerName, ownerId, ownerName, venueName, issue, status, resolvedNote, createdAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DisputeDto {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    bookingId: ").append(toIndentedString(bookingId)).append("\n");
    sb.append("    playerId: ").append(toIndentedString(playerId)).append("\n");
    sb.append("    playerName: ").append(toIndentedString(playerName)).append("\n");
    sb.append("    ownerId: ").append(toIndentedString(ownerId)).append("\n");
    sb.append("    ownerName: ").append(toIndentedString(ownerName)).append("\n");
    sb.append("    venueName: ").append(toIndentedString(venueName)).append("\n");
    sb.append("    issue: ").append(toIndentedString(issue)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    resolvedNote: ").append(toIndentedString(resolvedNote)).append("\n");
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


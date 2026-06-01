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
 * CreateDisputeRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.6.0")
public class CreateDisputeRequest implements Serializable {

  private static final long serialVersionUID = 1L;

  private Long bookingId;

  private String issue;

  public CreateDisputeRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CreateDisputeRequest(Long bookingId, String issue) {
    this.bookingId = bookingId;
    this.issue = issue;
  }

  public CreateDisputeRequest bookingId(Long bookingId) {
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

  public CreateDisputeRequest issue(String issue) {
    this.issue = issue;
    return this;
  }

  /**
   * Get issue
   * @return issue
  */
  @NotNull 
  @JsonProperty("issue")
  public String getIssue() {
    return issue;
  }

  public void setIssue(String issue) {
    this.issue = issue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CreateDisputeRequest createDisputeRequest = (CreateDisputeRequest) o;
    return Objects.equals(this.bookingId, createDisputeRequest.bookingId) &&
        Objects.equals(this.issue, createDisputeRequest.issue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(bookingId, issue);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreateDisputeRequest {\n");
    sb.append("    bookingId: ").append(toIndentedString(bookingId)).append("\n");
    sb.append("    issue: ").append(toIndentedString(issue)).append("\n");
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


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
 * ResolveDisputeRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.6.0")
public class ResolveDisputeRequest implements Serializable {

  private static final long serialVersionUID = 1L;

  private String resolvedNote;

  public ResolveDisputeRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ResolveDisputeRequest(String resolvedNote) {
    this.resolvedNote = resolvedNote;
  }

  public ResolveDisputeRequest resolvedNote(String resolvedNote) {
    this.resolvedNote = resolvedNote;
    return this;
  }

  /**
   * Get resolvedNote
   * @return resolvedNote
  */
  @NotNull 
  @JsonProperty("resolvedNote")
  public String getResolvedNote() {
    return resolvedNote;
  }

  public void setResolvedNote(String resolvedNote) {
    this.resolvedNote = resolvedNote;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ResolveDisputeRequest resolveDisputeRequest = (ResolveDisputeRequest) o;
    return Objects.equals(this.resolvedNote, resolveDisputeRequest.resolvedNote);
  }

  @Override
  public int hashCode() {
    return Objects.hash(resolvedNote);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ResolveDisputeRequest {\n");
    sb.append("    resolvedNote: ").append(toIndentedString(resolvedNote)).append("\n");
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


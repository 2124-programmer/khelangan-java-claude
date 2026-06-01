package com.turfbook.backend.dto;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.io.Serializable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * BroadcastRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.6.0")
public class BroadcastRequest implements Serializable {

  private static final long serialVersionUID = 1L;

  private String title;

  private String body;

  /**
   * Gets or Sets audience
   */
  public enum AudienceEnum {
    ALL("ALL"),
    
    PLAYERS("PLAYERS"),
    
    OWNERS("OWNERS");

    private String value;

    AudienceEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static AudienceEnum fromValue(String value) {
      for (AudienceEnum b : AudienceEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private AudienceEnum audience;

  public BroadcastRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public BroadcastRequest(String title, String body, AudienceEnum audience) {
    this.title = title;
    this.body = body;
    this.audience = audience;
  }

  public BroadcastRequest title(String title) {
    this.title = title;
    return this;
  }

  /**
   * Get title
   * @return title
  */
  @NotNull 
  @JsonProperty("title")
  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public BroadcastRequest body(String body) {
    this.body = body;
    return this;
  }

  /**
   * Get body
   * @return body
  */
  @NotNull 
  @JsonProperty("body")
  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public BroadcastRequest audience(AudienceEnum audience) {
    this.audience = audience;
    return this;
  }

  /**
   * Get audience
   * @return audience
  */
  @NotNull 
  @JsonProperty("audience")
  public AudienceEnum getAudience() {
    return audience;
  }

  public void setAudience(AudienceEnum audience) {
    this.audience = audience;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BroadcastRequest broadcastRequest = (BroadcastRequest) o;
    return Objects.equals(this.title, broadcastRequest.title) &&
        Objects.equals(this.body, broadcastRequest.body) &&
        Objects.equals(this.audience, broadcastRequest.audience);
  }

  @Override
  public int hashCode() {
    return Objects.hash(title, body, audience);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BroadcastRequest {\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    body: ").append(toIndentedString(body)).append("\n");
    sb.append("    audience: ").append(toIndentedString(audience)).append("\n");
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


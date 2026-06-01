package com.turfbook.backend.dto;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.io.Serializable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * UpdateProfileRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.6.0")
public class UpdateProfileRequest implements Serializable {

  private static final long serialVersionUID = 1L;

  private String name;

  private String phone;

  private String avatarUrl;

  @Valid
  private List<String> preferredSports = new ArrayList<>();

  public UpdateProfileRequest name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
  */
  @Size(min = 2, max = 100) 
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public UpdateProfileRequest phone(String phone) {
    this.phone = phone;
    return this;
  }

  /**
   * Get phone
   * @return phone
  */
  
  @JsonProperty("phone")
  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public UpdateProfileRequest avatarUrl(String avatarUrl) {
    this.avatarUrl = avatarUrl;
    return this;
  }

  /**
   * Get avatarUrl
   * @return avatarUrl
  */
  
  @JsonProperty("avatarUrl")
  public String getAvatarUrl() {
    return avatarUrl;
  }

  public void setAvatarUrl(String avatarUrl) {
    this.avatarUrl = avatarUrl;
  }

  public UpdateProfileRequest preferredSports(List<String> preferredSports) {
    this.preferredSports = preferredSports;
    return this;
  }

  public UpdateProfileRequest addPreferredSportsItem(String preferredSportsItem) {
    if (this.preferredSports == null) {
      this.preferredSports = new ArrayList<>();
    }
    this.preferredSports.add(preferredSportsItem);
    return this;
  }

  /**
   * Get preferredSports
   * @return preferredSports
  */
  
  @JsonProperty("preferredSports")
  public List<String> getPreferredSports() {
    return preferredSports;
  }

  public void setPreferredSports(List<String> preferredSports) {
    this.preferredSports = preferredSports;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UpdateProfileRequest updateProfileRequest = (UpdateProfileRequest) o;
    return Objects.equals(this.name, updateProfileRequest.name) &&
        Objects.equals(this.phone, updateProfileRequest.phone) &&
        Objects.equals(this.avatarUrl, updateProfileRequest.avatarUrl) &&
        Objects.equals(this.preferredSports, updateProfileRequest.preferredSports);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, phone, avatarUrl, preferredSports);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UpdateProfileRequest {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    phone: ").append(toIndentedString(phone)).append("\n");
    sb.append("    avatarUrl: ").append(toIndentedString(avatarUrl)).append("\n");
    sb.append("    preferredSports: ").append(toIndentedString(preferredSports)).append("\n");
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


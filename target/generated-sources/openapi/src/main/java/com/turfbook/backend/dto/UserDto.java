package com.turfbook.backend.dto;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import java.io.Serializable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * UserDto
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.6.0")
public class UserDto implements Serializable {

  private static final long serialVersionUID = 1L;

  private Long id;

  private String name;

  private String email;

  private String phone;

  private String role;

  private String avatarUrl;

  @Valid
  private List<String> preferredSports = new ArrayList<>();

  private Integer totalBookings;

  private Boolean isPremium;

  private Boolean isBlocked;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  public UserDto id(Long id) {
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

  public UserDto name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
  */
  
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public UserDto email(String email) {
    this.email = email;
    return this;
  }

  /**
   * Get email
   * @return email
  */
  
  @JsonProperty("email")
  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public UserDto phone(String phone) {
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

  public UserDto role(String role) {
    this.role = role;
    return this;
  }

  /**
   * Get role
   * @return role
  */
  
  @JsonProperty("role")
  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public UserDto avatarUrl(String avatarUrl) {
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

  public UserDto preferredSports(List<String> preferredSports) {
    this.preferredSports = preferredSports;
    return this;
  }

  public UserDto addPreferredSportsItem(String preferredSportsItem) {
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

  public UserDto totalBookings(Integer totalBookings) {
    this.totalBookings = totalBookings;
    return this;
  }

  /**
   * Get totalBookings
   * @return totalBookings
  */
  
  @JsonProperty("totalBookings")
  public Integer getTotalBookings() {
    return totalBookings;
  }

  public void setTotalBookings(Integer totalBookings) {
    this.totalBookings = totalBookings;
  }

  public UserDto isPremium(Boolean isPremium) {
    this.isPremium = isPremium;
    return this;
  }

  /**
   * Get isPremium
   * @return isPremium
  */
  
  @JsonProperty("isPremium")
  public Boolean getIsPremium() {
    return isPremium;
  }

  public void setIsPremium(Boolean isPremium) {
    this.isPremium = isPremium;
  }

  public UserDto isBlocked(Boolean isBlocked) {
    this.isBlocked = isBlocked;
    return this;
  }

  /**
   * Get isBlocked
   * @return isBlocked
  */
  
  @JsonProperty("isBlocked")
  public Boolean getIsBlocked() {
    return isBlocked;
  }

  public void setIsBlocked(Boolean isBlocked) {
    this.isBlocked = isBlocked;
  }

  public UserDto createdAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  /**
   * Get createdAt
   * @return createdAt
  */
  @Valid 
  @JsonProperty("createdAt")
  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
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
    UserDto userDto = (UserDto) o;
    return Objects.equals(this.id, userDto.id) &&
        Objects.equals(this.name, userDto.name) &&
        Objects.equals(this.email, userDto.email) &&
        Objects.equals(this.phone, userDto.phone) &&
        Objects.equals(this.role, userDto.role) &&
        Objects.equals(this.avatarUrl, userDto.avatarUrl) &&
        Objects.equals(this.preferredSports, userDto.preferredSports) &&
        Objects.equals(this.totalBookings, userDto.totalBookings) &&
        Objects.equals(this.isPremium, userDto.isPremium) &&
        Objects.equals(this.isBlocked, userDto.isBlocked) &&
        Objects.equals(this.createdAt, userDto.createdAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, email, phone, role, avatarUrl, preferredSports, totalBookings, isPremium, isBlocked, createdAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UserDto {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    email: ").append(toIndentedString(email)).append("\n");
    sb.append("    phone: ").append(toIndentedString(phone)).append("\n");
    sb.append("    role: ").append(toIndentedString(role)).append("\n");
    sb.append("    avatarUrl: ").append(toIndentedString(avatarUrl)).append("\n");
    sb.append("    preferredSports: ").append(toIndentedString(preferredSports)).append("\n");
    sb.append("    totalBookings: ").append(toIndentedString(totalBookings)).append("\n");
    sb.append("    isPremium: ").append(toIndentedString(isPremium)).append("\n");
    sb.append("    isBlocked: ").append(toIndentedString(isBlocked)).append("\n");
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


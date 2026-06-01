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
 * VenueSummaryDto
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.6.0")
public class VenueSummaryDto implements Serializable {

  private static final long serialVersionUID = 1L;

  private Long id;

  private String name;

  private String address;

  private String city;

  private String status;

  private Double rating;

  private Integer reviewCount;

  private Integer pricePerSlot;

  private String coverPhoto;

  private Double lat;

  private Double lng;

  private Long ownerId;

  public VenueSummaryDto id(Long id) {
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

  public VenueSummaryDto name(String name) {
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

  public VenueSummaryDto address(String address) {
    this.address = address;
    return this;
  }

  /**
   * Get address
   * @return address
  */
  
  @JsonProperty("address")
  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public VenueSummaryDto city(String city) {
    this.city = city;
    return this;
  }

  /**
   * Get city
   * @return city
  */
  
  @JsonProperty("city")
  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public VenueSummaryDto status(String status) {
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

  public VenueSummaryDto rating(Double rating) {
    this.rating = rating;
    return this;
  }

  /**
   * Get rating
   * @return rating
  */
  
  @JsonProperty("rating")
  public Double getRating() {
    return rating;
  }

  public void setRating(Double rating) {
    this.rating = rating;
  }

  public VenueSummaryDto reviewCount(Integer reviewCount) {
    this.reviewCount = reviewCount;
    return this;
  }

  /**
   * Get reviewCount
   * @return reviewCount
  */
  
  @JsonProperty("reviewCount")
  public Integer getReviewCount() {
    return reviewCount;
  }

  public void setReviewCount(Integer reviewCount) {
    this.reviewCount = reviewCount;
  }

  public VenueSummaryDto pricePerSlot(Integer pricePerSlot) {
    this.pricePerSlot = pricePerSlot;
    return this;
  }

  /**
   * Get pricePerSlot
   * @return pricePerSlot
  */
  
  @JsonProperty("pricePerSlot")
  public Integer getPricePerSlot() {
    return pricePerSlot;
  }

  public void setPricePerSlot(Integer pricePerSlot) {
    this.pricePerSlot = pricePerSlot;
  }

  public VenueSummaryDto coverPhoto(String coverPhoto) {
    this.coverPhoto = coverPhoto;
    return this;
  }

  /**
   * Get coverPhoto
   * @return coverPhoto
  */
  
  @JsonProperty("coverPhoto")
  public String getCoverPhoto() {
    return coverPhoto;
  }

  public void setCoverPhoto(String coverPhoto) {
    this.coverPhoto = coverPhoto;
  }

  public VenueSummaryDto lat(Double lat) {
    this.lat = lat;
    return this;
  }

  /**
   * Get lat
   * @return lat
  */
  
  @JsonProperty("lat")
  public Double getLat() {
    return lat;
  }

  public void setLat(Double lat) {
    this.lat = lat;
  }

  public VenueSummaryDto lng(Double lng) {
    this.lng = lng;
    return this;
  }

  /**
   * Get lng
   * @return lng
  */
  
  @JsonProperty("lng")
  public Double getLng() {
    return lng;
  }

  public void setLng(Double lng) {
    this.lng = lng;
  }

  public VenueSummaryDto ownerId(Long ownerId) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    VenueSummaryDto venueSummaryDto = (VenueSummaryDto) o;
    return Objects.equals(this.id, venueSummaryDto.id) &&
        Objects.equals(this.name, venueSummaryDto.name) &&
        Objects.equals(this.address, venueSummaryDto.address) &&
        Objects.equals(this.city, venueSummaryDto.city) &&
        Objects.equals(this.status, venueSummaryDto.status) &&
        Objects.equals(this.rating, venueSummaryDto.rating) &&
        Objects.equals(this.reviewCount, venueSummaryDto.reviewCount) &&
        Objects.equals(this.pricePerSlot, venueSummaryDto.pricePerSlot) &&
        Objects.equals(this.coverPhoto, venueSummaryDto.coverPhoto) &&
        Objects.equals(this.lat, venueSummaryDto.lat) &&
        Objects.equals(this.lng, venueSummaryDto.lng) &&
        Objects.equals(this.ownerId, venueSummaryDto.ownerId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, address, city, status, rating, reviewCount, pricePerSlot, coverPhoto, lat, lng, ownerId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class VenueSummaryDto {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    address: ").append(toIndentedString(address)).append("\n");
    sb.append("    city: ").append(toIndentedString(city)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    rating: ").append(toIndentedString(rating)).append("\n");
    sb.append("    reviewCount: ").append(toIndentedString(reviewCount)).append("\n");
    sb.append("    pricePerSlot: ").append(toIndentedString(pricePerSlot)).append("\n");
    sb.append("    coverPhoto: ").append(toIndentedString(coverPhoto)).append("\n");
    sb.append("    lat: ").append(toIndentedString(lat)).append("\n");
    sb.append("    lng: ").append(toIndentedString(lng)).append("\n");
    sb.append("    ownerId: ").append(toIndentedString(ownerId)).append("\n");
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


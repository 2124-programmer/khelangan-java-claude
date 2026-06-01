package com.turfbook.backend.dto;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.turfbook.backend.dto.CourtDto;
import com.turfbook.backend.dto.SportDto;
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
 * VenueDetailDto
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.6.0")
public class VenueDetailDto implements Serializable {

  private static final long serialVersionUID = 1L;

  private Long id;

  private String name;

  private String address;

  private String city;

  private String description;

  private String status;

  private Double rating;

  private Integer reviewCount;

  private Integer pricePerSlot;

  private String coverPhoto;

  @Valid
  private List<String> photos = new ArrayList<>();

  @Valid
  private List<String> amenities = new ArrayList<>();

  private Double lat;

  private Double lng;

  private Long ownerId;

  @Valid
  private List<@Valid SportDto> sports = new ArrayList<>();

  @Valid
  private List<@Valid CourtDto> courts = new ArrayList<>();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  public VenueDetailDto id(Long id) {
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

  public VenueDetailDto name(String name) {
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

  public VenueDetailDto address(String address) {
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

  public VenueDetailDto city(String city) {
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

  public VenueDetailDto description(String description) {
    this.description = description;
    return this;
  }

  /**
   * Get description
   * @return description
  */
  
  @JsonProperty("description")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public VenueDetailDto status(String status) {
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

  public VenueDetailDto rating(Double rating) {
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

  public VenueDetailDto reviewCount(Integer reviewCount) {
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

  public VenueDetailDto pricePerSlot(Integer pricePerSlot) {
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

  public VenueDetailDto coverPhoto(String coverPhoto) {
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

  public VenueDetailDto photos(List<String> photos) {
    this.photos = photos;
    return this;
  }

  public VenueDetailDto addPhotosItem(String photosItem) {
    if (this.photos == null) {
      this.photos = new ArrayList<>();
    }
    this.photos.add(photosItem);
    return this;
  }

  /**
   * Get photos
   * @return photos
  */
  
  @JsonProperty("photos")
  public List<String> getPhotos() {
    return photos;
  }

  public void setPhotos(List<String> photos) {
    this.photos = photos;
  }

  public VenueDetailDto amenities(List<String> amenities) {
    this.amenities = amenities;
    return this;
  }

  public VenueDetailDto addAmenitiesItem(String amenitiesItem) {
    if (this.amenities == null) {
      this.amenities = new ArrayList<>();
    }
    this.amenities.add(amenitiesItem);
    return this;
  }

  /**
   * Get amenities
   * @return amenities
  */
  
  @JsonProperty("amenities")
  public List<String> getAmenities() {
    return amenities;
  }

  public void setAmenities(List<String> amenities) {
    this.amenities = amenities;
  }

  public VenueDetailDto lat(Double lat) {
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

  public VenueDetailDto lng(Double lng) {
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

  public VenueDetailDto ownerId(Long ownerId) {
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

  public VenueDetailDto sports(List<@Valid SportDto> sports) {
    this.sports = sports;
    return this;
  }

  public VenueDetailDto addSportsItem(SportDto sportsItem) {
    if (this.sports == null) {
      this.sports = new ArrayList<>();
    }
    this.sports.add(sportsItem);
    return this;
  }

  /**
   * Get sports
   * @return sports
  */
  @Valid 
  @JsonProperty("sports")
  public List<@Valid SportDto> getSports() {
    return sports;
  }

  public void setSports(List<@Valid SportDto> sports) {
    this.sports = sports;
  }

  public VenueDetailDto courts(List<@Valid CourtDto> courts) {
    this.courts = courts;
    return this;
  }

  public VenueDetailDto addCourtsItem(CourtDto courtsItem) {
    if (this.courts == null) {
      this.courts = new ArrayList<>();
    }
    this.courts.add(courtsItem);
    return this;
  }

  /**
   * Get courts
   * @return courts
  */
  @Valid 
  @JsonProperty("courts")
  public List<@Valid CourtDto> getCourts() {
    return courts;
  }

  public void setCourts(List<@Valid CourtDto> courts) {
    this.courts = courts;
  }

  public VenueDetailDto createdAt(OffsetDateTime createdAt) {
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
    VenueDetailDto venueDetailDto = (VenueDetailDto) o;
    return Objects.equals(this.id, venueDetailDto.id) &&
        Objects.equals(this.name, venueDetailDto.name) &&
        Objects.equals(this.address, venueDetailDto.address) &&
        Objects.equals(this.city, venueDetailDto.city) &&
        Objects.equals(this.description, venueDetailDto.description) &&
        Objects.equals(this.status, venueDetailDto.status) &&
        Objects.equals(this.rating, venueDetailDto.rating) &&
        Objects.equals(this.reviewCount, venueDetailDto.reviewCount) &&
        Objects.equals(this.pricePerSlot, venueDetailDto.pricePerSlot) &&
        Objects.equals(this.coverPhoto, venueDetailDto.coverPhoto) &&
        Objects.equals(this.photos, venueDetailDto.photos) &&
        Objects.equals(this.amenities, venueDetailDto.amenities) &&
        Objects.equals(this.lat, venueDetailDto.lat) &&
        Objects.equals(this.lng, venueDetailDto.lng) &&
        Objects.equals(this.ownerId, venueDetailDto.ownerId) &&
        Objects.equals(this.sports, venueDetailDto.sports) &&
        Objects.equals(this.courts, venueDetailDto.courts) &&
        Objects.equals(this.createdAt, venueDetailDto.createdAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, address, city, description, status, rating, reviewCount, pricePerSlot, coverPhoto, photos, amenities, lat, lng, ownerId, sports, courts, createdAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class VenueDetailDto {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    address: ").append(toIndentedString(address)).append("\n");
    sb.append("    city: ").append(toIndentedString(city)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    rating: ").append(toIndentedString(rating)).append("\n");
    sb.append("    reviewCount: ").append(toIndentedString(reviewCount)).append("\n");
    sb.append("    pricePerSlot: ").append(toIndentedString(pricePerSlot)).append("\n");
    sb.append("    coverPhoto: ").append(toIndentedString(coverPhoto)).append("\n");
    sb.append("    photos: ").append(toIndentedString(photos)).append("\n");
    sb.append("    amenities: ").append(toIndentedString(amenities)).append("\n");
    sb.append("    lat: ").append(toIndentedString(lat)).append("\n");
    sb.append("    lng: ").append(toIndentedString(lng)).append("\n");
    sb.append("    ownerId: ").append(toIndentedString(ownerId)).append("\n");
    sb.append("    sports: ").append(toIndentedString(sports)).append("\n");
    sb.append("    courts: ").append(toIndentedString(courts)).append("\n");
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


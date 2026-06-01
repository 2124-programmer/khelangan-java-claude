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
 * UpdateVenueRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.6.0")
public class UpdateVenueRequest implements Serializable {

  private static final long serialVersionUID = 1L;

  private String name;

  private String description;

  private Integer pricePerSlot;

  @Valid
  private List<String> amenities = new ArrayList<>();

  private String coverPhoto;

  @Valid
  private List<String> photos = new ArrayList<>();

  public UpdateVenueRequest name(String name) {
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

  public UpdateVenueRequest description(String description) {
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

  public UpdateVenueRequest pricePerSlot(Integer pricePerSlot) {
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

  public UpdateVenueRequest amenities(List<String> amenities) {
    this.amenities = amenities;
    return this;
  }

  public UpdateVenueRequest addAmenitiesItem(String amenitiesItem) {
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

  public UpdateVenueRequest coverPhoto(String coverPhoto) {
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

  public UpdateVenueRequest photos(List<String> photos) {
    this.photos = photos;
    return this;
  }

  public UpdateVenueRequest addPhotosItem(String photosItem) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UpdateVenueRequest updateVenueRequest = (UpdateVenueRequest) o;
    return Objects.equals(this.name, updateVenueRequest.name) &&
        Objects.equals(this.description, updateVenueRequest.description) &&
        Objects.equals(this.pricePerSlot, updateVenueRequest.pricePerSlot) &&
        Objects.equals(this.amenities, updateVenueRequest.amenities) &&
        Objects.equals(this.coverPhoto, updateVenueRequest.coverPhoto) &&
        Objects.equals(this.photos, updateVenueRequest.photos);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, description, pricePerSlot, amenities, coverPhoto, photos);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UpdateVenueRequest {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    pricePerSlot: ").append(toIndentedString(pricePerSlot)).append("\n");
    sb.append("    amenities: ").append(toIndentedString(amenities)).append("\n");
    sb.append("    coverPhoto: ").append(toIndentedString(coverPhoto)).append("\n");
    sb.append("    photos: ").append(toIndentedString(photos)).append("\n");
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


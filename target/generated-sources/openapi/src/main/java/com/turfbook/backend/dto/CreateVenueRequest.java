package com.turfbook.backend.dto;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.turfbook.backend.dto.CreateCourtRequest;
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
 * CreateVenueRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.6.0")
public class CreateVenueRequest implements Serializable {

  private static final long serialVersionUID = 1L;

  private String name;

  private String address;

  private String city;

  private String description;

  private Integer pricePerSlot;

  @Valid
  private List<String> amenities = new ArrayList<>();

  private Double lat;

  private Double lng;

  @Valid
  private List<Long> sportIds = new ArrayList<>();

  private String coverPhoto;

  @Valid
  private List<String> photos = new ArrayList<>();

  @Valid
  private List<@Valid CreateCourtRequest> courts = new ArrayList<>();

  public CreateVenueRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CreateVenueRequest(String name, String address, String city, Integer pricePerSlot, Double lat, Double lng) {
    this.name = name;
    this.address = address;
    this.city = city;
    this.pricePerSlot = pricePerSlot;
    this.lat = lat;
    this.lng = lng;
  }

  public CreateVenueRequest name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
  */
  @NotNull 
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public CreateVenueRequest address(String address) {
    this.address = address;
    return this;
  }

  /**
   * Get address
   * @return address
  */
  @NotNull 
  @JsonProperty("address")
  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public CreateVenueRequest city(String city) {
    this.city = city;
    return this;
  }

  /**
   * Get city
   * @return city
  */
  @NotNull 
  @JsonProperty("city")
  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public CreateVenueRequest description(String description) {
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

  public CreateVenueRequest pricePerSlot(Integer pricePerSlot) {
    this.pricePerSlot = pricePerSlot;
    return this;
  }

  /**
   * Get pricePerSlot
   * @return pricePerSlot
  */
  @NotNull 
  @JsonProperty("pricePerSlot")
  public Integer getPricePerSlot() {
    return pricePerSlot;
  }

  public void setPricePerSlot(Integer pricePerSlot) {
    this.pricePerSlot = pricePerSlot;
  }

  public CreateVenueRequest amenities(List<String> amenities) {
    this.amenities = amenities;
    return this;
  }

  public CreateVenueRequest addAmenitiesItem(String amenitiesItem) {
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

  public CreateVenueRequest lat(Double lat) {
    this.lat = lat;
    return this;
  }

  /**
   * Get lat
   * @return lat
  */
  @NotNull 
  @JsonProperty("lat")
  public Double getLat() {
    return lat;
  }

  public void setLat(Double lat) {
    this.lat = lat;
  }

  public CreateVenueRequest lng(Double lng) {
    this.lng = lng;
    return this;
  }

  /**
   * Get lng
   * @return lng
  */
  @NotNull 
  @JsonProperty("lng")
  public Double getLng() {
    return lng;
  }

  public void setLng(Double lng) {
    this.lng = lng;
  }

  public CreateVenueRequest sportIds(List<Long> sportIds) {
    this.sportIds = sportIds;
    return this;
  }

  public CreateVenueRequest addSportIdsItem(Long sportIdsItem) {
    if (this.sportIds == null) {
      this.sportIds = new ArrayList<>();
    }
    this.sportIds.add(sportIdsItem);
    return this;
  }

  /**
   * Get sportIds
   * @return sportIds
  */
  
  @JsonProperty("sportIds")
  public List<Long> getSportIds() {
    return sportIds;
  }

  public void setSportIds(List<Long> sportIds) {
    this.sportIds = sportIds;
  }

  public CreateVenueRequest coverPhoto(String coverPhoto) {
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

  public CreateVenueRequest photos(List<String> photos) {
    this.photos = photos;
    return this;
  }

  public CreateVenueRequest addPhotosItem(String photosItem) {
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

  public CreateVenueRequest courts(List<@Valid CreateCourtRequest> courts) {
    this.courts = courts;
    return this;
  }

  public CreateVenueRequest addCourtsItem(CreateCourtRequest courtsItem) {
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
  public List<@Valid CreateCourtRequest> getCourts() {
    return courts;
  }

  public void setCourts(List<@Valid CreateCourtRequest> courts) {
    this.courts = courts;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CreateVenueRequest createVenueRequest = (CreateVenueRequest) o;
    return Objects.equals(this.name, createVenueRequest.name) &&
        Objects.equals(this.address, createVenueRequest.address) &&
        Objects.equals(this.city, createVenueRequest.city) &&
        Objects.equals(this.description, createVenueRequest.description) &&
        Objects.equals(this.pricePerSlot, createVenueRequest.pricePerSlot) &&
        Objects.equals(this.amenities, createVenueRequest.amenities) &&
        Objects.equals(this.lat, createVenueRequest.lat) &&
        Objects.equals(this.lng, createVenueRequest.lng) &&
        Objects.equals(this.sportIds, createVenueRequest.sportIds) &&
        Objects.equals(this.coverPhoto, createVenueRequest.coverPhoto) &&
        Objects.equals(this.photos, createVenueRequest.photos) &&
        Objects.equals(this.courts, createVenueRequest.courts);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, address, city, description, pricePerSlot, amenities, lat, lng, sportIds, coverPhoto, photos, courts);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreateVenueRequest {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    address: ").append(toIndentedString(address)).append("\n");
    sb.append("    city: ").append(toIndentedString(city)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    pricePerSlot: ").append(toIndentedString(pricePerSlot)).append("\n");
    sb.append("    amenities: ").append(toIndentedString(amenities)).append("\n");
    sb.append("    lat: ").append(toIndentedString(lat)).append("\n");
    sb.append("    lng: ").append(toIndentedString(lng)).append("\n");
    sb.append("    sportIds: ").append(toIndentedString(sportIds)).append("\n");
    sb.append("    coverPhoto: ").append(toIndentedString(coverPhoto)).append("\n");
    sb.append("    photos: ").append(toIndentedString(photos)).append("\n");
    sb.append("    courts: ").append(toIndentedString(courts)).append("\n");
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


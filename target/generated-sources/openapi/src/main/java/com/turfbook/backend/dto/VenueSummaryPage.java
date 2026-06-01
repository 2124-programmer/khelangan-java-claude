package com.turfbook.backend.dto;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.turfbook.backend.dto.VenueSummaryDto;
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
 * VenueSummaryPage
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.6.0")
public class VenueSummaryPage implements Serializable {

  private static final long serialVersionUID = 1L;

  @Valid
  private List<@Valid VenueSummaryDto> content = new ArrayList<>();

  private Long totalElements;

  private Integer totalPages;

  private Integer size;

  private Integer number;

  public VenueSummaryPage content(List<@Valid VenueSummaryDto> content) {
    this.content = content;
    return this;
  }

  public VenueSummaryPage addContentItem(VenueSummaryDto contentItem) {
    if (this.content == null) {
      this.content = new ArrayList<>();
    }
    this.content.add(contentItem);
    return this;
  }

  /**
   * Get content
   * @return content
  */
  @Valid 
  @JsonProperty("content")
  public List<@Valid VenueSummaryDto> getContent() {
    return content;
  }

  public void setContent(List<@Valid VenueSummaryDto> content) {
    this.content = content;
  }

  public VenueSummaryPage totalElements(Long totalElements) {
    this.totalElements = totalElements;
    return this;
  }

  /**
   * Get totalElements
   * @return totalElements
  */
  
  @JsonProperty("totalElements")
  public Long getTotalElements() {
    return totalElements;
  }

  public void setTotalElements(Long totalElements) {
    this.totalElements = totalElements;
  }

  public VenueSummaryPage totalPages(Integer totalPages) {
    this.totalPages = totalPages;
    return this;
  }

  /**
   * Get totalPages
   * @return totalPages
  */
  
  @JsonProperty("totalPages")
  public Integer getTotalPages() {
    return totalPages;
  }

  public void setTotalPages(Integer totalPages) {
    this.totalPages = totalPages;
  }

  public VenueSummaryPage size(Integer size) {
    this.size = size;
    return this;
  }

  /**
   * Get size
   * @return size
  */
  
  @JsonProperty("size")
  public Integer getSize() {
    return size;
  }

  public void setSize(Integer size) {
    this.size = size;
  }

  public VenueSummaryPage number(Integer number) {
    this.number = number;
    return this;
  }

  /**
   * Get number
   * @return number
  */
  
  @JsonProperty("number")
  public Integer getNumber() {
    return number;
  }

  public void setNumber(Integer number) {
    this.number = number;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    VenueSummaryPage venueSummaryPage = (VenueSummaryPage) o;
    return Objects.equals(this.content, venueSummaryPage.content) &&
        Objects.equals(this.totalElements, venueSummaryPage.totalElements) &&
        Objects.equals(this.totalPages, venueSummaryPage.totalPages) &&
        Objects.equals(this.size, venueSummaryPage.size) &&
        Objects.equals(this.number, venueSummaryPage.number);
  }

  @Override
  public int hashCode() {
    return Objects.hash(content, totalElements, totalPages, size, number);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class VenueSummaryPage {\n");
    sb.append("    content: ").append(toIndentedString(content)).append("\n");
    sb.append("    totalElements: ").append(toIndentedString(totalElements)).append("\n");
    sb.append("    totalPages: ").append(toIndentedString(totalPages)).append("\n");
    sb.append("    size: ").append(toIndentedString(size)).append("\n");
    sb.append("    number: ").append(toIndentedString(number)).append("\n");
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


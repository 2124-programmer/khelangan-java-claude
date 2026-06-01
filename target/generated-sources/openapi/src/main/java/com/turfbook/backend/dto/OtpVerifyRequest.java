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
 * OtpVerifyRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.6.0")
public class OtpVerifyRequest implements Serializable {

  private static final long serialVersionUID = 1L;

  private String phone;

  private String code;

  public OtpVerifyRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public OtpVerifyRequest(String phone, String code) {
    this.phone = phone;
    this.code = code;
  }

  public OtpVerifyRequest phone(String phone) {
    this.phone = phone;
    return this;
  }

  /**
   * Get phone
   * @return phone
  */
  @NotNull 
  @JsonProperty("phone")
  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public OtpVerifyRequest code(String code) {
    this.code = code;
    return this;
  }

  /**
   * Get code
   * @return code
  */
  @NotNull 
  @JsonProperty("code")
  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OtpVerifyRequest otpVerifyRequest = (OtpVerifyRequest) o;
    return Objects.equals(this.phone, otpVerifyRequest.phone) &&
        Objects.equals(this.code, otpVerifyRequest.code);
  }

  @Override
  public int hashCode() {
    return Objects.hash(phone, code);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class OtpVerifyRequest {\n");
    sb.append("    phone: ").append(toIndentedString(phone)).append("\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
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


package com.haui.UrbanMart.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ResendOtpRequest {

    @NotBlank(message = "Số điện thoại không được để trống")
    private String phone;
}
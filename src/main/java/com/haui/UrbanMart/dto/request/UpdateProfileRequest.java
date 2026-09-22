package com.haui.UrbanMart.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter 
public class UpdateProfileRequest {
    @NotBlank (message = "Họ tên không được để trống")
    @Size (max = 150)
    private String fullName; 

    @Email(message = "Email không hợp lệ")
    private String email;

    private LocalDate birthDate;
    private String gender;     
}

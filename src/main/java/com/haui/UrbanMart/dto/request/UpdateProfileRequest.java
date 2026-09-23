package com.haui.UrbanMart.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter 
public class UpdateProfileRequest {
    @Size(min = 1, max = 150, message = "Họ tên phải có từ 1 đến 150 ký tự")
    private String fullName; 

    @Email(message = "Email không hợp lệ")
    private String email;

    private LocalDate birthDate;
    private String gender;     
}

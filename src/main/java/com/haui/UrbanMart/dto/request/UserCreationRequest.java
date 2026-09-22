package com.haui.UrbanMart.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter 
public class UserCreationRequest {
    @NotBlank (message = "Họ tên không được để trống")
    @Size (max = 150)
    private String fullName; 

    @NotBlank (message = "Số điện thoại không được để trống")
    @Pattern (regexp = "^0\\d{9}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 8, message = "Mật khẩu phải có ít nhất 8 ký tự")
    private String password;

    private LocalDate birthDate;
    private String gender;     
}

package com.haui.UrbanMart.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AddressCreationRequest {

    @NotBlank(message = "Ten nguoi nhan khong duoc de trong")
    @Size(max = 150, message = "Ten nguoi nhan khong duoc vuot qua 150 ky tu")
    private String recipientName;

    @NotBlank(message = "So dien thoai khong duoc de trong")
    @Size(max = 20, message = "So dien thoai khong duoc vuot qua 20 ky tu")
    private String phone;

    @NotBlank(message = "Dia chi khong duoc de trong")
    @Size(max = 255, message = "Dia chi khong duoc vuot qua 255 ky tu")
    private String line;

    @Size(max = 100, message = "Phuong xa khong duoc vuot qua 100 ky tu")
    private String ward;

    @Size(max = 100, message = "Quan huyen khong duoc vuot qua 100 ky tu")
    private String district;

    @Size(max = 100, message = "Tinh thanh pho khong duoc vuot qua 100 ky tu")
    private String city;

    @DecimalMin(value = "-90.0", message = "Vi do phai nam trong khoang -90 den 90")
    @DecimalMax(value = "90.0", message = "Vi do phai nam trong khoang -90 den 90")
    private BigDecimal lat;

    @DecimalMin(value = "-180.0", message = "Kinh do phai nam trong khoang -180 den 180")
    @DecimalMax(value = "180.0", message = "Kinh do phai nam trong khoang -180 den 180")
    private BigDecimal lng;

    private boolean isDefault;
}

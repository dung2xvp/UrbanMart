package com.haui.UrbanMart.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AddressUpdateRequest {

    @Size(max = 150, message = "Tên người nhận không được vượt quá 150 ký tự")
    private String recipientName;

    @Size(max = 20, message = "Số điện thoại không được vượt quá 20 ký tự")
    private String phone;

    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự")
    private String line;

    @Size(max = 100, message = "Phường, xã không được vượt quá 100 ký tự")
    private String ward;

    @Size(max = 100, message = "Quận, huyện không được vượt quá 100 ký tự")
    private String district;

    @Size(max = 100, message = "Tỉnh, thành phố không được vượt quá 100 ký tự")
    private String city;

    @DecimalMin(value = "-90.0", message = "Vĩ độ phải nằm trong khoảng từ -90 đến 90")
    @DecimalMax(value = "90.0", message = "Vĩ độ phải nằm trong khoảng từ -90 đến 90")
    private BigDecimal lat;

    @DecimalMin(value = "-180.0", message = "Kinh độ phải nằm trong khoảng từ -180 đến 180")
    @DecimalMax(value = "180.0", message = "Kinh độ phải nằm trong khoảng từ -180 đến 180")
    private BigDecimal lng;

    private Boolean isDefault;
}

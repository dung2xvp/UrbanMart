package com.haui.UrbanMart.dto.request;

import com.haui.UrbanMart.entity.BranchStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;

public record BranchWriteRequest(
        @NotBlank(message = "Tên chi nhánh không được để trống")
        @Size(max = 150, message = "Tên chi nhánh tối đa 150 ký tự")
        String name,

        @NotBlank(message = "Địa chỉ không được để trống")
        @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự")
        String address,

        @DecimalMin(value = "-90.0", message = "Vĩ độ phải từ -90 đến 90")
        @DecimalMax(value = "90.0", message = "Vĩ độ phải từ -90 đến 90")
        @Digits(integer = 3, fraction = 6, message = "Vĩ độ tối đa 6 chữ số thập phân")
        BigDecimal lat,

        @DecimalMin(value = "-180.0", message = "Kinh độ phải từ -180 đến 180")
        @DecimalMax(value = "180.0", message = "Kinh độ phải từ -180 đến 180")
        @Digits(integer = 3, fraction = 6, message = "Kinh độ tối đa 6 chữ số thập phân")
        BigDecimal lng,

        @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
        String phone,

        @DecimalMin(value = "0.01", message = "Bán kính giao hàng phải lớn hơn 0")
        @Digits(integer = 3, fraction = 2, message = "Bán kính tối đa 3 chữ số nguyên và 2 chữ số thập phân")
        BigDecimal deliveryRadiusKm,

        Boolean isWarehouse,
        Map<String, String> openingHours,
        BranchStatus status
) {
}

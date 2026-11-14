package com.haui.UrbanMart.service;

import com.haui.UrbanMart.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ShippingFeeCalculatorTest {

    @Test
    void appliesFeesBySubtotalAndDistanceBands() {
        assertFee("299999", 0, "10000");
        assertFee("299999", 3.01, "20000");
        assertFee("299999", 10.01, "30000");
        assertFee("299999", 20.01, "40000");
        assertFee("299999", 40, "40000");
        assertFee("300000", 3, "0");
        assertFee("300000", 3.01, "10000");
        assertFee("300000", 10.01, "15000");
        assertFee("300000", 20.01, "20000");
        assertFee("500000", 10, "10000");
        assertFee("500000", 30.01, "20000");
        assertFee("500001", 30, "0");
        assertFee("500001", 30.01, "20000");
        assertFee("500001", 40, "20000");
    }

    @Test
    void rejectsDeliveryBeyondFortyKilometers() {
        assertThrows(
                BadRequestException.class,
                () -> ShippingFeeCalculator.calculateFee(
                        BigDecimal.valueOf(100_000), 40.01
                )
        );
    }

    private void assertFee(String subtotal, double distanceKm, String expectedFee) {
        assertEquals(
                new BigDecimal(expectedFee),
                ShippingFeeCalculator.calculateFee(
                        new BigDecimal(subtotal), distanceKm
                )
        );
    }
}

package com.haui.UrbanMart.service;

import com.haui.UrbanMart.dto.request.AddressCreationRequest;
import com.haui.UrbanMart.dto.request.AddressUpdateRequest;
import com.haui.UrbanMart.dto.response.AddressResponse;
import com.haui.UrbanMart.entity.Address;
import com.haui.UrbanMart.entity.User;
import com.haui.UrbanMart.exception.ResourceNotFoundException;
import com.haui.UrbanMart.repository.AddressRepository;
import com.haui.UrbanMart.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<AddressResponse> getMyAddresses(UUID userId) {
        return addressRepository.findAllByUserId(userId)
                .stream()
                .map(AddressResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AddressResponse getMyAddress(UUID userId, UUID addressId) {
        return AddressResponse.from(findAddressOfUser(userId, addressId));
    }

    @Transactional
    public AddressResponse createAddress(
            UUID userId,
            AddressCreationRequest request
    ) {
        User user = findUser(userId);
        boolean firstAddress = addressRepository.countByUserId(userId) == 0;
        boolean shouldBeDefault = request.isDefault() || firstAddress;

        if (shouldBeDefault) {
            addressRepository.clearDefaultAddress(userId);
        }

        Address address = new Address();
        address.setUser(user);
        address.setRecipientName(request.getRecipientName());
        address.setPhone(request.getPhone());
        address.setLine(request.getLine());
        address.setWard(request.getWard());
        address.setDistrict(request.getDistrict());
        address.setCity(request.getCity());
        address.setLat(request.getLat());
        address.setLng(request.getLng());
        address.setDefault(shouldBeDefault);

        return AddressResponse.from(addressRepository.save(address));
    }

    @Transactional
    public AddressResponse updateAddress(
            UUID userId,
            UUID addressId,
            AddressUpdateRequest request
    ) {
        Address address = findAddressOfUser(userId, addressId);

        if (request.getRecipientName() != null) {
            address.setRecipientName(request.getRecipientName());
        }
        if (request.getPhone() != null) {
            address.setPhone(request.getPhone());
        }
        if (request.getLine() != null) {
            address.setLine(request.getLine());
        }
        if (request.getWard() != null) {
            address.setWard(request.getWard());
        }
        if (request.getDistrict() != null) {
            address.setDistrict(request.getDistrict());
        }
        if (request.getCity() != null) {
            address.setCity(request.getCity());
        }
        if (request.getLat() != null) {
            address.setLat(request.getLat());
        }
        if (request.getLng() != null) {
            address.setLng(request.getLng());
        }

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepository.clearDefaultAddress(userId);
            address.setDefault(true);
        } else if (Boolean.FALSE.equals(request.getIsDefault())) {
            address.setDefault(false);
        }

        return AddressResponse.from(addressRepository.save(address));
    }

    @Transactional
    public void deleteAddress(UUID userId, UUID addressId) {
        addressRepository.delete(findAddressOfUser(userId, addressId));
    }

    @Transactional
    public AddressResponse setDefaultAddress(UUID userId, UUID addressId) {
        Address address = findAddressOfUser(userId, addressId);
        addressRepository.clearDefaultAddress(userId);
        address.setDefault(true);
        return AddressResponse.from(addressRepository.save(address));
    }

    private Address findAddressOfUser(UUID userId, UUID addressId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Khong tim thay dia chi"
                ));
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Khong tim thay nguoi dung"
                ));
    }
}

package com.haui.UrbanMart.repository;

import com.haui.UrbanMart.entity.FoodTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodTagRepository extends JpaRepository<FoodTag, String> {

    List<FoodTag> findAllByOrderByCodeAsc();
}

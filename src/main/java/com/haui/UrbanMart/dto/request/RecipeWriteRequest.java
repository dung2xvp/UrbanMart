package com.haui.UrbanMart.dto.request;

import com.haui.UrbanMart.entity.IngredientGroup;
import com.haui.UrbanMart.entity.RecipeCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record RecipeWriteRequest(
        @NotBlank @Size(max = 200) String name,
        /** Bo trong thi backend tu sinh tu name (bo dau tieng Viet). */
        @Size(max = 200)
        @Pattern(
                regexp = "^[a-z0-9]+(-[a-z0-9]+)*$",
                message = "Slug chỉ gồm chữ thường không dấu, số và dấu gạch ngang"
        )
        String slug,
        @Size(max = 5000) String description,
        @Size(max = 500) String imageUrl,
        @NotNull RecipeCategory categoryMonAn,
        Boolean isActive,
        @NotEmpty @Size(max = 50) List<@Valid @NotNull Ingredient> ingredients
) {
    public record Ingredient(
            @NotNull UUID productId,
            @NotNull @Min(1) @Max(1000) Integer quantity,
            Boolean isRequired,
            /** Bo trong: isRequired=true -> MAIN, nguoc lai -> SPICE. */
            IngredientGroup group,
            @Size(max = 255) String note
    ) {
    }
}

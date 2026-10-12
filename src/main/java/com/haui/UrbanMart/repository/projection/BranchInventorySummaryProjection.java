package com.haui.UrbanMart.repository.projection;

import java.util.UUID;

public interface BranchInventorySummaryProjection {
    UUID getBranchId();

    String getBranchName();

    String getBranchAddress();

    String getBranchPhone();

    Long getTotalStock();
}

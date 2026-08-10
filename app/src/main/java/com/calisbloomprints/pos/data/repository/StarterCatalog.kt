package com.calisbloomprints.pos.data.repository

import com.calisbloomprints.pos.data.db.entity.ProductEntity
import com.calisbloomprints.pos.data.model.ProductCategory

object StarterCatalog {
    val products = listOf(
        ProductEntity(
            name = "Fresh Rose Bouquet",
            category = ProductCategory.BOUQUET,
            priceCents = 180000,
            trackStock = true,
            stockQuantity = 8,
            sortOrder = 10,
        ),
        ProductEntity(
            name = "Sunflower Bouquet",
            category = ProductCategory.BOUQUET,
            priceCents = 150000,
            trackStock = true,
            stockQuantity = 6,
            sortOrder = 20,
        ),
        ProductEntity(
            name = "Single Stem Rose",
            category = ProductCategory.FLOWER,
            priceCents = 15000,
            trackStock = true,
            stockQuantity = 40,
            sortOrder = 30,
        ),
        ProductEntity(
            name = "Baby's Breath Bundle",
            category = ProductCategory.FLOWER,
            priceCents = 35000,
            trackStock = true,
            stockQuantity = 20,
            sortOrder = 40,
        ),
        ProductEntity(
            name = "Gift Box",
            category = ProductCategory.GIFT,
            priceCents = 45000,
            trackStock = true,
            stockQuantity = 15,
            sortOrder = 50,
        ),
        ProductEntity(
            name = "Keepsake Frame",
            category = ProductCategory.GIFT,
            priceCents = 65000,
            trackStock = true,
            stockQuantity = 10,
            sortOrder = 60,
        ),
        ProductEntity(
            name = "Photo Print",
            category = ProductCategory.PRINTING,
            priceCents = 5000,
            trackStock = false,
            stockQuantity = null,
            sortOrder = 70,
        ),
        ProductEntity(
            name = "Card Print",
            category = ProductCategory.PRINTING,
            priceCents = 8000,
            trackStock = false,
            stockQuantity = null,
            sortOrder = 80,
        ),
        ProductEntity(
            name = "Custom Arrangement",
            category = ProductCategory.CUSTOM,
            priceCents = 100000,
            trackStock = false,
            stockQuantity = null,
            sortOrder = 90,
        ),
        ProductEntity(
            name = "Custom Keepsake Order",
            category = ProductCategory.CUSTOM,
            priceCents = 75000,
            trackStock = false,
            stockQuantity = null,
            sortOrder = 100,
        ),
    )
}

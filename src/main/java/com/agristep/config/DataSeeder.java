package com.agristep.config;

import com.agristep.domain.Crop;
import com.agristep.repository.CropRepository;
import com.agristep.repository.Transactions;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Runs at every start: adds crops from crops.json that are missing from the database (matched by code).
 * Because it matches by code, new crops reach a database created by an older version.
 * (Beds and settings belong to each user and are created when the account is made.)
 */
public class DataSeeder {
    private final CropRepository crops;
    private final Transactions transactions;

    public DataSeeder(CropRepository crops, Transactions transactions) {
        this.crops = crops;
        this.transactions = transactions;
    }

    public void run() throws IOException {
        List<Crop> catalogue;
        try (InputStream in = getClass().getResourceAsStream("/data/crops.json")) {
            if (in == null) throw new IOException("data/crops.json is missing from the application");
            catalogue = CropCatalog.load(in);
        }
        transactions.run(() -> {
            for (Crop crop : catalogue) {
                if (crops.findByCode(crop.getCode()).isEmpty()) {
                    crops.save(crop);
                }
            }
            return null;
        });
    }
}

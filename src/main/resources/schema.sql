-- PlantWise database schema. Works on H2 (in MySQL mode) and MySQL/MariaDB.
-- Safe to run on every start: tables are only created when missing.
-- Databases made by the earlier single-garden version are upgraded by SchemaMigrator (adds the user_id columns).

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(30) NOT NULL,
    username_key VARCHAR(30) NOT NULL UNIQUE,
    display_name VARCHAR(60) NOT NULL,
    password_hash VARCHAR(200) NOT NULL,
    created_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS crops (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(80) NOT NULL,
    english_name VARCHAR(80),
    scientific_name VARCHAR(120),
    emoji VARCHAR(16),
    cycle_days INT NOT NULL,
    sunlight VARCHAR(120),
    spacing VARCHAR(120),
    verified BOOLEAN NOT NULL,
    source_note VARCHAR(500),
    source_url VARCHAR(300),
    temp_abs_min DOUBLE, temp_opt_min DOUBLE, temp_opt_max DOUBLE, temp_abs_max DOUBLE,
    rain_abs_min DOUBLE, rain_opt_min DOUBLE, rain_opt_max DOUBLE, rain_abs_max DOUBLE,
    ph_abs_min DOUBLE, ph_opt_min DOUBLE, ph_opt_max DOUBLE, ph_abs_max DOUBLE
);

CREATE TABLE IF NOT EXISTS crop_optimal_soils (
    crop_id BIGINT NOT NULL,
    soil VARCHAR(20) NOT NULL,
    PRIMARY KEY (crop_id, soil),
    FOREIGN KEY (crop_id) REFERENCES crops (id)
);

CREATE TABLE IF NOT EXISTS crop_tolerable_soils (
    crop_id BIGINT NOT NULL,
    soil VARCHAR(20) NOT NULL,
    PRIMARY KEY (crop_id, soil),
    FOREIGN KEY (crop_id) REFERENCES crops (id)
);

CREATE TABLE IF NOT EXISTS crop_tips (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    crop_id BIGINT NOT NULL,
    category VARCHAR(10) NOT NULL,
    content VARCHAR(600) NOT NULL,
    FOREIGN KEY (crop_id) REFERENCES crops (id)
);

CREATE TABLE IF NOT EXISTS task_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    crop_id BIGINT NOT NULL,
    kind VARCHAR(20) NOT NULL,
    day_offset INT NOT NULL,
    amount DOUBLE,
    detail VARCHAR(500),
    extra VARCHAR(500),
    FOREIGN KEY (crop_id) REFERENCES crops (id)
);

CREATE TABLE IF NOT EXISTS plots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    slot_number INT NOT NULL,
    name VARCHAR(60) NOT NULL,
    crop_id BIGINT,
    planted_on DATE,
    CONSTRAINT uq_plots_user_slot UNIQUE (user_id, slot_number),
    FOREIGN KEY (crop_id) REFERENCES crops (id)
);

CREATE TABLE IF NOT EXISTS care_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plot_id BIGINT NOT NULL,
    task_type VARCHAR(20) NOT NULL,
    day_offset INT NOT NULL,
    due_date DATE NOT NULL,
    amount DOUBLE,
    detail VARCHAR(500),
    extra VARCHAR(500),
    completed BOOLEAN NOT NULL,
    completed_at DATETIME,
    FOREIGN KEY (plot_id) REFERENCES plots (id)
);

CREATE TABLE IF NOT EXISTS harvest_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    crop_name VARCHAR(80) NOT NULL,
    crop_emoji VARCHAR(16),
    plot_name VARCHAR(60) NOT NULL,
    planted_on DATE NOT NULL,
    harvested_on DATE NOT NULL,
    completion_percent INT NOT NULL
);

-- One row per user; the row's id is the owner's user id.
CREATE TABLE IF NOT EXISTS garden_settings (
    id BIGINT PRIMARY KEY,
    user_id BIGINT,
    location_name VARCHAR(160) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    soil VARCHAR(20) NOT NULL,
    grid_columns INT NOT NULL DEFAULT 3
);

CREATE TABLE IF NOT EXISTS climate_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    location_key VARCHAR(40) NOT NULL UNIQUE,
    location_name VARCHAR(160),
    avg_temp_30d_c DOUBLE NOT NULL,
    annual_rain_mm DOUBLE NOT NULL,
    fetched_at DATETIME NOT NULL
);

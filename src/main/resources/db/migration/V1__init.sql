CREATE TABLE courier_location (
    id BIGSERIAL PRIMARY KEY,
    courier_id VARCHAR NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    applied BOOLEAN NOT NULL,
    CONSTRAINT courier_location_courier_recorded_unique UNIQUE (courier_id, recorded_at)
);

CREATE TABLE courier_track (
    courier_id VARCHAR PRIMARY KEY,
    last_latitude DOUBLE PRECISION,
    last_longitude DOUBLE PRECISION,
    last_recorded_at TIMESTAMPTZ,
    total_distance_meters DOUBLE PRECISION NOT NULL DEFAULT 0
);

CREATE TABLE geofence_state (
    courier_id VARCHAR NOT NULL,
    store_id VARCHAR NOT NULL,
    inside BOOLEAN NOT NULL,
    last_entrance_at TIMESTAMPTZ,
    PRIMARY KEY (courier_id, store_id)
);

CREATE TABLE entrance_event (
    id BIGSERIAL PRIMARY KEY,
    courier_id VARCHAR NOT NULL,
    store_id VARCHAR NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT entrance_event_unique UNIQUE (courier_id, store_id, occurred_at)
);

CREATE INDEX idx_entrance_event_courier_store_occurred
    ON entrance_event (courier_id, store_id, occurred_at);

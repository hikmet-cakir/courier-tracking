package com.example.courier.infrastructure.store;

import com.example.courier.domain.model.Store;
import com.example.courier.domain.port.StoreCatalog;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
public final class JsonStoreCatalog implements StoreCatalog {

    private final List<Store> stores;

    public JsonStoreCatalog(
            StoresProperties properties, ResourceLoader resourceLoader, ObjectMapper objectMapper) {
        String location = properties.location();
        if (location == null || location.isBlank()) {
            throw new IllegalStateException("courier.stores.location must be set");
        }
        Resource resource = resourceLoader.getResource(location);
        if (!resource.exists()) {
            throw new IllegalStateException("Stores file not found: " + location);
        }
        try (InputStream input = resource.getInputStream()) {
            this.stores = List.copyOf(parse(objectMapper, input, location));
        } catch (IllegalStateException | IllegalArgumentException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read stores from " + location + ": " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<Store> findAll() {
        return stores;
    }

    private static List<Store> parse(ObjectMapper objectMapper, InputStream input, String location)
            throws IOException {
        List<StoreFileEntry> entries;
        try {
            entries = objectMapper.readValue(input, new TypeReference<>() {});
        } catch (IOException ex) {
            throw new IllegalStateException("Stores file is not valid JSON: " + location, ex);
        }
        if (entries == null || entries.isEmpty()) {
            throw new IllegalStateException("Stores file contains no stores: " + location);
        }

        List<Store> parsed = new ArrayList<>(entries.size());
        Set<String> seenIds = new HashSet<>();
        for (int i = 0; i < entries.size(); i++) {
            StoreFileEntry entry = entries.get(i);
            if (entry == null) {
                throw new IllegalStateException("Store entry is null at index " + i + " in " + location);
            }
            if (entry.name() == null || entry.name().isBlank()) {
                throw new IllegalStateException("Store name is missing at index " + i + " in " + location);
            }
            if (entry.lat() == null) {
                throw new IllegalStateException("Store lat is missing at index " + i + " in " + location);
            }
            if (entry.lng() == null) {
                throw new IllegalStateException("Store lng is missing at index " + i + " in " + location);
            }
            Store store;
            try {
                store = new Store(idFromName(entry.name()), entry.name(), entry.lat(), entry.lng());
            } catch (IllegalArgumentException ex) {
                throw new IllegalStateException(
                        "Invalid store at index " + i + " in " + location + ": " + ex.getMessage(), ex);
            }
            if (!seenIds.add(store.id())) {
                throw new IllegalStateException("Duplicate store id '" + store.id() + "' in " + location);
            }
            parsed.add(store);
        }
        return parsed;
    }

    private static String idFromName(String name) {
        return UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private record StoreFileEntry(String name, Double lat, Double lng) {}
}

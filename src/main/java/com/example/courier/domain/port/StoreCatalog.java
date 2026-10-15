package com.example.courier.domain.port;

import com.example.courier.domain.model.Store;
import java.util.List;

public interface StoreCatalog {

    List<Store> findAll();
}

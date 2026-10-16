package com.example.courier.domain.port;

import com.example.courier.domain.model.EntranceEvent;
import java.util.List;

public interface EntranceEventRepository {

    void save(EntranceEvent event);

    List<EntranceEvent> find(String courierId, String storeId);
}

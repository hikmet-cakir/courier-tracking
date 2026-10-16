package com.example.courier.api;

import com.example.courier.api.dto.EntranceEventResponse;
import com.example.courier.domain.port.EntranceEventRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class EntranceEventController {

    private final EntranceEventRepository entranceEvents;

    public EntranceEventController(EntranceEventRepository entranceEvents) {
        this.entranceEvents = entranceEvents;
    }

    @GetMapping("/entrance-events")
    public List<EntranceEventResponse> list(
            @RequestParam(required = false) String courierId, @RequestParam(required = false) String storeId) {
        return entranceEvents.find(courierId, storeId).stream()
                .map(EntranceEventResponse::from)
                .toList();
    }
}

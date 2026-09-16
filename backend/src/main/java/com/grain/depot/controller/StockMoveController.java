package com.grain.depot.controller;

import com.grain.depot.entity.StockMove;
import com.grain.depot.service.StockMoveService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class StockMoveController {

    private final StockMoveService service;

    public StockMoveController(StockMoveService service) {
        this.service = service;
    }

    @GetMapping("/moves")
    public List<StockMove> list(@RequestParam(required = false) Long batchId,
                                @RequestParam(required = false) String moveType,
                                @RequestParam(required = false) String status) {
        return service.list(batchId, moveType, status);
    }

    @PostMapping("/moves")
    public StockMove create(@RequestBody StockMove input) {
        return service.create(input);
    }

    @PostMapping("/moves/{id}/execute")
    public StockMove execute(@PathVariable Long id) {
        return service.execute(id);
    }
}

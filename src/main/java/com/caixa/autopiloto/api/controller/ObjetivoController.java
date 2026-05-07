package com.caixa.autopiloto.api.controller;

import com.caixa.autopiloto.domain.port.InsightGenerator;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/analise")
public class ObjetivoController {

    private final InsightGenerator ai;

    public ObjetivoController(InsightGenerator ai) {
        this.ai = ai;
    }

    @PostMapping
    public String fazerAnalise(@RequestBody String objetivo) {
        return ai.gerar(objetivo);
    }
@GetMapping("/teste")
public String teste() {
    return "Backend funcionando";
}
}
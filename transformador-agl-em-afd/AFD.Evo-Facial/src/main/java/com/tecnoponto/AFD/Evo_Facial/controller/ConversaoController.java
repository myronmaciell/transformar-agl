package com.tecnoponto.AFD.Evo_Facial.controller;

import com.tecnoponto.AFD.Evo_Facial.service.ProcessamentoService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/api/conversoes")

public class ConversaoController {

    private final ProcessamentoService processamentoService;

    public ConversaoController(
            ProcessamentoService processamentoService
    ) {
        this.processamentoService = processamentoService;
    }

    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE
    )
    public ResponseEntity<byte[]> converter(
            @RequestParam("arquivo") MultipartFile arquivo
    ) throws IOException {

        if (arquivo.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body("Arquivo vazio.".getBytes(StandardCharsets.UTF_8));
        }

        try (
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                arquivo.getInputStream(),
                                StandardCharsets.UTF_8
                        )
                )
        ) {

            List<String> linhas = reader.lines().toList();

            String resultado =
                    processamentoService.processar(linhas);

            byte[] conteudo =
                    resultado.getBytes(StandardCharsets.UTF_8);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(
                            "text/plain;charset=UTF-8"
                    ))
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"marcacoes_secullum.txt\""
                    )
                    .body(conteudo);

        } catch (IllegalArgumentException |
                 DateTimeParseException e) {

            return ResponseEntity.badRequest()
                    .body(e.getMessage().getBytes(StandardCharsets.UTF_8));
        }
    }
}


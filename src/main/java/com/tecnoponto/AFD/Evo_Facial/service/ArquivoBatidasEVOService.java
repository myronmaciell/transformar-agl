package com.tecnoponto.AFD.Evo_Facial.service;

import com.tecnoponto.AFD.Evo_Facial.model.Marcacao;
import com.tecnoponto.AFD.Evo_Facial.parser.EVOParser;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class ArquivoBatidasEVOService {

    private final EVOParser evoParser;

    public ArquivoBatidasEVOService(EVOParser evoParser) {
        this.evoParser = evoParser;
    }

    public List<Marcacao> lerArquivo(Path caminho) {

        if (caminho == null) {
            throw new IllegalArgumentException(
                    "O caminho do arquivo EVO não pode ser nulo."
            );
        }

        if (!Files.exists(caminho)) {
            throw new IllegalArgumentException(
                    "Arquivo EVO não encontrado: " + caminho
            );
        }

        if (!Files.isRegularFile(caminho)) {
            throw new IllegalArgumentException(
                    "O caminho informado não corresponde a um arquivo válido."
            );
        }

        try {

            List<String> linhas = Files.readAllLines(
                    caminho,
                    StandardCharsets.UTF_8
            );

            if (linhas.isEmpty()) {
                throw new IllegalArgumentException(
                        "O arquivo EVO está vazio."
                );
            }

            return evoParser.interpretar(linhas);

        } catch (IOException e) {

            throw new RuntimeException(
                    "Erro ao ler o arquivo EVO: " + e.getMessage(),
                    e
            );
        }
    }
}

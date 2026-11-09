package com.tecnoponto.AFD.Evo_Facial.service;

import com.tecnoponto.AFD.Evo_Facial.model.Marcacao;
import com.tecnoponto.AFD.Evo_Facial.parser.EVOParser;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ProcessamentoService {

    private final EVOParser evoParser;
    private final ValidacaoService validacaoService;
    private final ConversaoService conversaoService;

    public ProcessamentoService(
            EVOParser evoParser,
            ValidacaoService validacaoService,
            ConversaoService conversaoService
    ) {
        this.evoParser = evoParser;
        this.validacaoService = validacaoService;
        this.conversaoService = conversaoService;
    }

    public String processar(List<String> linhas) {

        // 1. Interpretar arquivo
        List<Marcacao> marcacoes =
                evoParser.interpretar(linhas);

        // 2. Validar marcações
        List<String> erros =
                validacaoService.validar(marcacoes);

        // 3. Impedir conversão se houver erros
        if (!erros.isEmpty()) {
            throw new IllegalArgumentException(
                    String.join("\n", erros)
            );
        }

        // 4. Gerar arquivo convertido
        return conversaoService.converter(marcacoes);
    }
}
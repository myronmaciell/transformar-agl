package com.tecnoponto.AFD.Evo_Facial.service;

import com.tecnoponto.AFD.Evo_Facial.model.Marcacao;
import org.springframework.stereotype.Service;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ValidacaoService {

    public List<String> validar(List<Marcacao> marcacoes) {

        Set<String> registrosEncontrados = new HashSet<>();
        List<String> erros = new java.util.ArrayList<>();

        if (marcacoes == null || marcacoes.isEmpty()) {
            erros.add("Nenhuma marcação encontrada.");
            return erros;
        }

        for (int i = 0; i < marcacoes.size(); i++) {

            Marcacao marcacao = marcacoes.get(i);

            if (marcacao == null) {
                erros.add("Marcação inválida na posição " + (i + 1));
                continue;
            }

            String identificador = marcacao.identificadorFuncionario();

            if (identificador == null || identificador.isBlank()) {
                erros.add("Identificador vazio na posição " + (i + 1));
                continue;
            }

            String chave = identificador + "|"
                    + marcacao.data() + "|"
                    + marcacao.hora();

            if (!registrosEncontrados.add(chave)) {
                erros.add(
                        "Marcação duplicada: funcionário "
                                + identificador
                                + ", data "
                                + marcacao.data()
                                + ", horário "
                                + marcacao.hora()
                );
            }
        }

        return erros;
    }
}

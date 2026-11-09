package com.tecnoponto.AFD.Evo_Facial.service;

import com.tecnoponto.AFD.Evo_Facial.model.Marcacao;
import org.springframework.stereotype.Service;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ConversaoService {

    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final DateTimeFormatter FORMATO_HORA =
            DateTimeFormatter.ofPattern("HH:mm");

    public String converter(List<Marcacao> marcacoes) {

        if (marcacoes == null || marcacoes.isEmpty()) {
            throw new IllegalArgumentException(
                    "Não existem marcações para converter."
            );
        }

        StringBuilder arquivo = new StringBuilder();

        // Cabeçalho
        arquivo.append("Identificador\tData\tHora\n");

        // Converter cada marcação
        for (Marcacao marcacao : marcacoes) {

            arquivo.append(marcacao.identificadorFuncionario())
                    .append("\t")
                    .append(marcacao.data().format(FORMATO_DATA))
                    .append("\t")
                    .append(marcacao.hora().format(FORMATO_HORA))
                    .append("\n");
        }

        return arquivo.toString();
    }
}
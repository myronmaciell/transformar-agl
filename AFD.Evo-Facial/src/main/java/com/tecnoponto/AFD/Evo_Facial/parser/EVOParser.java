package com.tecnoponto.AFD.Evo_Facial.parser;

import com.tecnoponto.AFD.Evo_Facial.model.Marcacao;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class EVOParser {

    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA_BR = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter HORA_CURTA = DateTimeFormatter.ofPattern("HH:mm");

    // Regex para capturar data e hora no formato ISO 8601 presente nos AFDs (Ex: 2026-10-19T07:41:00-0300)
    private static final Pattern PATTERN_ISO_TIMESTAMP =
            Pattern.compile("(\\d{4}-\\d{2}-\\d{2})T(\\d{2}:\\d{2}:\\d{2})");

    public List<Marcacao> parse(List<String> linhas) {
        List<Marcacao> marcacoes = new ArrayList<>();

        if (linhas == null || linhas.isEmpty()) {
            throw new IllegalArgumentException("O arquivo informado está vazio.");
        }

        for (int i = 0; i < linhas.size(); i++) {
            String linha = linhas.get(i);

            if (linha == null || linha.isBlank()) {
                continue;
            }

            linha = linha.replace("\uFEFF", "").trim();

            // 1. TENTA PROCESSAR COMO AFD OFICIAL (PORTARIA 671 / REP-P / REP-C)
            Matcher matcherIso = PATTERN_ISO_TIMESTAMP.matcher(linha);
            if (matcherIso.find()) {
                try {
                    LocalDate data = LocalDate.parse(matcherIso.group(1));
                    LocalTime hora = LocalTime.parse(matcherIso.group(2));

                    // Extrai o PIS/CPF do funcionário (sequência numérica de 11 a 12 dígitos após a data/hora)
                    String identificador = extrairIdentificadorAFD(linha);

                    if (identificador != null && !identificador.isBlank()) {
                        marcacoes.add(new Marcacao(identificador, data, hora));
                        continue;
                    }
                } catch (Exception ignored) {
                    // Se não for um registro válido de marcação (ex: cabeçalho Tipo 1 ou reboque Tipo 9), pula a linha
                }
            }

            // 2. TENTA PROCESSAR COMO ARQUIVO AGL/TXT SIMPLES (3 CAMPOS SEPARADOS POR TAB OU PONTO E VÍRGULA)
            String[] campos = linha.split("[;\\t]", -1);
            if (campos.length == 3) {
                try {
                    String identificador = campos[0].trim();
                    LocalDate data = interpretarData(campos[1].trim());
                    LocalTime hora = interpretarHora(campos[2].trim());

                    marcacoes.add(new Marcacao(identificador, data, hora));
                } catch (RuntimeException ignored) {
                    // Ignora linhas de cabeçalho descritivo
                }
            }
        }

        if (marcacoes.isEmpty()) {
            throw new IllegalArgumentException("Nenhuma marcação válida foi encontrada no arquivo enviado.");
        }

        return marcacoes;
    }

    private String extrairIdentificadorAFD(String linha) {
        // No layout REP-P da Portaria 671, o PIS/CPF de 11 a 12 dígitos fica localizado logo após a primeira Timestamp ISO
        Matcher matcherPis = Pattern.compile("T\\d{2}:\\d{2}:\\d{2}[+-]\\d{4}(\\d{11,12})").matcher(linha);
        if (matcherPis.find()) {
            return matcherPis.group(1);
        }

        // Fallback: busca qualquer sequência de 11 dígitos que represente o PIS/CPF
        Matcher mDigits = Pattern.compile("\\b\\d{11,12}\\b").matcher(linha);
        if (mDigits.find()) {
            return mDigits.group();
        }

        return "DESCONHECIDO";
    }

    private LocalDate interpretarData(String valor) {
        try { return LocalDate.parse(valor, DATA_BR); } catch (DateTimeParseException ignored) {}
        try { return LocalDate.parse(valor); } catch (DateTimeParseException ignored) {}
        try { return OffsetDateTime.parse(valor).toLocalDate(); } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Data inválida: " + valor);
        }
    }

    private LocalTime interpretarHora(String valor) {
        try { return LocalTime.parse(valor, HORA_BR); } catch (DateTimeParseException ignored) {}
        try { return LocalTime.parse(valor, HORA_CURTA); } catch (DateTimeParseException ignored) {}
        try { return LocalTime.parse(valor); } catch (DateTimeParseException ignored) {}
        try { return OffsetDateTime.parse(valor).toLocalTime(); } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Hora inválida: " + valor);
        }
    }

    public List<Marcacao> interpretar(List<String> linhas) {
        return parse(linhas);
    }
}
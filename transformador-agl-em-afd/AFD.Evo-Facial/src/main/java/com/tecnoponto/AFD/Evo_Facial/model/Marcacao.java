package com.tecnoponto.AFD.Evo_Facial.model;

import java.time.LocalDate;
import java.time.LocalTime;

public record Marcacao (

        String identificadorFuncionario,
        LocalDate data,
        LocalTime hora
) {

    public Marcacao {
        if (identificadorFuncionario == null ||
                identificadorFuncionario.isBlank()) {
            throw new IllegalArgumentException(
                    "Identificador do funcionário é obrigatório."
            );
        }

        if (data == null) {
            throw new IllegalArgumentException(
                    "Data da marcação é obrigatória."
            );
        }

        if (hora == null) {
            throw new IllegalArgumentException(
                    "Horário da marcação é obrigatório."
            );
        }
    }
}

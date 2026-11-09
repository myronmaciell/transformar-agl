package com.tecnoponto.AFD.Evo_Facial;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TransformadorAGLFrame extends JFrame {

    private JLabel lblStatus;
    private JButton btnSelecionar;
    private JButton btnConverter;
    private File arquivoSelecionado;

    public TransformadorAGLFrame() {
        super("Transformador de AGL");
        configurarJanela();
        criarComponentes();
    }

    private void configurarJanela() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 320);
        setLocationRelativeTo(null);
        setResizable(false);
    }

    private void criarComponentes() {
        JPanel container = new JPanel(new BorderLayout(15, 15));
        container.setBorder(new EmptyBorder(20, 20, 20, 20));
        container.setBackground(new Color(15, 23, 42)); // Azul escuro estilo tema web

        // Título
        JLabel lblTitulo = new JLabel("Transformador de AGL", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitulo.setForeground(Color.WHITE);
        container.add(lblTitulo, BorderLayout.NORTH);

        // Painel Central (Seleção de Ficheiro)
        JPanel painelCentral = new JPanel(new GridLayout(2, 1, 10, 10));
        painelCentral.setOpaque(false);

        btnSelecionar = new JButton("Selecionar Arquivo (.txt / .agl)");
        estilarBotao(btnSelecionar, new Color(30, 41, 59), Color.WHITE);
        btnSelecionar.addActionListener(e -> selecionarArquivo());

        lblStatus = new JLabel("Nenhum arquivo selecionado", SwingConstants.CENTER);
        lblStatus.setForeground(new Color(148, 163, 184));
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        painelCentral.add(btnSelecionar);
        painelCentral.add(lblStatus);
        container.add(painelCentral, BorderLayout.CENTER);

        // Painel Inferior (Botão de Converter)
        btnConverter = new JButton("Converter arquivo");
        estilarBotao(btnConverter, new Color(37, 99, 235), Color.WHITE);
        btnConverter.setEnabled(false);
        btnConverter.setPreferredSize(new Dimension(0, 45));
        btnConverter.addActionListener(e -> converterEGuardar());

        container.add(btnConverter, BorderLayout.SOUTH);

        add(container);
    }

    private void estilarBotao(JButton btn, Color bg, Color fg) {
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void selecionarArquivo() {
        JFileChooser fileChooser = new JFileChooser();
        int res = fileChooser.showOpenDialog(this);

        if (res == JFileChooser.APPROVE_OPTION) {
            arquivoSelecionado = fileChooser.getSelectedFile();
            lblStatus.setText("Selecionado: " + arquivoSelecionado.getName());
            lblStatus.setForeground(new Color(52, 211, 153)); // Verde
            btnConverter.setEnabled(true);
        }
    }

    private void converterEGuardar() {
        if (arquivoSelecionado == null) return;

        List<String> marcacoes = processarArquivo(arquivoSelecionado);

        if (marcacoes.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Nenhuma marcação válida foi encontrada no arquivo.",
                    "Erro de Conversão",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Escolher onde guardar o ficheiro AFD convertido
        JFileChooser fileSaver = new JFileChooser();
        fileSaver.setSelectedFile(new File("afd_convertido.txt"));
        int res = fileSaver.showSaveDialog(this);

        if (res == JFileChooser.APPROVE_OPTION) {
            File arquivoSaida = fileSaver.getSelectedFile();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(arquivoSaida, StandardCharsets.UTF_8))) {
                for (String m : marcacoes) {
                    writer.write(m);
                    writer.newLine();
                }
                JOptionPane.showMessageDialog(this,
                        "Arquivo convertido com sucesso!\nSalvo em: " + arquivoSaida.getAbsolutePath(),
                        "Sucesso",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                        "Erro ao salvar o arquivo: " + ex.getMessage(),
                        "Erro",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private List<String> processarArquivo(File file) {
        List<String> resultado = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String linha;
            int contadorLinha = 0;

            while ((linha = reader.readLine()) != null) {
                contadorLinha++;
                linha = linha.trim();
                if (linha.isEmpty()) continue;

                // Ignora o cabeçalho inicial
                if (contadorLinha == 1 && linha.toLowerCase().contains("no") && linha.toLowerCase().contains("datetime")) {
                    continue;
                }

                // 1. TENTA LER NO FORMATO SEPARADO POR PONTO E VÍRGULA (ex: 3;06/01/2024;07:51:32)
                Pattern pSemicolon = Pattern.compile("^(\\d+);(\\d{2}/\\d{2}/\\d{4});(\\d{2}:\\d{2})");
                Matcher mSemicolon = pSemicolon.matcher(linha);
                if (mSemicolon.find()) {
                    resultado.add(mSemicolon.group(1) + ";" + mSemicolon.group(2) + ";" + mSemicolon.group(3));
                    continue;
                }

                // 2. TENTA LER NO FORMATO TABULAR DO AGL (ex: 2024-01-06 07:51:32 ...)
                Pattern pAgl = Pattern.compile("(\\d{4}[-/]\\d{2}[-/]\\d{2})\\s+(\\d{2}:\\d{2}:\\d{2})");
                Matcher mAgl = pAgl.matcher(linha);

                if (mAgl.find()) {
                    String dataOriginal = mAgl.group(1);
                    String hora = mAgl.group(2).substring(0, 5);
                    String[] partes = linha.split("\\s+");
                    String identificador = null;

                    if (partes.length >= 3 && partes[2].matches("\\d+")) {
                        identificador = partes[2];
                    } else {
                        for (int p = 1; p < partes.length; p++) {
                            if (partes[p].matches("\\d{1,12}") && !partes[p].equals(partes[0])) {
                                identificador = partes[p];
                                break;
                            }
                        }
                    }

                    if (identificador != null) {
                        String[] dataSplit = dataOriginal.replace("-", "/").split("/");
                        String dataBr = dataSplit[2] + "/" + dataSplit[1] + "/" + dataSplit[0];
                        resultado.add(identificador + ";" + dataBr + ";" + hora);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return resultado;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new TransformadorAGLFrame().setVisible(true);
        });
    }
}
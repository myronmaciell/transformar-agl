document.addEventListener('DOMContentLoaded', () => {
    const fileInput = document.getElementById('fileInput');
    const uploadArea = document.getElementById('uploadArea');
    const fileInfo = document.getElementById('fileInfo');
    const fileNameSpan = document.getElementById('fileName');
    const btnRemove = document.getElementById('btnRemove');
    const btnConverter = document.getElementById('btnConverter');
    const statusDiv = document.getElementById('status');

    let arquivoSelecionado = null;

    // Permite clicar na caixa pontilhada para abrir a janela de seleção de arquivo
    uploadArea.addEventListener('click', () => {
        fileInput.click();
    });

    // Eventos de Drag and Drop
    uploadArea.addEventListener('dragover', (e) => {
        e.preventDefault();
        uploadArea.style.borderColor = '#007bff';
    });

    uploadArea.addEventListener('dragleave', () => {
        uploadArea.style.borderColor = '';
    });

    uploadArea.addEventListener('drop', (e) => {
        e.preventDefault();
        uploadArea.style.borderColor = '';
        if (e.dataTransfer.files.length > 0) {
            tratarArquivoSelecionado(e.dataTransfer.files[0]);
        }
    });

    // Quando o arquivo é selecionado pelo input nativo
    fileInput.addEventListener('change', function() {
        if (fileInput.files.length > 0) {
            tratarArquivoSelecionado(fileInput.files[0]);
        }
    });

    function tratarArquivoSelecionado(file) {
        arquivoSelecionado = file;
        fileNameSpan.textContent = file.name;

        // Alterna a visibilidade dos elementos na DOM
        uploadArea.style.display = 'none';
        fileInfo.style.display = 'flex';

        btnConverter.disabled = false;
        esconderStatus();
    }

    // Botão para remover o arquivo selecionado
    btnRemove.addEventListener('click', (e) => {
        e.stopPropagation();
        fileInput.value = '';
        arquivoSelecionado = null;

        // Restaura a visibilidade da caixa de upload
        fileInfo.style.display = 'none';
        uploadArea.style.display = 'block';

        btnConverter.disabled = true;
        esconderStatus();
    });

    // Ação do Botão de Converter
    btnConverter.addEventListener('click', () => {
        if (!arquivoSelecionado) return;

        const reader = new FileReader();

        reader.onerror = function() {
            mostrarStatus("Erro ao ler o arquivo.", true);
        };

        reader.onload = function(e) {
            try {
                const conteudo = e.target.result;
                const linhas = conteudo.split(/\r?\n/);
                const marcacoes = [];

                for (let i = 0; i < linhas.length; i++) {
                    let linha = linhas[i].trim();
                    if (!linha) continue;

                    // Ignora o cabeçalho inicial
                    if (i === 0 && linha.toLowerCase().includes('no') && linha.toLowerCase().includes('datetime')) {
                        continue;
                    }

                    // 1. TENTA LER NO FORMATO TABULAR DO AGL
                    const matchDataHora = linha.match(/(\d{4}[-/]\d{2}[-/]\d{2})\s+(\d{2}:\d{2}:\d{2})/);

                    if (matchDataHora) {
                        const dataOriginal = matchDataHora[1];
                        const horaCompleta = matchDataHora[2];
                        const hora = horaCompleta.substring(0, 5);

                        const partes = linha.split(/\s+/);
                        let identificador = null;

                        if (partes.length >= 3 && /^\d+$/.test(partes[2])) {
                            identificador = partes[2];
                        }

                        if (!identificador) {
                            for (let p = 1; p < partes.length; p++) {
                                if (/^\d{1,12}$/.test(partes[p]) && partes[p] !== partes[0]) {
                                    identificador = partes[p];
                                    break;
                                }
                            }
                        }

                        if (identificador) {
                            const dataLimpa = dataOriginal.replace(/-/g, '/');
                            const [ano, mes, dia] = dataLimpa.split('/');
                            const dataBr = `${dia}/${mes}/${ano}`;

                            // Saída com ponto e vírgula
                            marcacoes.push(`${identificador};${dataBr};${hora}`);
                            continue;
                        }
                    }

                    // 2. TENTA PROCESSAR COMO AFD OFICIAL (Portaria 671)
                    const matchIso = linha.match(/(\d{4}-\d{2}-\d{2})T(\d{2}:\d{2}:\d{2})/);
                    if (matchIso) {
                        const dataIso = matchIso[1];
                        const hora = matchIso[2].substring(0, 5);
                        const matchPis = linha.match(/T\d{2}:\d{2}:\d{2}[+-]\d{4}(\d{11,12})/);
                        let identificador = matchPis ? matchPis[1] : null;

                        if (!identificador) {
                            const digits = linha.match(/\b\d{11,12}\b/);
                            if (digits) identificador = digits[0];
                        }

                        if (identificador) {
                            const [ano, mes, dia] = dataIso.split('-');
                            const dataBr = `${dia}/${mes}/${ano}`;

                            // Saída com ponto e vírgula
                            marcacoes.push(`${identificador};${dataBr};${hora}`);
                            continue;
                        }
                    }

                    // 3. TENTA LER NO FORMATO SEPARADO POR PONTO E VÍRGULA (ex: 3;06/01/2024;07:51:32)
                    const matchSemicolon = linha.match(/^(\d+);(\d{2}\/\d{2}\/\d{4});(\d{2}:\d{2})/);
                    if (matchSemicolon) {
                        const id = matchSemicolon[1];
                        const data = matchSemicolon[2];
                        const hora = matchSemicolon[3];

                        // Saída com ponto e vírgula
                        marcacoes.push(`${id};${data};${hora}`);
                        continue;
                    }
                }

                if (marcacoes.length === 0) {
                    throw new Error("Nenhuma marcação válida foi encontrada no arquivo. Verifique o formato.");
                }

                // Gera o conteúdo final e dispara o download automático
                const textoFinal = marcacoes.join('\r\n');
                const blob = new Blob([textoFinal], { type: 'text/plain;charset=utf-8' });
                const url = URL.createObjectURL(blob);

                const linkTemp = document.createElement('a');
                linkTemp.href = url;
                linkTemp.download = 'afd_convertido.txt';
                document.body.appendChild(linkTemp);
                linkTemp.click();
                document.body.removeChild(linkTemp);
                URL.revokeObjectURL(url);

                mostrarStatus("Arquivo convertido e baixado com sucesso!", false);

            } catch (err) {
                mostrarStatus(err.message, true);
            }
        };

        reader.readAsText(arquivoSelecionado, 'UTF-8');
    });

    function mostrarStatus(mensagem, ehErro) {
        statusDiv.textContent = mensagem;
        statusDiv.hidden = false;
        statusDiv.style.color = ehErro ? '#dc3545' : '#28a745';
    }

    function esconderStatus() {
        statusDiv.hidden = true;
        statusDiv.textContent = '';
    }
});
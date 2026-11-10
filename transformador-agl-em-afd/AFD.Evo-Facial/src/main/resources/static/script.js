document.addEventListener('DOMContentLoaded', () => {
    const fileInput = document.getElementById('fileInput');
    const uploadArea = document.getElementById('uploadArea');
    const fileInfo = document.getElementById('fileInfo');
    const fileNameSpan = document.getElementById('fileName');
    const btnRemove = document.getElementById('btnRemove');
    const btnConverter = document.getElementById('btnConverter');
    const statusDiv = document.getElementById('status');

    let arquivoSelecionado = null;

    // --- VARIÁVEIS DO RATE LIMITER ---
    let tempoUltimaConversao = 0;
    const INTERVALO_LIMITE_MS = 3000; // Tempo de espera (3 segundos)

    // --- LÓGICA DO MODAL DE AJUDA ---
    const btnAjuda = document.getElementById('btnAjuda');
    const modalAjuda = document.getElementById('modalAjuda');
    const btnCloseModal = document.getElementById('btnCloseModal');

    function abrirModal() {
        if (modalAjuda) {
            modalAjuda.removeAttribute('hidden');
            modalAjuda.style.display = 'flex';
        }
    }

    function fecharModal() {
        if (modalAjuda) {
            modalAjuda.setAttribute('hidden', '');
            modalAjuda.style.display = 'none';
        }
    }

    if (btnAjuda) {
        btnAjuda.addEventListener('click', (e) => {
            e.stopPropagation();
            abrirModal();
        });
    }

    if (btnCloseModal) {
        btnCloseModal.addEventListener('click', (e) => {
            e.preventDefault();
            e.stopPropagation();
            fecharModal();
        });
    }

    if (modalAjuda) {
        modalAjuda.addEventListener('click', (e) => {
            if (e.target === modalAjuda) {
                fecharModal();
            }
        });
    }

    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && modalAjuda && modalAjuda.style.display !== 'none') {
            fecharModal();
        }
    });

    // --- LÓGICA DE DRAG & DROP E FILE INPUT ---
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

    fileInput.addEventListener('change', function() {
        if (fileInput.files.length > 0) {
            tratarArquivoSelecionado(fileInput.files[0]);
        }
    });

    function tratarArquivoSelecionado(file) {
        arquivoSelecionado = file;
        fileNameSpan.textContent = file.name;

        uploadArea.style.display = 'none';
        fileInfo.style.display = 'flex';
        fileInfo.removeAttribute('hidden');

        btnConverter.disabled = false;
        esconderStatus();
    }

    btnRemove.addEventListener('click', (e) => {
        e.stopPropagation();
        fileInput.value = '';
        arquivoSelecionado = null;

        fileInfo.style.display = 'none';
        fileInfo.setAttribute('hidden', '');
        uploadArea.style.display = 'block';

        btnConverter.disabled = true;
        esconderStatus();
    });

    // --- LÓGICA DE CONVERSÃO (COM RATE LIMITER) ---
    btnConverter.addEventListener('click', () => {
        if (!arquivoSelecionado) return;

        const agora = Date.now();

        // Checagem de Rate Limit
        if (agora - tempoUltimaConversao < INTERVALO_LIMITE_MS) {
            const segundosRestantes = Math.ceil((INTERVALO_LIMITE_MS - (agora - tempoUltimaConversao)) / 1000);
            mostrarStatus(`Aguarde ${segundosRestantes} segundo(s) para converter novamente.`, true);
            return;
        }

        // Atualiza a hora da conversão atual
        tempoUltimaConversao = agora;

        // Desabilita o botão temporariamente
        btnConverter.disabled = true;

        const reader = new FileReader();

        reader.onerror = function() {
            mostrarStatus("Erro ao ler o arquivo.", true);
            btnConverter.disabled = false;
        };

        reader.onload = function(e) {
            try {
                const conteudo = e.target.result;
                const linhas = conteudo.split(/\r?\n/);
                const marcacoes = [];

                for (let i = 0; i < linhas.length; i++) {
                    let linha = linhas[i].trim();
                    if (!linha) continue;

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

                            marcacoes.push(`${identificador};${dataBr};${hora}`);
                            continue;
                        }
                    }

                    // 3. TENTA LER NO FORMATO SEPARADO POR PONTO E VÍRGULA
                    const matchSemicolon = linha.match(/^(\d+);(\d{2}\/\d{2}\/\d{4});(\d{2}:\d{2})/);
                    if (matchSemicolon) {
                        const id = matchSemicolon[1];
                        const data = matchSemicolon[2];
                        const hora = matchSemicolon[3];

                        marcacoes.push(`${id};${data};${hora}`);
                        continue;
                    }
                }

                if (marcacoes.length === 0) {
                    throw new Error("Nenhuma marcação válida foi encontrada no arquivo. Verifique o formato.");
                }

                const textoFinal = marcacoes.join('\r\n');
                const blob = new Blob([textoFinal], { type: 'text/plain;charset=utf-8' });
                const url = URL.createObjectURL(blob);
                const linkTemp = document.createElement('a');
                linkTemp.href = url;
                linkTemp.download = 'AGL_Convertido.txt';
                document.body.appendChild(linkTemp);
                linkTemp.click();
                document.body.removeChild(linkTemp);
                URL.revokeObjectURL(url);

                mostrarStatus("Arquivo convertido e baixado com sucesso!", false);

            } catch (err) {
                mostrarStatus(err.message, true);
            } finally {
                // Habilita novamente o botão para novos envios
                btnConverter.disabled = false;
            }
        };

        reader.readAsText(arquivoSelecionado, 'UTF-8');
    });

    function mostrarStatus(mensagem, ehErro) {
        statusDiv.textContent = mensagem;
        statusDiv.hidden = false;
        statusDiv.style.display = 'block';
        statusDiv.className = ehErro ? 'status error' : 'status success';
    }

    function esconderStatus() {
        statusDiv.hidden = true;
        statusDiv.style.display = 'none';
        statusDiv.textContent = '';
    }
});
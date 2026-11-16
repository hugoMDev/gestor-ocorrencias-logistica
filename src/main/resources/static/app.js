/**
 * Gestor de Ocorrências Logísticas - Frontend Controller (app.js)
 * Conexão com API REST (/api/ocorrencias), Autocomplete OpenStreetMap/Nominatim,
 * Integração com Google Maps, Atualização de Status Dinâmica e KPIs em tempo real.
 */

// Estado global da aplicação
let todasOcorrencias = [];

// Mapeamentos amigáveis de enums legados para exibição
const POLOS_LABELS = {
    'ITUPEVA': 'Itupeva - SP (CD Central)',
    'PAULINIA': 'Paulínia - SP (Hub Petroquímico)',
    'SAO_JOSE_DOS_CAMPOS': 'São José dos Campos - SP (Vale do Paraíba)',
    'CAPITAL_SP': 'Capital - SP (CD Urbano)',
    'OUTRO': 'Outro / Rota Interestadual'
};

const TIPOS_LABELS = {
    'AVARIA': 'Avaria na Carga',
    'DESVIO_ROTA': 'Desvio de Rota / Telemetria',
    'ATRASO_CARGA': 'Atraso Carga / Descarga',
    'SINISTRO': 'Sinistro / Acidente na Via',
    'FALHA_VEICULO': 'Falha Mecânica / Parada',
    'OUTRO': 'Outro Incidente'
};

// ==========================================================================
// Inicialização ao carregar a página
// ==========================================================================
document.addEventListener('DOMContentLoaded', () => {
    iniciarRelogio();
    configurarMascaraPlaca();
    configurarAutocompletePolo();
    configurarEventosFormulario();
    configurarFiltros();
    configurarEventosAutenticacao();
    verificarSessaoInicial();
});

// ==========================================================================
// Relógio em tempo real no cabeçalho
// ==========================================================================
function iniciarRelogio() {
    const clockEl = document.getElementById('clock-display');
    if (!clockEl) return;

    function atualizar() {
        const agora = new Date();
        clockEl.textContent = agora.toLocaleTimeString('pt-BR', { hour12: false });
    }
    atualizar();
    setInterval(atualizar, 1000);
}

// ==========================================================================
// Máscara e Formatação Automática da Placa (ABC-1234 / BRA-2E19)
// ==========================================================================
function configurarMascaraPlaca() {
    const inputPlaca = document.getElementById('placaVeiculo');
    if (!inputPlaca) return;

    inputPlaca.addEventListener('input', (e) => {
        let val = e.target.value.toUpperCase().replace(/[^A-Z0-9]/g, '');

        if (val.length > 7) {
            val = val.substring(0, 7);
        }

        if (val.length > 3) {
            val = val.substring(0, 3) + '-' + val.substring(3);
        }

        e.target.value = val;
    });
}

// ==========================================================================
// Autocomplete Inteligente de Localização (Nominatim / OpenStreetMap)
// ==========================================================================
function configurarAutocompletePolo() {
    const inputPolo = document.getElementById('poloRegional');
    const listContainer = document.getElementById('polo-autocomplete-list');
    const iconSpinner = document.getElementById('polo-spinner-icon');

    if (!inputPolo || !listContainer) return;

    let debounceTimeout = null;
    let abortController = null;

    inputPolo.addEventListener('input', (e) => {
        const query = e.target.value.trim();

        clearTimeout(debounceTimeout);
        if (abortController) {
            abortController.abort();
            abortController = null;
        }

        if (query.length < 3) {
            listContainer.innerHTML = '';
            listContainer.classList.add('hidden');
            return;
        }

        // Aguarda 400ms após a última digitação (debounce)
        debounceTimeout = setTimeout(async () => {
            try {
                if (iconSpinner) iconSpinner.style.opacity = '0.4';
                listContainer.innerHTML = `
                    <div class="autocomplete-loading">
                        <span class="btn-spinner" style="width: 14px; height: 14px; border-width: 2px;"></span>
                        <span>Buscando cidades e rodovias no OpenStreetMap...</span>
                    </div>
                `;
                listContainer.classList.remove('hidden');

                abortController = new AbortController();
                const url = `https://nominatim.openstreetmap.org/search?format=json&countrycodes=br&limit=5&q=${encodeURIComponent(query)}`;

                const resposta = await fetch(url, {
                    signal: abortController.signal,
                    headers: { 'Accept': 'application/json' }
                });

                if (!resposta.ok) {
                    throw new Error('Falha na resposta do serviço de mapas');
                }

                const resultados = await resposta.json();

                if (!Array.isArray(resultados) || resultados.length === 0) {
                    listContainer.innerHTML = `
                        <div class="autocomplete-loading" style="color: var(--text-muted);">
                            <span>Nenhum local sugerido. Você pode prosseguir com a digitação livre.</span>
                        </div>
                    `;
                    return;
                }

                listContainer.innerHTML = resultados.map((item, idx) => {
                    const addr = item.address || {};
                    const cidade = addr.city || addr.town || addr.municipality || addr.village || '';
                    const estado = addr.state ? (addr['ISO3166-2-lvl4'] ? addr['ISO3166-2-lvl4'].replace('BR-', '') : addr.state) : '';
                    const rodovia = addr.road || '';

                    let titulo = '';
                    if (rodovia && cidade) {
                        titulo = `${rodovia}, ${cidade}${estado ? ' - ' + estado : ''}`;
                    } else if (cidade) {
                        titulo = `${cidade}${estado ? ' - ' + estado : ''}`;
                    } else {
                        titulo = item.name || query;
                    }

                    const subtitulo = item.display_name;
                    const valorCompleto = item.display_name || titulo;

                    return `
                        <div class="autocomplete-item" data-index="${idx}" data-valor="${escapeHtml(valorCompleto)}" title="${escapeHtml(subtitulo)}">
                            <div class="autocomplete-item-icon">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                    <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path>
                                    <circle cx="12" cy="10" r="3"></circle>
                                </svg>
                            </div>
                            <div class="autocomplete-item-content">
                                <span class="autocomplete-item-title">${escapeHtml(titulo)}</span>
                                <span class="autocomplete-item-sub">${escapeHtml(subtitulo)}</span>
                            </div>
                        </div>
                    `;
                }).join('');

                // Seleção dos itens via mousedown com preventDefault() para evitar que o blur do input feche a lista
                listContainer.querySelectorAll('.autocomplete-item').forEach(itemEl => {
                    itemEl.addEventListener('mousedown', (event) => {
                        event.preventDefault();
                        const valorSelecionado = itemEl.getAttribute('data-valor');
                        inputPolo.value = valorSelecionado;
                        listContainer.innerHTML = '';
                        listContainer.classList.add('hidden');
                    });
                });

            } catch (err) {
                if (err.name === 'AbortError') return;
                console.warn('Erro ao consultar Nominatim:', err);
                listContainer.classList.add('hidden');
            } finally {
                if (iconSpinner) iconSpinner.style.opacity = '1';
            }
        }, 400);
    });

    // Fecha dropdown se o usuário clicar fora
    document.addEventListener('click', (e) => {
        if (!inputPolo.contains(e.target) && !listContainer.contains(e.target)) {
            listContainer.classList.add('hidden');
        }
    });

    // Fecha dropdown ao pressionar Escape
    inputPolo.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            listContainer.classList.add('hidden');
        }
    });
}

// ==========================================================================
// Requisições à API (/api/ocorrencias)
// ==========================================================================

/**
 * Carrega a lista de ocorrências via GET /api/ocorrencias
 */
async function carregarOcorrencias() {
    const loader = document.getElementById('estado-carregando');
    const vazio = document.getElementById('estado-vazio');
    const corpoTabela = document.getElementById('tabela-corpo');

    try {
        if (loader) loader.classList.remove('hidden');
        if (vazio) vazio.classList.add('hidden');

        const resposta = await authFetch('/api/ocorrencias', {
            method: 'GET',
            headers: { 'Accept': 'application/json' }
        });

        if (!resposta.ok) {
            throw new Error(`Erro na API (${resposta.status} - ${resposta.statusText})`);
        }

        const dados = await resposta.json();

        // Ordena das mais recentes para as mais antigas
        todasOcorrencias = (Array.isArray(dados) ? dados : []).sort((a, b) => {
            const dataA = a.dataHora ? new Date(a.dataHora).getTime() : 0;
            const dataB = b.dataHora ? new Date(b.dataHora).getTime() : 0;
            return dataB - dataA;
        });

        atualizarKPIs(todasOcorrencias);
        renderizarTabela();

    } catch (erro) {
        console.error('Falha ao carregar ocorrências:', erro);
        mostrarToast('Erro ao carregar ocorrências da API: ' + erro.message, 'erro');
        if (corpoTabela) corpoTabela.innerHTML = '';
        if (vazio) vazio.classList.remove('hidden');
    } finally {
        if (loader) loader.classList.add('hidden');
    }
}

/**
 * Registra uma nova ocorrência via POST /api/ocorrencias
 */
async function salvarOcorrencia(evento) {
    evento.preventDefault();

    const form = document.getElementById('form-ocorrencia');
    const btnSalvar = document.getElementById('btn-salvar');
    const btnSpinner = document.getElementById('btn-spinner');

    const placaVeiculo = document.getElementById('placaVeiculo').value.trim();
    const motorista = document.getElementById('motorista').value.trim();
    const monitorResponsavel = document.getElementById('monitorResponsavel').value.trim();
    const poloRegional = document.getElementById('poloRegional').value.trim();
    const tipoOcorrencia = document.getElementById('tipoOcorrencia').value;
    const gravidade = document.getElementById('gravidade').value;
    const status = document.getElementById('status').value;
    const descricaoFatos = document.getElementById('descricaoFatos').value.trim();

    if (!placaVeiculo && !motorista && !descricaoFatos) {
        mostrarToast('Preencha ao menos a placa, motorista ou descrição da ocorrência.', 'erro');
        document.getElementById('placaVeiculo').focus();
        return;
    }

    const payload = {
        placaVeiculo: placaVeiculo || null,
        motorista: motorista || null,
        monitorResponsavel: monitorResponsavel || 'Operador Central',
        poloRegional: poloRegional || null,
        tipoOcorrencia: tipoOcorrencia || null,
        gravidade: gravidade || 'MEDIA',
        status: status || 'ABERTO',
        descricaoFatos: descricaoFatos || null
    };

    btnSalvar.disabled = true;
    if (btnSpinner) btnSpinner.classList.remove('hidden');

    try {
        const resposta = await authFetch('/api/ocorrencias', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify(payload)
        });

        if (!resposta.ok) {
            const erroCorpo = await resposta.text();
            throw new Error(erroCorpo || `Erro ${resposta.status}: ${resposta.statusText}`);
        }

        const ocorrenciaCriada = await resposta.json();
        const codigoFormatado = ocorrenciaCriada.codigoOco || 'Registrada';
        mostrarToast(`Ocorrência #${codigoFormatado} registrada com sucesso!`, 'sucesso');

        limparFormulario();
        await carregarOcorrencias();

    } catch (erro) {
        console.error('Falha ao salvar ocorrência:', erro);
        mostrarToast('Erro ao salvar ocorrência: ' + erro.message, 'erro');
    } finally {
        btnSalvar.disabled = false;
        if (btnSpinner) btnSpinner.classList.add('hidden');
    }
}

// ==========================================================================
// Atualização de Status Dinâmica na Linha (PATCH /api/ocorrencias/{id}/status)
// ==========================================================================
async function alterarStatusOcorrencia(selectEl) {
    const id = selectEl.getAttribute('data-id');
    const codigo = selectEl.getAttribute('data-codigo') || id;
    const novoStatus = selectEl.value;
    const statusAnterior = selectEl.getAttribute('data-anterior') || 'ABERTO';

    selectEl.disabled = true;

    try {
        const resposta = await authFetch(`/api/ocorrencias/${encodeURIComponent(id)}/status`, {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify({ status: novoStatus })
        });

        if (!resposta.ok) {
            const erroTexto = await resposta.text();
            throw new Error(erroTexto || `Erro ${resposta.status}`);
        }

        // Atualiza objeto em memória
        const index = todasOcorrencias.findIndex(o => o.id === id || o.codigoOco === codigo);
        if (index !== -1) {
            todasOcorrencias[index].status = novoStatus;
        }

        // Atualiza estilos da linha
        const stNorm = normalizarStatus(novoStatus);
        const classeStatus = stNorm === 'EM_ANDAMENTO' ? 'andamento' : (stNorm === 'CONCLUIDO' ? 'concluido' : 'aberto');
        selectEl.className = `status-select ${classeStatus}`;
        selectEl.setAttribute('data-anterior', novoStatus);

        // Atualiza botão de ação rápida na mesma linha
        const tr = selectEl.closest('tr');
        if (tr) {
            const acoesCell = tr.querySelector('.acoes-cell');
            if (acoesCell) {
                acoesCell.innerHTML = stNorm !== 'CONCLUIDO'
                    ? `<button type="button" class="btn-action btn-action-concluir" 
                             onclick="concluirRapido('${id}', '${escapeHtml(codigo)}', this)" 
                             title="Concluir ocorrência imediatamente">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                            <polyline points="20 6 9 17 4 12"></polyline>
                        </svg>
                        Concluir
                      </button>`
                    : `<button type="button" class="btn-action btn-action-reabrir" 
                             onclick="reabrirRapido('${id}', '${escapeHtml(codigo)}', this)" 
                             title="Reabrir ocorrência para Aberto">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                            <polyline points="1 4 1 10 7 10"></polyline>
                            <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"></path>
                        </svg>
                        Reabrir
                      </button>`;
            }
        }

        // Atualiza os cartões de KPI instantaneamente
        atualizarKPIs(todasOcorrencias);

        const rotulos = { 'ABERTO': 'Aberto', 'EM_ANDAMENTO': 'Em Andamento', 'CONCLUIDO': 'Concluído' };
        mostrarToast(`Status da ocorrência #${codigo} alterado para ${rotulos[stNorm] || novoStatus}!`, 'sucesso');

    } catch (erro) {
        console.error('Falha ao atualizar status:', erro);
        mostrarToast('Erro ao atualizar status: ' + erro.message, 'erro');
        selectEl.value = statusAnterior;
    } finally {
        selectEl.disabled = false;
    }
}

/**
 * Ação rápida de 1-clique para concluir a ocorrência
 */
function concluirRapido(id, codigo, btnEl) {
    const tr = btnEl.closest('tr');
    const selectEl = tr ? tr.querySelector('.status-select') : null;
    if (selectEl) {
        selectEl.value = 'CONCLUIDO';
        alterarStatusOcorrencia(selectEl);
    }
}

/**
 * Ação rápida de 1-clique para reabrir a ocorrência
 */
function reabrirRapido(id, codigo, btnEl) {
    const tr = btnEl.closest('tr');
    const selectEl = tr ? tr.querySelector('.status-select') : null;
    if (selectEl) {
        selectEl.value = 'ABERTO';
        alterarStatusOcorrencia(selectEl);
    }
}

// ==========================================================================
// Configuração de Eventos
// ==========================================================================
function configurarEventosFormulario() {
    const form = document.getElementById('form-ocorrencia');
    if (form) {
        form.addEventListener('submit', salvarOcorrencia);
    }

    const btnLimpar = document.getElementById('btn-limpar');
    if (btnLimpar) {
        btnLimpar.addEventListener('click', limparFormulario);
    }

    const btnRecarregar = document.getElementById('btn-recarregar');
    if (btnRecarregar) {
        btnRecarregar.addEventListener('click', () => {
            mostrarToast('Atualizando dados das ocorrências...', 'info');
            carregarOcorrencias();
        });
    }
}

function limparFormulario() {
    const form = document.getElementById('form-ocorrencia');
    if (form) form.reset();

    const poloList = document.getElementById('polo-autocomplete-list');
    if (poloList) {
        poloList.innerHTML = '';
        poloList.classList.add('hidden');
    }

    const monitor = document.getElementById('monitorResponsavel');
    if (monitor) monitor.value = 'Operador Central';

    const gravidade = document.getElementById('gravidade');
    if (gravidade) gravidade.value = 'MEDIA';

    const status = document.getElementById('status');
    if (status) status.value = 'ABERTO';
}

function configurarFiltros() {
    const filtroBusca = document.getElementById('filtro-busca');
    const filtroStatus = document.getElementById('filtro-status');

    if (filtroBusca) {
        filtroBusca.addEventListener('input', () => renderizarTabela());
    }

    if (filtroStatus) {
        filtroStatus.addEventListener('change', () => renderizarTabela());
    }
}

// ==========================================================================
// Renderização da Tabela, Mapas e Dropdowns de Status
// ==========================================================================
function renderizarTabela() {
    const corpoTabela = document.getElementById('tabela-corpo');
    const vazio = document.getElementById('estado-vazio');
    const badgeTotal = document.getElementById('badge-total-ocorrencias');
    const filtroTexto = (document.getElementById('filtro-busca')?.value || '').toLowerCase().trim();
    const filtroStatus = document.getElementById('filtro-status')?.value || 'TODOS';

    if (!corpoTabela) return;

    // Aplica filtros locais
    const filtradas = todasOcorrencias.filter(item => {
        if (filtroStatus !== 'TODOS') {
            const st = normalizarStatus(item.status);
            if (st !== filtroStatus) return false;
        }

        if (filtroTexto) {
            const codigo = (item.codigoOco || '').toLowerCase();
            const placa = (item.placaVeiculo || '').toLowerCase();
            const motorista = (item.motorista || '').toLowerCase();
            const monitor = (item.monitorResponsavel || '').toLowerCase();
            const polo = (POLOS_LABELS[item.poloRegional] || item.poloRegional || '').toLowerCase();
            const tipo = (TIPOS_LABELS[item.tipoOcorrencia] || item.tipoOcorrencia || '').toLowerCase();
            const desc = (item.descricaoFatos || '').toLowerCase();

            return codigo.includes(filtroTexto) ||
                   placa.includes(filtroTexto) ||
                   motorista.includes(filtroTexto) ||
                   monitor.includes(filtroTexto) ||
                   polo.includes(filtroTexto) ||
                   tipo.includes(filtroTexto) ||
                   desc.includes(filtroTexto);
        }

        return true;
    });

    if (badgeTotal) {
        badgeTotal.textContent = filtradas.length;
    }

    if (filtradas.length === 0) {
        corpoTabela.innerHTML = '';
        if (vazio) vazio.classList.remove('hidden');
        return;
    }

    if (vazio) vazio.classList.add('hidden');

    corpoTabela.innerHTML = filtradas.map(item => {
        const id = item.id || item.codigoOco;
        const codigo = escapeHtml(item.codigoOco || 'S/N');
        const dataFormatada = formatarDataHora(item.dataHora);
        const placaFormatada = item.placaVeiculo ? escapeHtml(item.placaVeiculo) : '<span class="text-muted">—</span>';
        const motorista = item.motorista ? escapeHtml(item.motorista) : '<span class="text-muted">—</span>';
        
        // Polo Regional com integração Google Maps
        const poloTexto = item.poloRegional ? (POLOS_LABELS[item.poloRegional] || item.poloRegional) : '';
        const poloHtml = poloTexto
            ? `<div class="polo-cell">
                 <span class="polo-nome" title="${escapeHtml(poloTexto)}">${escapeHtml(poloTexto)}</span>
                 <a href="https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(poloTexto)}" 
                    target="_blank" 
                    rel="noopener noreferrer" 
                    class="btn-maps" 
                    title="Abrir '${escapeHtml(poloTexto)}' no Google Maps">
                     <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                         <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path>
                         <circle cx="12" cy="10" r="3"></circle>
                     </svg>
                     <span>Ver no Maps</span>
                 </a>
               </div>`
            : '<span class="text-muted">—</span>';

        const tipo = TIPOS_LABELS[item.tipoOcorrencia] || item.tipoOcorrencia || '<span class="text-muted">—</span>';
        const monitor = item.monitorResponsavel ? escapeHtml(item.monitorResponsavel) : '<span class="text-muted">—</span>';
        const desc = item.descricaoFatos ? escapeHtml(item.descricaoFatos) : '<span class="text-muted">—</span>';

        // Dropdown de Status Interativo
        const stNorm = normalizarStatus(item.status);
        const classeStatus = stNorm === 'EM_ANDAMENTO' ? 'andamento' : (stNorm === 'CONCLUIDO' ? 'concluido' : 'aberto');
        const statusHtml = `
            <select class="status-select ${classeStatus}" 
                    data-id="${id}" 
                    data-codigo="${escapeHtml(item.codigoOco || '')}"
                    data-anterior="${stNorm}"
                    onchange="alterarStatusOcorrencia(this)"
                    title="Alterar status operacional">
                <option value="ABERTO" ${stNorm === 'ABERTO' ? 'selected' : ''}>Aberto</option>
                <option value="EM_ANDAMENTO" ${stNorm === 'EM_ANDAMENTO' ? 'selected' : ''}>Em Andamento</option>
                <option value="CONCLUIDO" ${stNorm === 'CONCLUIDO' ? 'selected' : ''}>Concluído</option>
            </select>
        `;

        // Botão de Ação Rápida
        const acoesHtml = stNorm !== 'CONCLUIDO'
            ? `<button type="button" class="btn-action btn-action-concluir" 
                     onclick="concluirRapido('${id}', '${escapeHtml(item.codigoOco || '')}', this)" 
                     title="Concluir ocorrência imediatamente">
                 <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                     <polyline points="20 6 9 17 4 12"></polyline>
                 </svg>
                 Concluir
               </button>`
            : `<button type="button" class="btn-action btn-action-reabrir" 
                     onclick="reabrirRapido('${id}', '${escapeHtml(item.codigoOco || '')}', this)" 
                     title="Reabrir ocorrência para Aberto">
                 <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                     <polyline points="1 4 1 10 7 10"></polyline>
                     <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"></path>
                 </svg>
                 Reabrir
               </button>`;

        return `
            <tr>
                <td class="col-codigo">${codigo}</td>
                <td class="col-data">${dataFormatada}</td>
                <td><span class="col-placa">${placaFormatada}</span></td>
                <td><strong>${motorista}</strong></td>
                <td>${poloHtml}</td>
                <td>${tipo}</td>
                <td>${renderizarBadgeGravidade(item.gravidade)}</td>
                <td class="col-status">${statusHtml}</td>
                <td>${monitor}</td>
                <td class="col-desc" title="${escapeHtml(item.descricaoFatos || '')}">${desc}</td>
                <td class="acoes-cell">${acoesHtml}</td>
            </tr>
        `;
    }).join('');
}

// Normaliza status para filtros e agrupamentos
function normalizarStatus(status) {
    if (!status) return 'ABERTO';
    const s = status.toUpperCase();
    if (s === 'PENDENTE' || s === 'ABERTO') return 'ABERTO';
    if (s === 'EM_ANALISE' || s === 'EM_ANDAMENTO') return 'EM_ANDAMENTO';
    if (s === 'RESOLVIDO' || s === 'CONCLUIDO') return 'CONCLUIDO';
    return s;
}

// Renderiza Badge de Gravidade
function renderizarBadgeGravidade(gravidade) {
    const g = (gravidade || 'MEDIA').toUpperCase();
    let classe = 'badge-grav-media';
    let rotulo = 'Média';

    switch (g) {
        case 'BAIXA':
            classe = 'badge-grav-baixa';
            rotulo = 'Baixa';
            break;
        case 'MEDIA':
            classe = 'badge-grav-media';
            rotulo = 'Média';
            break;
        case 'ALTA':
            classe = 'badge-grav-alta';
            rotulo = 'Alta';
            break;
        case 'CRITICA':
        case 'CRÍTICA':
            classe = 'badge-grav-critica';
            rotulo = 'Crítica';
            break;
    }

    return `<span class="badge ${classe}"><span class="badge-dot"></span>${rotulo}</span>`;
}

// ==========================================================================
// Atualização de Métricas / KPIs
// ==========================================================================
function atualizarKPIs(lista) {
    const elTotal = document.getElementById('kpi-total');
    const elAbertos = document.getElementById('kpi-abertos');
    const elAndamento = document.getElementById('kpi-andamento');
    const elCriticos = document.getElementById('kpi-criticos');

    let total = lista.length;
    let abertos = 0;
    let andamento = 0;
    let criticos = 0;

    lista.forEach(item => {
        const st = normalizarStatus(item.status);
        if (st === 'ABERTO') abertos++;
        if (st === 'EM_ANDAMENTO') andamento++;

        const grav = (item.gravidade || '').toUpperCase();
        if (grav === 'CRITICA' || grav === 'CRÍTICA' || grav === 'ALTA') {
            criticos++;
        }
    });

    if (elTotal) elTotal.textContent = total;
    if (elAbertos) elAbertos.textContent = abertos;
    if (elAndamento) elAndamento.textContent = andamento;
    if (elCriticos) elCriticos.textContent = criticos;
}

// ==========================================================================
// Formatação de Data / Hora (dd/MM/yyyy HH:mm)
// ==========================================================================
function formatarData(valorData) {
    if (!valorData) return '—';

    try {
        const d = new Date(valorData);
        if (isNaN(d.getTime())) return '—';

        const dia = String(d.getDate()).padStart(2, '0');
        const mes = String(d.getMonth() + 1).padStart(2, '0');
        const ano = d.getFullYear();
        const hora = String(d.getHours()).padStart(2, '0');
        const minuto = String(d.getMinutes()).padStart(2, '0');

        return `${dia}/${mes}/${ano} ${hora}:${minuto}`;
    } catch {
        return '—';
    }
}

function formatarDataHora(valorData) {
    return formatarData(valorData);
}

// ==========================================================================
// Sistema de Toasts (Notificações Flutuantes Discretas)
// ==========================================================================
function mostrarToast(mensagem, tipo = 'sucesso') {
    const container = document.getElementById('toast-container');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = `toast toast-${tipo}`;

    let iconeSvg = '';
    let titulo = 'Informação';

    if (tipo === 'sucesso') {
        titulo = 'Sucesso';
        iconeSvg = `
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                <polyline points="22 4 12 14.01 9 11.01"></polyline>
            </svg>
        `;
    } else if (tipo === 'erro') {
        titulo = 'Atenção';
        iconeSvg = `
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="12" y1="8" x2="12" y2="12"></line>
                <line x1="12" y1="16" x2="12.01" y2="16"></line>
            </svg>
        `;
    } else {
        iconeSvg = `
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="12" y1="16" x2="12" y2="12"></line>
                <line x1="12" y1="8" x2="12.01" y2="8"></line>
            </svg>
        `;
    }

    toast.innerHTML = `
        <div class="toast-icon">${iconeSvg}</div>
        <div class="toast-body">
            <span class="toast-title">${titulo}</span>
            <span class="toast-msg">${escapeHtml(mensagem)}</span>
        </div>
        <button class="toast-close" title="Fechar">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="18" y1="6" x2="6" y2="18"></line>
                <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
        </button>
    `;

    const closeBtn = toast.querySelector('.toast-close');
    closeBtn.addEventListener('click', () => removerToast(toast));

    container.appendChild(toast);

    setTimeout(() => {
        removerToast(toast);
    }, 4000);
}

function removerToast(toast) {
    if (!toast || toast.classList.contains('hiding')) return;
    toast.classList.add('hiding');
    setTimeout(() => {
        if (toast.parentElement) {
            toast.parentElement.removeChild(toast);
        }
    }, 300);
}

// ==========================================================================
// Utilitários de Segurança
// ==========================================================================
function escapeHtml(texto) {
    if (texto === null || texto === undefined) return '';
    return String(texto)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

// ==========================================================================
// Módulo de Autenticação Corporativa Superfrio (Ciclo de 15 Dias)
// ==========================================================================
const AUTH_STORAGE_KEY = 'superfrio_auth_token';
const USER_STORAGE_KEY = 'superfrio_auth_user';

let usuarioLogado = null;
let authToken = null;

function obterToken() {
    if (authToken) return authToken;
    authToken = localStorage.getItem(AUTH_STORAGE_KEY);
    return authToken;
}

/**
 * Wrapper de fetch que injeta o Bearer token e intercepta 401 para bloquear a tela
 */
async function authFetch(url, options = {}) {
    const token = obterToken();
    const headers = Object.assign({}, options.headers || {});

    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    const mergedOptions = { ...options, headers };

    const resposta = await fetch(url, mergedOptions);

    if (resposta.status === 401) {
        console.warn('Requisição não autorizada (401). Bloqueando tela e limpando sessão.');
        encerrarSessaoLocal();
        exibirTelaBloqueio();
        mostrarToast('Sessão expirada ou não autorizada. Por favor, acesse novamente.', 'erro');
        throw new Error('Sessão expirada (401)');
    }

    return resposta;
}

async function verificarSessaoInicial() {
    const token = obterToken();
    if (!token) {
        exibirTelaBloqueio();
        return;
    }

    try {
        const resposta = await fetch('/api/auth/me', {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (!resposta.ok) {
            throw new Error('Token inválido ou sessão encerrada');
        }

        const usuario = await resposta.json();
        iniciarSessao(usuario, token, false);
    } catch (e) {
        console.warn('Falha na validação da sessão existente:', e);
        encerrarSessaoLocal();
        exibirTelaBloqueio();
    }
}

function iniciarSessao(usuario, token, exibirToastBoasVindas = true) {
    usuarioLogado = usuario;
    authToken = token;

    localStorage.setItem(AUTH_STORAGE_KEY, token);
    localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(usuario));

    atualizarUIUsuario(usuario);
    ocultarTelaBloqueio();

    // Carrega dados protegidos da central e contador da equipe
    carregarOcorrencias();
    atualizarContadorEquipe();

    if (exibirToastBoasVindas) {
        mostrarToast(`Bem-vindo(a), ${usuario.nome || usuario.username}! Central Superfrio conectada.`, 'sucesso');
    }
}

function encerrarSessaoLocal() {
    usuarioLogado = null;
    authToken = null;
    localStorage.removeItem(AUTH_STORAGE_KEY);
    localStorage.removeItem(USER_STORAGE_KEY);
    limparDadosInterface();
}

function limparDadosInterface() {
    todasOcorrencias = [];
    atualizarKPIs([]);
    const corpoTabela = document.getElementById('tabela-corpo');
    if (corpoTabela) corpoTabela.innerHTML = '';
    const vazio = document.getElementById('estado-vazio');
    if (vazio) vazio.classList.add('hidden');
    const equipeCount = document.getElementById('equipe-count');
    if (equipeCount) equipeCount.textContent = '--/10';
}

function exibirTelaBloqueio() {
    const overlay = document.getElementById('auth-overlay');
    const viewLogin = document.getElementById('auth-view-login');
    const viewExpirada = document.getElementById('auth-view-expirada');
    const loginAlerta = document.getElementById('login-alerta');

    if (overlay) overlay.classList.remove('hidden');
    if (viewLogin) viewLogin.classList.remove('hidden');
    if (viewExpirada) viewExpirada.classList.add('hidden');
    if (loginAlerta) loginAlerta.classList.add('hidden');

    limparDadosInterface();
}

function ocultarTelaBloqueio() {
    const overlay = document.getElementById('auth-overlay');
    if (overlay) overlay.classList.add('hidden');
}

function atualizarUIUsuario(usuario) {
    if (!usuario) return;

    const nameEl = document.getElementById('header-user-name');
    const avatarEl = document.getElementById('header-user-avatar');
    const roleEl = document.getElementById('header-user-role');

    if (nameEl) nameEl.textContent = usuario.nome || usuario.username;

    if (avatarEl) {
        const partes = (usuario.nome || usuario.username).trim().split(/\s+/);
        let iniciais = partes[0] ? partes[0][0] : 'U';
        if (partes.length > 1) {
            iniciais += partes[partes.length - 1][0];
        }
        avatarEl.textContent = iniciais.toUpperCase().substring(0, 2);
    }

    if (roleEl) {
        if (usuario.admin) {
            roleEl.textContent = 'ADMIN';
            roleEl.className = 'user-pill-role admin';
        } else {
            roleEl.textContent = 'OPERADOR';
            roleEl.className = 'user-pill-role';
        }
    }
}

function exibirAlerta(el, mensagem, tipo = 'erro') {
    if (!el) return;
    el.textContent = mensagem;
    el.className = `auth-alert ${tipo === 'aviso' ? 'alert-warning' : (tipo === 'sucesso' ? 'alert-success' : '')}`;
    el.classList.remove('hidden');
}

// ==========================================================================
// Configuração de Eventos de Autenticação, Senhas e Equipe
// ==========================================================================
function configurarEventosAutenticacao() {
    // 1. Formulário de Login Principal
    const formLogin = document.getElementById('form-login');
    if (formLogin) {
        formLogin.addEventListener('submit', submeterLogin);
    }

    // 2. Formulário de Senha Expirada (Ciclo de 15 dias)
    const formExpirada = document.getElementById('form-expirada');
    if (formExpirada) {
        formExpirada.addEventListener('submit', submeterTrocaSenhaExpirada);
    }

    const btnExpiradaVoltar = document.getElementById('btn-expirada-voltar');
    if (btnExpiradaVoltar) {
        btnExpiradaVoltar.addEventListener('click', () => {
            const viewLogin = document.getElementById('auth-view-login');
            const viewExpirada = document.getElementById('auth-view-expirada');
            if (viewExpirada) viewExpirada.classList.add('hidden');
            if (viewLogin) viewLogin.classList.remove('hidden');
        });
    }

    // 3. Botão Sair no cabeçalho
    const btnLogout = document.getElementById('btn-logout');
    if (btnLogout) {
        btnLogout.addEventListener('click', fazerLogout);
    }

    // 4. Modal Alteração Voluntária de Senha
    const btnAlterarSenhaHeader = document.getElementById('btn-alterar-senha-header');
    if (btnAlterarSenhaHeader) {
        btnAlterarSenhaHeader.addEventListener('click', abrirModalAlterarSenha);
    }

    const btnFecharModalSenha = document.getElementById('btn-fechar-modal-senha');
    const btnCancelarModalSenha = document.getElementById('btn-cancelar-modal-senha');
    const fecharModalSenhaHandler = () => {
        const modal = document.getElementById('modal-alterar-senha');
        if (modal) modal.classList.add('hidden');
    };
    if (btnFecharModalSenha) btnFecharModalSenha.addEventListener('click', fecharModalSenhaHandler);
    if (btnCancelarModalSenha) btnCancelarModalSenha.addEventListener('click', fecharModalSenhaHandler);

    const formModalSenha = document.getElementById('form-modal-senha');
    if (formModalSenha) {
        formModalSenha.addEventListener('submit', submeterModalAlterarSenha);
    }

    // 5. Modal de Aviso de Expiração (Últimos 5 dias)
    const btnAvisoAlterar = document.getElementById('btn-aviso-alterar-agora');
    if (btnAvisoAlterar) {
        btnAvisoAlterar.addEventListener('click', () => {
            const modalAviso = document.getElementById('modal-aviso-expiracao');
            if (modalAviso) modalAviso.classList.add('hidden');
            abrirModalAlterarSenha();
        });
    }

    const btnAvisoLembrar = document.getElementById('btn-aviso-lembrar-amanha');
    if (btnAvisoLembrar) {
        btnAvisoLembrar.addEventListener('click', () => {
            const modalAviso = document.getElementById('modal-aviso-expiracao');
            if (modalAviso) modalAviso.classList.add('hidden');
        });
    }

    // 6. Modal Gestão de Equipe (Máx 10)
    const btnEquipe = document.getElementById('btn-equipe');
    if (btnEquipe) {
        btnEquipe.addEventListener('click', abrirModalEquipe);
    }

    const btnFecharModalEquipe = document.getElementById('btn-fechar-modal-equipe');
    if (btnFecharModalEquipe) {
        btnFecharModalEquipe.addEventListener('click', () => {
            const modal = document.getElementById('modal-equipe');
            if (modal) modal.classList.add('hidden');
        });
    }

    const formNovoUsuario = document.getElementById('form-novo-usuario');
    if (formNovoUsuario) {
        formNovoUsuario.addEventListener('submit', cadastrarNovoUsuario);
    }

    // 7. Modal Edição de Usuário da Equipe
    const btnFecharModalEditar = document.getElementById('btn-fechar-modal-editar-usuario');
    const btnCancelarModalEditar = document.getElementById('btn-cancelar-modal-editar-usuario');
    const fecharModalEditarHandler = () => {
        const modal = document.getElementById('modal-editar-usuario');
        if (modal) modal.classList.add('hidden');
    };
    if (btnFecharModalEditar) btnFecharModalEditar.addEventListener('click', fecharModalEditarHandler);
    if (btnCancelarModalEditar) btnCancelarModalEditar.addEventListener('click', fecharModalEditarHandler);

    const formEditarUsuario = document.getElementById('form-editar-usuario');
    if (formEditarUsuario) {
        formEditarUsuario.addEventListener('submit', submeterEditarUsuario);
    }
}

async function submeterLogin(evento) {
    evento.preventDefault();
    const btnSubmit = document.getElementById('btn-login-submit');
    const btnSpinner = btnSubmit ? btnSubmit.querySelector('.btn-spinner') : null;
    const btnText = btnSubmit ? btnSubmit.querySelector('.btn-text') : null;
    const alertaEl = document.getElementById('login-alerta');
    const usernameInput = document.getElementById('login-username');
    const senhaInput = document.getElementById('login-senha');

    const username = usernameInput ? usernameInput.value.trim() : '';
    const senha = senhaInput ? senhaInput.value : '';

    if (!username || !senha) {
        exibirAlerta(alertaEl, 'Informe o nome de usuário e a senha.', 'erro');
        return;
    }

    if (btnSubmit) btnSubmit.disabled = true;
    if (btnSpinner) btnSpinner.classList.remove('hidden');
    if (btnText) btnText.textContent = 'Verificando credenciais...';
    if (alertaEl) alertaEl.classList.add('hidden');

    try {
        const resposta = await fetch('/api/auth/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify({ username, senha })
        });

        const dados = await resposta.json();

        if (resposta.status === 401 || resposta.status === 400) {
            throw new Error(dados.erro || 'Usuário ou senha inválidos.');
        }

        if (!resposta.ok) {
            throw new Error(dados.erro || `Erro de autenticação (${resposta.status})`);
        }

        // Se senha expirada (>= 15 dias de ciclo de segurança Superfrio)
        if (dados.status === 'SENHA_EXPIRADA') {
            transicionarParaSenhaExpirada(dados.usuario ? dados.usuario.username : username);
            return;
        }

        // Sucesso na autenticação
        if (dados.token) {
            iniciarSessao(dados.usuario, dados.token, true);

            // Verifica regra do aviso diário de expiração (últimos 5 dias, entre 10 e 14 dias)
            if (dados.avisoExpiracao) {
                verificarAvisoDiarioExpiracao(dados.usuario ? dados.usuario.username : username, dados.diasRestantes);
            }
        } else {
            throw new Error('Token de autenticação não retornado pelo servidor.');
        }

    } catch (err) {
        console.error('Falha no login:', err);
        exibirAlerta(alertaEl, err.message, 'erro');
    } finally {
        if (btnSubmit) btnSubmit.disabled = false;
        if (btnSpinner) btnSpinner.classList.add('hidden');
        if (btnText) btnText.textContent = 'Acessar Sistema';
    }
}

function transicionarParaSenhaExpirada(username) {
    const viewLogin = document.getElementById('auth-view-login');
    const viewExpirada = document.getElementById('auth-view-expirada');
    const userInput = document.getElementById('expirada-username');
    const senhaAtualInput = document.getElementById('expirada-senha-atual');
    const novaSenhaInput = document.getElementById('expirada-nova-senha');
    const confirmaInput = document.getElementById('expirada-confirma-senha');
    const alertaEl = document.getElementById('expirada-alerta');

    if (viewLogin) viewLogin.classList.add('hidden');
    if (viewExpirada) viewExpirada.classList.remove('hidden');
    if (userInput) userInput.value = username;
    if (senhaAtualInput) senhaAtualInput.value = '';
    if (novaSenhaInput) novaSenhaInput.value = '';
    if (confirmaInput) confirmaInput.value = '';
    if (alertaEl) alertaEl.classList.add('hidden');

    setTimeout(() => {
        if (senhaAtualInput) senhaAtualInput.focus();
    }, 100);
}

async function submeterTrocaSenhaExpirada(evento) {
    evento.preventDefault();
    const username = document.getElementById('expirada-username').value.trim();
    const senhaAtual = document.getElementById('expirada-senha-atual').value;
    const novaSenha = document.getElementById('expirada-nova-senha').value;
    const confirmaSenha = document.getElementById('expirada-confirma-senha').value;
    const alertaEl = document.getElementById('expirada-alerta');
    const btnSubmit = document.getElementById('btn-expirada-submit');
    const btnSpinner = btnSubmit ? btnSubmit.querySelector('.btn-spinner') : null;
    const btnText = btnSubmit ? btnSubmit.querySelector('.btn-text') : null;

    if (!senhaAtual || !novaSenha || !confirmaSenha) {
        exibirAlerta(alertaEl, 'Preencha todos os campos.', 'erro');
        return;
    }

    if (novaSenha === senhaAtual) {
        exibirAlerta(alertaEl, 'A nova senha deve diferir da atual em pelo menos um caractere, inclusive maiúsculas ou minúsculas.', 'erro');
        return;
    }

    if (novaSenha !== confirmaSenha) {
        exibirAlerta(alertaEl, 'A confirmação de senha não coincide com a nova senha digitada.', 'erro');
        return;
    }

    if (novaSenha.length < 4) {
        exibirAlerta(alertaEl, 'A nova senha deve conter pelo menos 4 caracteres.', 'erro');
        return;
    }

    if (btnSubmit) btnSubmit.disabled = true;
    if (btnSpinner) btnSpinner.classList.remove('hidden');
    if (btnText) btnText.textContent = 'Atualizando ciclo de 15 dias...';
    if (alertaEl) alertaEl.classList.add('hidden');

    try {
        const resposta = await fetch('/api/auth/alterar-senha', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify({
                username,
                senhaAtual,
                novaSenha
            })
        });

        const dados = await resposta.json();

        if (!resposta.ok) {
            throw new Error(dados.erro || `Falha ao alterar senha (${resposta.status})`);
        }

        mostrarToast('Senha corporativa renovada com sucesso! Acesso concedido.', 'sucesso');
        iniciarSessao(dados.usuario, dados.token, false);

    } catch (err) {
        console.error('Erro na alteração de senha expirada:', err);
        exibirAlerta(alertaEl, err.message, 'erro');
    } finally {
        if (btnSubmit) btnSubmit.disabled = false;
        if (btnSpinner) btnSpinner.classList.add('hidden');
        if (btnText) btnText.textContent = 'Atualizar Senha e Entrar';
    }
}

function verificarAvisoDiarioExpiracao(username, diasRestantes) {
    const hoje = new Date().toISOString().split('T')[0];
    const chaveStorage = `aviso_senha_${username}_${hoje}`;

    if (localStorage.getItem(chaveStorage)) {
        return;
    }

    const modalAviso = document.getElementById('modal-aviso-expiracao');
    const diasEl = document.getElementById('aviso-dias-contagem');
    if (diasEl) {
        diasEl.textContent = `${diasRestantes} dia(s)`;
    }

    if (modalAviso) {
        modalAviso.classList.remove('hidden');
    }

    localStorage.setItem(chaveStorage, 'true');
}

async function fazerLogout() {
    const token = obterToken();
    if (token) {
        try {
            await fetch('/api/auth/logout', {
                method: 'POST',
                headers: { 'Authorization': `Bearer ${token}` }
            });
        } catch (e) {
            console.warn('Erro ao chamar logout na API:', e);
        }
    }

    encerrarSessaoLocal();
    exibirTelaBloqueio();
    mostrarToast('Sessão encerrada com segurança.', 'info');
}

// ==========================================================================
// Gestão da Equipe Superfrio (Máx. 10 Usuários)
// ==========================================================================
let listaUsuariosEquipe = [];

async function atualizarContadorEquipe() {
    try {
        const res = await authFetch('/api/auth/usuarios');
        if (!res.ok) return;
        const usuarios = await res.json();
        const count = Array.isArray(usuarios) ? usuarios.length : 0;
        const countEl = document.getElementById('equipe-count');
        if (countEl) {
            countEl.textContent = `${count}/10`;
        }
    } catch (e) {
        console.warn('Não foi possível obter contador de equipe:', e);
    }
}

async function abrirModalEquipe() {
    const modal = document.getElementById('modal-equipe');
    if (modal) modal.classList.remove('hidden');
    await carregarListaEquipe();
}

async function carregarListaEquipe() {
    const corpo = document.getElementById('tabela-usuarios-corpo');
    const quotaText = document.getElementById('equipe-quota-text');
    const quotaBar = document.getElementById('equipe-quota-bar');
    const boxNovo = document.getElementById('equipe-box-novo-usuario');
    const alertaLimite = document.getElementById('equipe-alerta-limite');
    const equipeCount = document.getElementById('equipe-count');

    // Estado inicial de carregamento visual na tabela
    if (corpo && (!corpo.children || corpo.children.length === 0)) {
        corpo.innerHTML = `
            <tr>
                <td colspan="6" style="text-align: center; padding: 22px; color: var(--text-muted);">
                    <div style="display: inline-flex; align-items: center; justify-content: center; gap: 8px;">
                        <span class="btn-spinner" style="width: 14px; height: 14px; border-width: 2px;"></span>
                        <span>Carregando usuários da equipe Superfrio...</span>
                    </div>
                </td>
            </tr>
        `;
    }

    try {
        const res = await authFetch('/api/auth/usuarios');
        if (!res.ok) {
            const erroCorpo = await res.text();
            throw new Error(erroCorpo || `Erro ${res.status}: ${res.statusText}`);
        }
        const usuarios = await res.json();
        listaUsuariosEquipe = Array.isArray(usuarios) ? usuarios : [];
        const total = listaUsuariosEquipe.length;

        if (quotaText) quotaText.textContent = `${total} / 10 usuários cadastrados`;
        if (equipeCount) equipeCount.textContent = `${total}/10`;

        if (quotaBar) {
            const perc = (total / 10) * 100;
            quotaBar.style.width = `${perc}%`;
            if (total >= 10) {
                quotaBar.className = 'quota-bar-fill warning';
            } else {
                quotaBar.className = 'quota-bar-fill';
            }
        }

        if (total >= 10) {
            if (boxNovo) boxNovo.classList.add('hidden');
            if (alertaLimite) alertaLimite.classList.remove('hidden');
        } else {
            if (boxNovo) boxNovo.classList.remove('hidden');
            if (alertaLimite) alertaLimite.classList.add('hidden');
        }

        if (!corpo) return;

        if (total === 0) {
            corpo.innerHTML = `
                <tr>
                    <td colspan="6" style="text-align: center; color: var(--text-muted); padding: 24px;">
                        Nenhum usuário corporativo cadastrado além do administrador padrão.
                    </td>
                </tr>
            `;
            return;
        }

        corpo.innerHTML = listaUsuariosEquipe.map(u => {
            const ehProprio = usuarioLogado && (usuarioLogado.id === u.id || usuarioLogado.username === u.username);
            const diasTroca = u.diasDesdeTroca !== null && u.diasDesdeTroca !== undefined ? u.diasDesdeTroca : 0;
            const diasFaltam = u.diasRestantes !== null && u.diasRestantes !== undefined ? u.diasRestantes : Math.max(0, 15 - diasTroca);

            let cicloHtml = '';
            if (diasFaltam === 0) {
                cicloHtml = `<span style="color: #f87171; font-weight: 700;">Expirada (0 dias)</span>`;
            } else if (diasFaltam <= 5) {
                cicloHtml = `<span style="color: #fbbf24; font-weight: 700;">Falta${diasFaltam === 1 ? '' : 'm'} ${diasFaltam} dia${diasFaltam === 1 ? '' : 's'}</span>`;
            } else {
                cicloHtml = `<span style="color: #34d399; font-weight: 600;">Faltam ${diasFaltam} dias</span>`;
            }

            const dataFormatada = formatarData(u.dataUltimaTrocaSenha);
            const perfilBadge = u.admin 
                ? `<span class="badge-admin-tag">ADMIN</span>`
                : `<span class="badge-operador-tag">OPERADOR</span>`;

            const btnEditar = `
                <button type="button" class="btn-action-edit" onclick="abrirModalEditarUsuario('${u.id}')" title="Editar dados e perfil do usuário">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width: 12px; height: 12px;">
                        <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                        <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                    </svg>
                    Editar
                </button>
            `;

            let btnExcluir = '';
            if (ehProprio) {
                btnExcluir = `<span style="font-size: 11px; color: var(--text-muted); font-style: italic; padding: 4px 6px;">(Você)</span>`;
            } else {
                btnExcluir = `
                    <button type="button" class="btn-action-delete" onclick="excluirUsuarioEquipe('${u.id}', '${escapeHtml(u.nome || u.username)}')" title="Remover usuário da equipe Superfrio">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width: 12px; height: 12px;">
                            <polyline points="3 6 5 6 21 6"></polyline>
                            <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                        </svg>
                        Excluir
                    </button>
                `;
            }

            return `
                <tr>
                    <td><strong>${escapeHtml(u.nome || '--')}</strong></td>
                    <td><code>${escapeHtml(u.username)}</code></td>
                    <td>${perfilBadge}</td>
                    <td style="font-size: 12px; color: var(--text-secondary);">${dataFormatada}</td>
                    <td style="font-size: 12px;">${cicloHtml} <span style="font-size: 10.5px; color: var(--text-muted); font-weight: 400;">(${diasTroca}/15d)</span></td>
                    <td style="text-align: center;">
                        <div class="table-actions-group">
                            ${btnEditar}
                            ${btnExcluir}
                        </div>
                    </td>
                </tr>
            `;
        }).join('');

    } catch (err) {
        console.error('Erro ao carregar equipe:', err);
        mostrarToast('Erro ao listar equipe: ' + err.message, 'erro');
        if (corpo) {
            corpo.innerHTML = `
                <tr>
                    <td colspan="6" style="text-align: center; padding: 22px; color: #f87171;">
                        <div style="display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px;">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width: 28px; height: 28px; color: #f87171;">
                                <circle cx="12" cy="12" r="10"></circle>
                                <line x1="12" y1="8" x2="12" y2="12"></line>
                                <line x1="12" y1="16" x2="12.01" y2="16"></line>
                            </svg>
                            <strong>Não foi possível carregar a lista de usuários da equipe Superfrio.</strong>
                            <span style="font-size: 12px; color: var(--text-secondary); max-width: 480px;">${escapeHtml(err.message)}</span>
                        </div>
                    </td>
                </tr>
            `;
        }
    }
}

async function excluirUsuarioEquipe(id, nome) {
    if (!confirm(`Deseja realmente remover o usuário "${nome}" da equipe Superfrio?`)) {
        return;
    }

    try {
        const res = await authFetch(`/api/auth/usuarios/${encodeURIComponent(id)}`, {
            method: 'DELETE'
        });

        const dados = await res.json();
        if (!res.ok) {
            throw new Error(dados.erro || 'Falha ao remover usuário.');
        }

        mostrarToast(dados.mensagem || 'Usuário removido com sucesso!', 'sucesso');
        await carregarListaEquipe();
        await atualizarContadorEquipe();
    } catch (err) {
        mostrarToast(err.message, 'erro');
    }
}

function abrirModalEditarUsuario(id) {
    const usuario = listaUsuariosEquipe.find(u => u.id === id);
    if (!usuario) {
        mostrarToast('Usuário não localizado na equipe.', 'erro');
        return;
    }

    const modal = document.getElementById('modal-editar-usuario');
    const alerta = document.getElementById('editar-usuario-alerta');
    const idInput = document.getElementById('editar-user-id');
    const nomeInput = document.getElementById('editar-nome');
    const usernameInput = document.getElementById('editar-username');
    const adminInput = document.getElementById('editar-admin');
    const senhaInput = document.getElementById('editar-nova-senha');

    if (alerta) alerta.classList.add('hidden');
    if (idInput) idInput.value = usuario.id;
    if (nomeInput) nomeInput.value = usuario.nome || '';
    if (usernameInput) usernameInput.value = usuario.username || '';
    if (adminInput) adminInput.checked = !!usuario.admin;
    if (senhaInput) senhaInput.value = '';

    if (modal) modal.classList.remove('hidden');
    setTimeout(() => { if (nomeInput) nomeInput.focus(); }, 100);
}

async function submeterEditarUsuario(evento) {
    evento.preventDefault();
    const id = document.getElementById('editar-user-id').value;
    const nome = document.getElementById('editar-nome').value.trim();
    const username = document.getElementById('editar-username').value.trim();
    const admin = document.getElementById('editar-admin').checked;
    const novaSenha = document.getElementById('editar-nova-senha').value.trim();
    const alertaEl = document.getElementById('editar-usuario-alerta');
    const btnSubmit = document.getElementById('btn-editar-usuario-submit');

    if (!nome || !username) {
        exibirAlerta(alertaEl, 'Nome completo e login são obrigatórios.', 'erro');
        return;
    }

    if (novaSenha && novaSenha.length < 4) {
        exibirAlerta(alertaEl, 'A nova senha deve possuir pelo menos 4 caracteres.', 'erro');
        return;
    }

    if (btnSubmit) btnSubmit.disabled = true;
    if (alertaEl) alertaEl.classList.add('hidden');

    try {
        const payload = {
            nome,
            username,
            admin,
            novaSenha: novaSenha || null
        };

        const res = await authFetch(`/api/auth/usuarios/${encodeURIComponent(id)}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify(payload)
        });

        const dados = await res.json();
        if (!res.ok) {
            throw new Error(dados.erro || 'Falha ao atualizar dados do usuário.');
        }

        // Se o usuário editado foi o próprio usuário conectado, atualiza os dados na UI
        if (usuarioLogado && usuarioLogado.id === id) {
            usuarioLogado.nome = dados.nome;
            usuarioLogado.username = dados.username;
            usuarioLogado.admin = dados.admin;
            localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(usuarioLogado));
            atualizarUIUsuario(usuarioLogado);
        }

        mostrarToast(`Usuário "${dados.username}" atualizado com sucesso!`, 'sucesso');

        const modal = document.getElementById('modal-editar-usuario');
        if (modal) modal.classList.add('hidden');

        await carregarListaEquipe();
        await atualizarContadorEquipe();

    } catch (err) {
        console.error('Erro ao editar usuário:', err);
        exibirAlerta(alertaEl, err.message, 'erro');
    } finally {
        if (btnSubmit) btnSubmit.disabled = false;
    }
}

async function cadastrarNovoUsuario(evento) {
    evento.preventDefault();
    const nomeInput = document.getElementById('novo-nome');
    const usernameInput = document.getElementById('novo-username');
    const senhaInput = document.getElementById('novo-senha');
    const adminInput = document.getElementById('novo-admin');
    const alertaEl = document.getElementById('novo-usuario-alerta');
    const btnSubmit = document.getElementById('btn-novo-usuario-submit');

    const nome = nomeInput ? nomeInput.value.trim() : '';
    const username = usernameInput ? usernameInput.value.trim() : '';
    const senha = senhaInput ? senhaInput.value.trim() : '';
    const admin = adminInput ? adminInput.checked : false;

    if (!nome || !username || !senha) {
        exibirAlerta(alertaEl, 'Preencha todos os campos.', 'erro');
        return;
    }

    if (btnSubmit) btnSubmit.disabled = true;
    if (alertaEl) alertaEl.classList.add('hidden');

    try {
        const res = await authFetch('/api/auth/usuarios', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify({ nome, username, senha, admin })
        });

        const dados = await res.json();
        if (!res.ok) {
            throw new Error(dados.erro || 'Falha ao cadastrar usuário.');
        }

        mostrarToast(`Usuário "${dados.username}" cadastrado com sucesso na Superfrio!`, 'sucesso');
        if (nomeInput) nomeInput.value = '';
        if (usernameInput) usernameInput.value = '';
        if (senhaInput) senhaInput.value = '';
        if (adminInput) adminInput.checked = false;

        await carregarListaEquipe();
        await atualizarContadorEquipe();
    } catch (err) {
        exibirAlerta(alertaEl, err.message, 'erro');
    } finally {
        if (btnSubmit) btnSubmit.disabled = false;
    }
}

// ==========================================================================
// Alteração Voluntária de Senha
// ==========================================================================
function abrirModalAlterarSenha() {
    const modal = document.getElementById('modal-alterar-senha');
    const alerta = document.getElementById('modal-senha-alerta');
    const sAtual = document.getElementById('modal-senha-atual');
    const sNova = document.getElementById('modal-nova-senha');
    const sConf = document.getElementById('modal-confirma-senha');

    if (alerta) alerta.classList.add('hidden');
    if (sAtual) sAtual.value = '';
    if (sNova) sNova.value = '';
    if (sConf) sConf.value = '';

    if (modal) modal.classList.remove('hidden');
    setTimeout(() => { if (sAtual) sAtual.focus(); }, 100);
}

async function submeterModalAlterarSenha(evento) {
    evento.preventDefault();
    if (!usuarioLogado) {
        mostrarToast('Você não está conectado.', 'erro');
        return;
    }

    const sAtual = document.getElementById('modal-senha-atual').value;
    const sNova = document.getElementById('modal-nova-senha').value;
    const sConf = document.getElementById('modal-confirma-senha').value;
    const alerta = document.getElementById('modal-senha-alerta');
    const btnSubmit = document.getElementById('btn-modal-senha-submit');

    if (!sAtual || !sNova || !sConf) {
        exibirAlerta(alerta, 'Preencha todos os campos.', 'erro');
        return;
    }

    if (sNova === sAtual) {
        exibirAlerta(alerta, 'A nova senha deve diferir da atual em pelo menos um caractere, inclusive maiúsculas ou minúsculas.', 'erro');
        return;
    }

    if (sNova !== sConf) {
        exibirAlerta(alerta, 'A confirmação de senha não coincide com a nova senha digitada.', 'erro');
        return;
    }

    if (sNova.length < 4) {
        exibirAlerta(alerta, 'A nova senha deve possuir no mínimo 4 caracteres.', 'erro');
        return;
    }

    if (btnSubmit) btnSubmit.disabled = true;

    try {
        const res = await fetch('/api/auth/alterar-senha', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify({
                username: usuarioLogado.username,
                senhaAtual: sAtual,
                novaSenha: sNova
            })
        });

        const dados = await res.json();
        if (!res.ok) {
            throw new Error(dados.erro || 'Falha ao alterar senha.');
        }

        authToken = dados.token;
        localStorage.setItem(AUTH_STORAGE_KEY, dados.token);
        mostrarToast('Senha atualizada com sucesso! Ciclo de 15 dias renovado.', 'sucesso');

        const modal = document.getElementById('modal-alterar-senha');
        if (modal) modal.classList.add('hidden');

    } catch (err) {
        exibirAlerta(alerta, err.message, 'erro');
    } finally {
        if (btnSubmit) btnSubmit.disabled = false;
    }
}

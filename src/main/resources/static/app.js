/**
 * Gestor de Ocorrências Logísticas - Frontend Controller (app.js)
 * Conexão com API REST (/api/ocorrencias), Formatação, KPIs e Micro-interações.
 */

// Estado global da aplicação
let todasOcorrencias = [];

// Mapeamentos amigáveis de enums para exibição
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
    configurarEventosFormulario();
    configurarFiltros();
    carregarOcorrencias();
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
        // Converte para maiúsculas e remove tudo que não for letra ou número
        let val = e.target.value.toUpperCase().replace(/[^A-Z0-9]/g, '');

        // Limita a 7 caracteres alfanuméricos (padrão Mercosul e Tradicional)
        if (val.length > 7) {
            val = val.substring(0, 7);
        }

        // Insere hífen automaticamente após os 3 primeiros caracteres
        if (val.length > 3) {
            val = val.substring(0, 3) + '-' + val.substring(3);
        }

        e.target.value = val;
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

        const resposta = await fetch('/api/ocorrencias', {
            method: 'GET',
            headers: {
                'Accept': 'application/json'
            }
        });

        if (!resposta.ok) {
            throw new Error(`Erro na API (${resposta.status} - ${resposta.statusText})`);
        }

        const dados = await resposta.json();

        // Ordena das ocorrências mais recentes para as mais antigas
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

    // Extrai valores exatamente com os nomes esperados pela entidade Ocorrencia.java
    const placaVeiculo = document.getElementById('placaVeiculo').value.trim();
    const motorista = document.getElementById('motorista').value.trim();
    const monitorResponsavel = document.getElementById('monitorResponsavel').value.trim();
    const poloRegional = document.getElementById('poloRegional').value;
    const tipoOcorrencia = document.getElementById('tipoOcorrencia').value;
    const gravidade = document.getElementById('gravidade').value;
    const status = document.getElementById('status').value;
    const descricaoFatos = document.getElementById('descricaoFatos').value.trim();

    // Validação básica amigável
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

    // Feedback de carregamento no botão
    btnSalvar.disabled = true;
    if (btnSpinner) btnSpinner.classList.remove('hidden');

    try {
        const resposta = await fetch('/api/ocorrencias', {
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

        // Notificação de Sucesso
        const codigoFormatado = ocorrenciaCriada.codigoOco || 'Registrada';
        mostrarToast(`Ocorrência #${codigoFormatado} registrada com sucesso!`, 'sucesso');

        // Limpa formulário e restaura padrões
        limparFormulario();

        // Atualiza tabela automaticamente sem recarregar a página
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

    // Restaura padrões amigáveis
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
// Renderização da Tabela e Badges
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
        // Filtro de Status
        if (filtroStatus !== 'TODOS') {
            const st = normalizarStatus(item.status);
            if (st !== filtroStatus) return false;
        }

        // Filtro textual
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
        const codigo = escapeHtml(item.codigoOco || 'S/N');
        const dataFormatada = formatarDataHora(item.dataHora);
        const placaFormatada = item.placaVeiculo ? escapeHtml(item.placaVeiculo) : '<span class="text-muted">—</span>';
        const motorista = item.motorista ? escapeHtml(item.motorista) : '<span class="text-muted">—</span>';
        const polo = POLOS_LABELS[item.poloRegional] || item.poloRegional || '<span class="text-muted">—</span>';
        const tipo = TIPOS_LABELS[item.tipoOcorrencia] || item.tipoOcorrencia || '<span class="text-muted">—</span>';
        const monitor = item.monitorResponsavel ? escapeHtml(item.monitorResponsavel) : '<span class="text-muted">—</span>';
        const desc = item.descricaoFatos ? escapeHtml(item.descricaoFatos) : '<span class="text-muted">—</span>';

        return `
            <tr>
                <td class="col-codigo">${codigo}</td>
                <td class="col-data">${dataFormatada}</td>
                <td><span class="col-placa">${placaFormatada}</span></td>
                <td><strong>${motorista}</strong></td>
                <td>${polo}</td>
                <td>${tipo}</td>
                <td>${renderizarBadgeGravidade(item.gravidade)}</td>
                <td>${renderizarBadgeStatus(item.status)}</td>
                <td>${monitor}</td>
                <td class="col-desc" title="${escapeHtml(item.descricaoFatos || '')}">${desc}</td>
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

// Renderiza Badge de Status
function renderizarBadgeStatus(status) {
    const s = normalizarStatus(status);
    let classe = 'badge-status-aberto';
    let rotulo = 'Aberto';

    switch (s) {
        case 'ABERTO':
            classe = 'badge-status-aberto';
            rotulo = 'Aberto';
            break;
        case 'EM_ANDAMENTO':
            classe = 'badge-status-andamento';
            rotulo = 'Em Andamento';
            break;
        case 'CONCLUIDO':
            classe = 'badge-status-concluido';
            rotulo = 'Concluído';
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
// Formatação de Data / Hora
// ==========================================================================
function formatarDataHora(valorData) {
    if (!valorData) return '—';

    try {
        const d = new Date(valorData);
        if (isNaN(d.getTime())) return String(valorData);

        const dia = String(d.getDate()).padStart(2, '0');
        const mes = String(d.getMonth() + 1).padStart(2, '0');
        const ano = d.getFullYear();
        const hora = String(d.getHours()).padStart(2, '0');
        const minuto = String(d.getMinutes()).padStart(2, '0');

        return `${dia}/${mes}/${ano} ${hora}:${minuto}`;
    } catch {
        return String(valorData);
    }
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

    // Auto-remover após 4 segundos
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

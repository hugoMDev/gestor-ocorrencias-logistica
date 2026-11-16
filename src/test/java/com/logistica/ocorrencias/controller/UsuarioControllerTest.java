package com.logistica.ocorrencias.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistica.ocorrencias.dto.EditarUsuarioRequest;
import com.logistica.ocorrencias.dto.NovoUsuarioRequest;
import com.logistica.ocorrencias.model.Usuario;
import com.logistica.ocorrencias.repository.UsuarioRepository;
import com.logistica.ocorrencias.service.AuthTokenService;
import com.logistica.ocorrencias.service.UsuarioSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UsuarioController.class)
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private AuthTokenService authTokenService;

    private UUID adminLogadoId;

    @BeforeEach
    void setUp() {
        adminLogadoId = UUID.randomUUID();
        UsuarioSession session = UsuarioSession.builder()
                .token("token-valido")
                .userId(adminLogadoId)
                .username("admin.superfrio")
                .nome("Admin")
                .admin(true)
                .build();

        when(authTokenService.validarToken("token-valido")).thenReturn(Optional.of(session));
    }

    @Test
    @DisplayName("Deve retornar 401 ao tentar listar usuários sem header de autenticação")
    void deveRetornar401SemToken() throws Exception {
        mockMvc.perform(get("/api/auth/usuarios"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deve listar usuários cadastrados quando autenticado")
    void deveListarUsuarios() throws Exception {
        Usuario u1 = Usuario.builder()
                .id(UUID.randomUUID())
                .nome("Admin")
                .username("admin.superfrio")
                .admin(true)
                .ativo(true)
                .dataUltimaTrocaSenha(LocalDateTime.now())
                .build();

        when(usuarioRepository.findAll()).thenReturn(List.of(u1));

        mockMvc.perform(get("/api/auth/usuarios")
                        .header("Authorization", "Bearer token-valido"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("admin.superfrio"));
    }

    @Test
    @DisplayName("Deve rejeitar criação de usuário quando limite de 10 for atingido")
    void deveRejeitarCriacaoQuandoLimiteDe10Atingido() throws Exception {
        when(usuarioRepository.count()).thenReturn(10L);

        NovoUsuarioRequest req = new NovoUsuarioRequest("Novo", "novo.superfrio", "123456", false);

        mockMvc.perform(post("/api/auth/usuarios")
                        .header("Authorization", "Bearer token-valido")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Limite máximo de 10 usuários corporativos da Superfrio atingido."));
    }

    @Test
    @DisplayName("Deve permitir criar usuário se total for menor que 10")
    void devePermitirCriacaoAbaixoDe10() throws Exception {
        when(usuarioRepository.count()).thenReturn(5L);
        when(usuarioRepository.existsByUsernameIgnoreCase("novo.superfrio")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hash");

        Usuario salvo = Usuario.builder()
                .id(UUID.randomUUID())
                .nome("Novo Operador")
                .username("novo.superfrio")
                .admin(false)
                .ativo(true)
                .dataUltimaTrocaSenha(LocalDateTime.now())
                .build();

        when(usuarioRepository.save(any(Usuario.class))).thenReturn(salvo);

        NovoUsuarioRequest req = new NovoUsuarioRequest("Novo Operador", "novo.superfrio", "123456", false);

        mockMvc.perform(post("/api/auth/usuarios")
                        .header("Authorization", "Bearer token-valido")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("novo.superfrio"));
    }

    @Test
    @DisplayName("Deve impedir excluir o próprio usuário logado")
    void deveImpedirExcluirProprioUsuario() throws Exception {
        mockMvc.perform(delete("/api/auth/usuarios/" + adminLogadoId)
                        .header("Authorization", "Bearer token-valido"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Não é permitido excluir o próprio usuário conectado."));
    }

    @Test
    @DisplayName("Deve impedir excluir o único admin ativo do sistema")
    void deveImpedirExcluirUnicoAdmin() throws Exception {
        UUID outroAdminId = UUID.randomUUID();
        Usuario admin = Usuario.builder()
                .id(outroAdminId)
                .nome("Outro Admin")
                .username("outro.superfrio")
                .admin(true)
                .ativo(true)
                .build();

        when(usuarioRepository.findById(outroAdminId)).thenReturn(Optional.of(admin));
        when(usuarioRepository.countByAdminTrueAndAtivoTrue()).thenReturn(1L);

        mockMvc.perform(delete("/api/auth/usuarios/" + outroAdminId)
                        .header("Authorization", "Bearer token-valido"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Não é permitido excluir o único administrador ativo do sistema Superfrio."));
    }

    @Test
    @DisplayName("Deve editar usuário com sucesso via PUT quando autenticado como admin")
    void deveEditarUsuarioComSucesso() throws Exception {
        UUID userId = UUID.randomUUID();
        Usuario existente = Usuario.builder()
                .id(userId)
                .nome("Nome Antigo")
                .username("antigo.superfrio")
                .admin(false)
                .ativo(true)
                .dataUltimaTrocaSenha(LocalDateTime.now().minusDays(5))
                .build();

        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(existente));
        when(usuarioRepository.findByUsernameIgnoreCase("novo.nome.superfrio")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("NovaSenha@2026")).thenReturn("novoHash123");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        EditarUsuarioRequest req = new EditarUsuarioRequest("Nome Atualizado", "novo.nome.superfrio", true, "NovaSenha@2026");

        mockMvc.perform(put("/api/auth/usuarios/" + userId)
                        .header("Authorization", "Bearer token-valido")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Nome Atualizado"))
                .andExpect(jsonPath("$.username").value("novo.nome.superfrio"))
                .andExpect(jsonPath("$.admin").value(true))
                .andExpect(jsonPath("$.diasRestantes").value(15));
    }

    @Test
    @DisplayName("Deve proibir edição se usuário conectado não for admin")
    void deveProibirEdicaoSeNaoAdmin() throws Exception {
        UsuarioSession sessionOperador = UsuarioSession.builder()
                .token("token-operador")
                .userId(UUID.randomUUID())
                .username("operador.superfrio")
                .nome("Operador")
                .admin(false)
                .build();

        when(authTokenService.validarToken("token-operador")).thenReturn(Optional.of(sessionOperador));

        UUID userId = UUID.randomUUID();
        EditarUsuarioRequest req = new EditarUsuarioRequest("Novo", "novo.superfrio", false, null);

        mockMvc.perform(put("/api/auth/usuarios/" + userId)
                        .header("Authorization", "Bearer token-operador")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.erro").value("Acesso restrito. Apenas administradores podem editar membros da equipe."));
    }
}

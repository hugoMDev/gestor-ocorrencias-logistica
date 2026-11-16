package com.logistica.ocorrencias.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistica.ocorrencias.dto.AlterarSenhaRequest;
import com.logistica.ocorrencias.dto.LoginRequest;
import com.logistica.ocorrencias.model.Usuario;
import com.logistica.ocorrencias.repository.UsuarioRepository;
import com.logistica.ocorrencias.service.AuthTokenService;
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
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

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

    private Usuario usuarioPadrao;

    @BeforeEach
    void setUp() {
        usuarioPadrao = Usuario.builder()
                .id(UUID.randomUUID())
                .username("operador.superfrio")
                .nome("Operador Logístico")
                .senhaHash("$2a$10$hashSimulado")
                .ativo(true)
                .admin(false)
                .dataUltimaTrocaSenha(LocalDateTime.now().minusDays(2))
                .build();
    }

    @Test
    @DisplayName("Deve autenticar com sucesso quando usuário e senha conferem")
    void deveAutenticarComSucesso() throws Exception {
        when(usuarioRepository.findByUsernameIgnoreCase("operador.superfrio"))
                .thenReturn(Optional.of(usuarioPadrao));
        when(passwordEncoder.matches("Superfrio@123", "$2a$10$hashSimulado"))
                .thenReturn(true);
        when(authTokenService.criarSessao(any(Usuario.class))).thenReturn("token-teste-123");

        LoginRequest req = new LoginRequest("operador.superfrio", "Superfrio@123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCESSO"))
                .andExpect(jsonPath("$.token").value("token-teste-123"))
                .andExpect(jsonPath("$.avisoExpiracao").value(false));
    }

    @Test
    @DisplayName("Deve retornar SENHA_EXPIRADA se a troca foi há 15 dias ou mais")
    void deveRetornarSenhaExpiradaApos15Dias() throws Exception {
        usuarioPadrao.setDataUltimaTrocaSenha(LocalDateTime.now().minusDays(16));

        when(usuarioRepository.findByUsernameIgnoreCase("operador.superfrio"))
                .thenReturn(Optional.of(usuarioPadrao));
        when(passwordEncoder.matches("Superfrio@123", "$2a$10$hashSimulado"))
                .thenReturn(true);

        LoginRequest req = new LoginRequest("operador.superfrio", "Superfrio@123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENHA_EXPIRADA"))
                .andExpect(jsonPath("$.diasRestantes").value(0));
    }

    @Test
    @DisplayName("Deve alertar avisoExpiracao=true se faltarem menos de 5 dias (entre 10 e 14 dias de uso)")
    void deveRetornarAvisoExpiracaoNosUltimos5Dias() throws Exception {
        usuarioPadrao.setDataUltimaTrocaSenha(LocalDateTime.now().minusDays(12));

        when(usuarioRepository.findByUsernameIgnoreCase("operador.superfrio"))
                .thenReturn(Optional.of(usuarioPadrao));
        when(passwordEncoder.matches("Superfrio@123", "$2a$10$hashSimulado"))
                .thenReturn(true);
        when(authTokenService.criarSessao(any(Usuario.class))).thenReturn("token-aviso");

        LoginRequest req = new LoginRequest("operador.superfrio", "Superfrio@123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCESSO"))
                .andExpect(jsonPath("$.avisoExpiracao").value(true))
                .andExpect(jsonPath("$.diasRestantes").value(3));
    }

    @Test
    @DisplayName("Deve rejeitar alteração de senha se a nova for idêntica à atual")
    void deveRejeitarSenhaIdenticaNaAlteracao() throws Exception {
        when(usuarioRepository.findByUsernameIgnoreCase("operador.superfrio"))
                .thenReturn(Optional.of(usuarioPadrao));
        when(passwordEncoder.matches("Superfrio@123", "$2a$10$hashSimulado"))
                .thenReturn(true);

        AlterarSenhaRequest req = new AlterarSenhaRequest("operador.superfrio", "Superfrio@123", "Superfrio@123");

        mockMvc.perform(post("/api/auth/alterar-senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("A nova senha deve diferir da atual em pelo menos um caractere, inclusive maiúsculas ou minúsculas"));
    }

    @Test
    @DisplayName("Deve aceitar alteração de senha quando difere por 1 caractere (case-sensitive)")
    void deveAceitarSenhaComAlteracaoDeUmCaractere() throws Exception {
        when(usuarioRepository.findByUsernameIgnoreCase("operador.superfrio"))
                .thenReturn(Optional.of(usuarioPadrao));
        when(passwordEncoder.matches("Superfrio@123", "$2a$10$hashSimulado"))
                .thenReturn(true);
        when(passwordEncoder.encode("superfrio@123")).thenReturn("$2a$10$novoHash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(authTokenService.criarSessao(any(Usuario.class))).thenReturn("token-novo");

        AlterarSenhaRequest req = new AlterarSenhaRequest("operador.superfrio", "Superfrio@123", "superfrio@123");

        mockMvc.perform(post("/api/auth/alterar-senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCESSO"))
                .andExpect(jsonPath("$.token").value("token-novo"));
    }
}

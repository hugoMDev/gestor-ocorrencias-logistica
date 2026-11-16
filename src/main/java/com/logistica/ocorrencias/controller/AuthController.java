package com.logistica.ocorrencias.controller;

import com.logistica.ocorrencias.dto.AlterarSenhaRequest;
import com.logistica.ocorrencias.dto.LoginRequest;
import com.logistica.ocorrencias.dto.LoginResponse;
import com.logistica.ocorrencias.dto.UsuarioDTO;
import com.logistica.ocorrencias.model.Usuario;
import com.logistica.ocorrencias.repository.UsuarioRepository;
import com.logistica.ocorrencias.service.AuthTokenService;
import com.logistica.ocorrencias.service.UsuarioSession;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthTokenService authTokenService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (request == null || request.getUsername() == null || request.getSenha() == null ||
                request.getUsername().isBlank() || request.getSenha().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Informe o nome de usuário e a senha."));
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsernameIgnoreCase(request.getUsername().trim());
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("erro", "Usuário ou senha inválidos."));
        }

        Usuario usuario = usuarioOpt.get();
        if (!usuario.isAtivo()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("erro", "Usuário inativo. Contate o administrador Superfrio."));
        }

        if (!passwordEncoder.matches(request.getSenha(), usuario.getSenhaHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("erro", "Usuário ou senha inválidos."));
        }

        LocalDateTime dataTroca = usuario.getDataUltimaTrocaSenha() != null
                ? usuario.getDataUltimaTrocaSenha()
                : LocalDateTime.now().minusDays(30);

        long diasDesdeTroca = ChronoUnit.DAYS.between(dataTroca, LocalDateTime.now());

        // Regra de expiração de 15 dias corridos
        if (diasDesdeTroca >= 15) {
            return ResponseEntity.ok(LoginResponse.builder()
                    .status("SENHA_EXPIRADA")
                    .mensagem("Sua senha corporativa da Superfrio expirou após 15 dias de uso. Atualize-a para continuar.")
                    .usuario(UsuarioDTO.fromEntity(usuario))
                    .avisoExpiracao(true)
                    .diasRestantes(0L)
                    .build());
        }

        // Gera token de sessão
        String token = authTokenService.criarSessao(usuario);
        long diasRestantes = Math.max(0, 15 - diasDesdeTroca);
        boolean avisoExpiracao = (diasDesdeTroca >= 10 && diasDesdeTroca < 15);

        return ResponseEntity.ok(LoginResponse.builder()
                .status("SUCESSO")
                .mensagem("Autenticação realizada com sucesso.")
                .token(token)
                .usuario(UsuarioDTO.fromEntity(usuario))
                .avisoExpiracao(avisoExpiracao)
                .diasRestantes(diasRestantes)
                .build());
    }

    @PostMapping("/alterar-senha")
    public ResponseEntity<?> alterarSenha(@RequestBody AlterarSenhaRequest request) {
        if (request == null || request.getUsername() == null || request.getSenhaAtual() == null || request.getNovaSenha() == null ||
                request.getUsername().isBlank() || request.getSenhaAtual().isBlank() || request.getNovaSenha().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Todos os campos (usuário, senha atual e nova senha) são obrigatórios."));
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsernameIgnoreCase(request.getUsername().trim());
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", "Usuário não localizado."));
        }

        Usuario usuario = usuarioOpt.get();
        if (!usuario.isAtivo()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("erro", "Usuário inativo."));
        }

        // Valida se senha atual confere com a gravada
        if (!passwordEncoder.matches(request.getSenhaAtual(), usuario.getSenhaHash())) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Senha atual incorreta."));
        }

        // Validação da regra case-sensitive de pelo menos 1 caractere de diferença
        if (request.getNovaSenha().equals(request.getSenhaAtual())) {
            return ResponseEntity.badRequest().body(Map.of("erro", "A nova senha deve diferir da atual em pelo menos um caractere, inclusive maiúsculas ou minúsculas"));
        }

        if (request.getNovaSenha().length() < 4) {
            return ResponseEntity.badRequest().body(Map.of("erro", "A nova senha deve possuir no mínimo 4 caracteres."));
        }

        // Atualiza a senha e a data do ciclo de 15 dias
        usuario.setSenhaHash(passwordEncoder.encode(request.getNovaSenha()));
        usuario.setDataUltimaTrocaSenha(LocalDateTime.now());
        Usuario salvo = usuarioRepository.save(usuario);

        // Gera e libera novo token de sessão
        String token = authTokenService.criarSessao(salvo);

        return ResponseEntity.ok(LoginResponse.builder()
                .status("SUCESSO")
                .mensagem("Senha alterada com sucesso! Acesso concedido.")
                .token(token)
                .usuario(UsuarioDTO.fromEntity(salvo))
                .avisoExpiracao(false)
                .diasRestantes(15L)
                .build());
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            authTokenService.removerSessao(token);
        }
        return ResponseEntity.ok(Map.of("mensagem", "Sessão encerrada com sucesso."));
    }

    @GetMapping("/me")
    public ResponseEntity<?> obterUsuarioAtual(HttpServletRequest request) {
        Object sessionObj = request.getAttribute("usuarioLogado");
        if (sessionObj instanceof UsuarioSession session) {
            Optional<Usuario> usuarioOpt = usuarioRepository.findById(session.getUserId());
            if (usuarioOpt.isPresent()) {
                return ResponseEntity.ok(UsuarioDTO.fromEntity(usuarioOpt.get()));
            }
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("erro", "Sessão inválida."));
    }
}

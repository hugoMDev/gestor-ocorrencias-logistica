package com.logistica.ocorrencias.controller;

import com.logistica.ocorrencias.dto.EditarUsuarioRequest;
import com.logistica.ocorrencias.dto.NovoUsuarioRequest;
import com.logistica.ocorrencias.dto.UsuarioDTO;
import com.logistica.ocorrencias.model.Usuario;
import com.logistica.ocorrencias.repository.UsuarioRepository;
import com.logistica.ocorrencias.service.UsuarioSession;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {

    private static final int LIMITE_MAXIMO_USUARIOS = 10;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping
    public ResponseEntity<List<UsuarioDTO>> listarUsuarios() {
        List<UsuarioDTO> usuarios = usuarioRepository.findAll().stream()
                .sorted(Comparator.comparing(Usuario::isAdmin).reversed()
                        .thenComparing(Usuario::getNome, String.CASE_INSENSITIVE_ORDER))
                .map(UsuarioDTO::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(usuarios);
    }

    @PostMapping
    public ResponseEntity<?> criarUsuario(@RequestBody NovoUsuarioRequest request) {
        long totalAtual = usuarioRepository.count();
        if (totalAtual >= LIMITE_MAXIMO_USUARIOS) {
            return ResponseEntity.badRequest().body(Map.of(
                    "erro", "Limite máximo de 10 usuários corporativos da Superfrio atingido."
            ));
        }

        if (request == null || request.getNome() == null || request.getNome().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("erro", "O nome do usuário é obrigatório."));
        }

        if (request.getUsername() == null || request.getUsername().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("erro", "O nome de usuário (login) é obrigatório."));
        }

        String usernameLimpo = request.getUsername().trim().toLowerCase();
        if (usuarioRepository.existsByUsernameIgnoreCase(usernameLimpo)) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Nome de usuário já cadastrado na Superfrio."));
        }

        if (request.getSenha() == null || request.getSenha().trim().length() < 4) {
            return ResponseEntity.badRequest().body(Map.of("erro", "A senha inicial deve conter pelo menos 4 caracteres."));
        }

        Usuario novoUsuario = Usuario.builder()
                .nome(request.getNome().trim())
                .username(usernameLimpo)
                .senhaHash(passwordEncoder.encode(request.getSenha().trim()))
                .admin(request.isAdmin())
                .ativo(true)
                .dataUltimaTrocaSenha(LocalDateTime.now())
                .build();

        Usuario salvo = usuarioRepository.save(novoUsuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioDTO.fromEntity(salvo));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> editarUsuario(
            @PathVariable UUID id,
            @RequestBody EditarUsuarioRequest request,
            HttpServletRequest httpRequest) {

        Object sessionObj = httpRequest.getAttribute("usuarioLogado");
        if (sessionObj instanceof UsuarioSession session) {
            if (!session.isAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "erro", "Acesso restrito. Apenas administradores podem editar membros da equipe."
                ));
            }
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", "Usuário não localizado."));
        }

        Usuario usuario = usuarioOpt.get();

        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            String novoUsername = request.getUsername().trim().toLowerCase();
            if (!novoUsername.equalsIgnoreCase(usuario.getUsername())) {
                Optional<Usuario> existente = usuarioRepository.findByUsernameIgnoreCase(novoUsername);
                if (existente.isPresent() && !existente.get().getId().equals(id)) {
                    return ResponseEntity.badRequest().body(Map.of("erro", "Nome de usuário já cadastrado na Superfrio."));
                }
                usuario.setUsername(novoUsername);
            }
        }

        if (request.getNome() != null && !request.getNome().isBlank()) {
            usuario.setNome(request.getNome().trim());
        }

        if (request.getAdmin() != null) {
            boolean novoAdmin = request.getAdmin();
            if (usuario.isAdmin() && !novoAdmin) {
                long totalAdminsAtivos = usuarioRepository.countByAdminTrueAndAtivoTrue();
                if (totalAdminsAtivos <= 1) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "erro", "Não é permitido revogar privilégios do único administrador ativo da Superfrio."
                    ));
                }
            }
            usuario.setAdmin(novoAdmin);
        }

        if (request.getNovaSenha() != null && !request.getNovaSenha().isBlank()) {
            if (request.getNovaSenha().trim().length() < 4) {
                return ResponseEntity.badRequest().body(Map.of("erro", "A nova senha deve possuir pelo menos 4 caracteres."));
            }
            usuario.setSenhaHash(passwordEncoder.encode(request.getNovaSenha().trim()));
            usuario.setDataUltimaTrocaSenha(LocalDateTime.now());
        }

        Usuario salvo = usuarioRepository.save(usuario);
        return ResponseEntity.ok(UsuarioDTO.fromEntity(salvo));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> excluirUsuario(@PathVariable UUID id, HttpServletRequest request) {
        Object sessionObj = request.getAttribute("usuarioLogado");
        if (sessionObj instanceof UsuarioSession session) {
            if (session.getUserId().equals(id)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "erro", "Não é permitido excluir o próprio usuário conectado."
                ));
            }
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", "Usuário não localizado."));
        }

        Usuario usuario = usuarioOpt.get();
        if (usuario.isAdmin()) {
            long totalAdminsAtivos = usuarioRepository.countByAdminTrueAndAtivoTrue();
            if (totalAdminsAtivos <= 1) {
                return ResponseEntity.badRequest().body(Map.of(
                        "erro", "Não é permitido excluir o único administrador ativo do sistema Superfrio."
                ));
            }
        }

        usuarioRepository.delete(usuario);
        return ResponseEntity.ok(Map.of(
                "mensagem", "Usuário '" + usuario.getUsername() + "' removido com sucesso."
        ));
    }
}

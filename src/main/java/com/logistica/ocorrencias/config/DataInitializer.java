package com.logistica.ocorrencias.config;

import com.logistica.ocorrencias.model.Usuario;
import com.logistica.ocorrencias.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        long count = usuarioRepository.count();
        if (count == 0) {
            log.info("Tabela de usuários vazia. Criando usuário administrador inicial da Superfrio...");

            Usuario admin = Usuario.builder()
                    .username("admin.superfrio")
                    .nome("Administrador Superfrio")
                    .senhaHash(passwordEncoder.encode("Superfrio@2026"))
                    .admin(true)
                    .ativo(true)
                    .dataUltimaTrocaSenha(LocalDateTime.now())
                    .build();

            usuarioRepository.save(admin);
            log.info("Usuário inicial criado com sucesso: admin.superfrio");
        } else {
            log.info("Sistema Superfrio inicializado com {} usuário(s) corporativo(s) cadastrado(s).", count);
        }
    }
}

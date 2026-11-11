package com.logistica.ocorrencias;

import com.formdev.flatlaf.FlatDarkLaf;
import com.logistica.ocorrencias.repository.JsonOcorrenciaRepository;
import com.logistica.ocorrencias.repository.OcorrenciaRepository;
import com.logistica.ocorrencias.service.OcorrenciaService;
import com.logistica.ocorrencias.view.MainFrame;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Dimension;

/**
 * Ponto de entrada da aplicação "Gestor de Ocorrências Logísticas".
 * Inicializa o Look and Feel FlatLaf Dark, configura a injeção de dependências e exibe a interface gráfica.
 */
public class App {

    public static void main(String[] args) {
        // Inicializa o tema FlatLaf Dark moderno e estilizações globais
        configurarAparenciaFlatLaf();

        // Tratamento global de exceções não capturadas no Event Dispatch Thread
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            System.err.println("Exceção não tratada capturada: " + throwable.getMessage());
            throwable.printStackTrace();
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                    null,
                    "Ocorreu um erro inesperado:\n" + throwable.getMessage(),
                    "Erro Operacional",
                    JOptionPane.ERROR_MESSAGE
            ));
        });

        // Inicialização na thread de despacho de eventos do Swing (EDT)
        SwingUtilities.invokeLater(() -> {
            try {
                // Instanciação das camadas desacopladas (Repository -> Service -> View)
                OcorrenciaRepository repository = new JsonOcorrenciaRepository();
                OcorrenciaService service = new OcorrenciaService(repository);

                MainFrame frame = new MainFrame(service);
                frame.setVisible(true);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(
                        null,
                        "Falha crítica na inicialização do sistema: " + ex.getMessage(),
                        "Erro Fatal",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });
    }

    /**
     * Aplica o FlatLaf Dark e configura curvas e dimensões dos componentes modernos.
     */
    private static void configurarAparenciaFlatLaf() {
        try {
            // Inicializa o tema escuro moderno do FlatLaf
            FlatDarkLaf.setup();

            // Customizações estéticas globais (cantos arredondados, espaçamento)
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("ProgressBar.arc", 8);
            UIManager.put("TextComponent.arc", 8);
            UIManager.put("ScrollBar.showButtons", false);
            UIManager.put("ScrollBar.width", 10);
            UIManager.put("Table.showHorizontalLines", true);
            UIManager.put("Table.showVerticalLines", false);
            UIManager.put("Table.intercellSpacing", new Dimension(0, 1));
            UIManager.put("TableHeader.separatorColor", new java.awt.Color(55, 65, 81));
        } catch (Exception e) {
            System.err.println("Não foi possível carregar o FlatLaf. Usando tema padrão do sistema. " + e.getMessage());
        }
    }
}

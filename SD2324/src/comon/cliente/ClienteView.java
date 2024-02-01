package comon.cliente;

import comon.Mensagem;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


public class ClienteView {
    private ClienteController controller;

    public ClienteView(ClienteController controller) {
        this.controller = controller;
    }

    public void run(DataOutputStream out, BufferedReader systemIn) throws IOException {
        sendMessage(systemIn, out); // Enviar pedidos
    }

    private void sendMessage(BufferedReader systemIn, DataOutputStream out) {
        new Thread(() -> {
            int opcao = 0;
            do {
                System.out.println("=== MENU INTERATIVO ===");
                System.out.println("1 - Autenticação");
                System.out.println("2 - Enviar pedidos");
                System.out.println("3 - Resposta aos pedidos");
                System.out.println("4 - Tarefas pendentes");
                System.out.println("5 - Ocupação do serviço (memória disponível)");
                System.out.println("6 - Tarefas pendentes do serviço");
                System.out.println("7 - Sair");
                System.out.print("Opção: ");
                try {
                    opcao = Integer.parseInt(systemIn.readLine());
                    switch (opcao) {
                        case 1 -> solicitarAutenticacaoView(systemIn, out);
                        case 2 -> enviaTarefaView(systemIn,out);
                        case 3 -> respostaPedidoView(systemIn, out);
                        case 4 -> tarefasPendentesClienteView(systemIn, out);
                        case 5 -> ocupacaoServicoView(systemIn, out);
                        case 6 -> tarefasPendentesServicoView(systemIn, out);
                        case 7 -> enviarShutDownView(systemIn, out);
                        default -> System.out.println("Opção inválida.\n");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Erro a escolher o número");
                } catch (IOException e) {
                    System.out.println("IOException erro");
                }
            } while (opcao != 7);
        }).start();
    }

    private void enviaTarefaView(BufferedReader systemIn, DataOutputStream out){
        try {
            System.out.print("Deseja ler e enviar tarefas de um arquivo?(sim ou nao)");
            String resposta;
            do {
                System.out.print("\nOpção: ");
                resposta = systemIn.readLine().toLowerCase();
            } while(!resposta.equals("sim") && !resposta.equals("nao"));

            if (resposta.equals("sim")) { // Ler de um ficheiro as tarefas
                String lerArquivo;
                String depositarArquivo;
                do {
                    System.out.print("Caminho do arquivo para as tarefas: ");
                    lerArquivo = systemIn.readLine();
                    System.out.print("\nCaminho do arquivo para as respostas: ");
                    depositarArquivo = systemIn.readLine();

                    // Cria uma pasta com o caminho fornecido
                    File arquivo = new File(depositarArquivo);
                    if (!arquivo.exists()) {
                        arquivo.createNewFile();
                    }

                } while (lerArquivo.isEmpty() && depositarArquivo.isEmpty());
                lerTarefasDeArquivoView(lerArquivo, depositarArquivo, out);
            } else {
                enviarPedidoView(systemIn, out);// Enviar através do terminal as tarefas
            }
        } catch (IOException e){
            System.err.println("Erro a enviar tarefa");
        }
    }

    public void lerTarefasDeArquivoView(String lerArquivo, String depositarArquivo, DataOutputStream out) throws IOException {
        Path path = Paths.get(lerArquivo);

        if (Files.exists(path)) {
            List<String> linhas = Files.readAllLines(path, StandardCharsets.UTF_8);

            for (String linha : linhas) {
                String[] partes = linha.split(",");
                if (partes.length >= 4) {
                    String nomeUser = partes[0].trim();
                    String senha = partes[1].trim();
                    String codigoTarefa = partes[2].trim();
                    String dadosTarefa = partes[3].trim();
                    byte[] dadosTarefaBytes = (!dadosTarefa.equals("null")) ? dadosTarefa.getBytes() : null;

                    String tipo = partes[4].trim();

                    switch (tipo){
                        case "envioTarefa" -> controller.enviarPedidoController            (nomeUser, senha, codigoTarefa, dadosTarefaBytes, depositarArquivo, out);
                        case "consultaR"   -> controller.respostaPedidoController          (nomeUser, senha, codigoTarefa, dadosTarefaBytes, depositarArquivo, out);
                        case "consultaP"   -> controller.tarefasPendentesClienteController (nomeUser, senha, codigoTarefa, dadosTarefaBytes, depositarArquivo, out);
                        case "consultaO"   -> controller.ocupacaoServicoController         (nomeUser, senha, codigoTarefa, dadosTarefaBytes, depositarArquivo, out);
                        case "consultaT"   -> controller.tarefasPendentesServicoController (nomeUser, senha, codigoTarefa, dadosTarefaBytes, depositarArquivo, out);
                        default -> System.out.println("Tipo de mensagens inválido.");
                    }
                } else {
                    System.out.println("Formato inválido na linha: " + linha);
                }
            }
            System.out.println("\nTarefas lidas e enviadas com sucesso!");
        } else {
            System.out.println("\nO arquivo não existe. Forneca um caminho válido.");
        }
    }

    public void solicitarAutenticacaoView(BufferedReader systemIn, DataOutputStream out) throws IOException {
        System.out.print("Nome do cliente: ");
        String nomeUser = systemIn.readLine();
        System.out.print("Senha: ");
        String senha = systemIn.readLine();

        controller.solicitarAutenticacaoController(nomeUser, senha, "null", null, "null", out);
    }

    public void enviarPedidoView(BufferedReader systemIn, DataOutputStream out) throws IOException {
        System.out.print("Nome de autenticacao: ");
        String nomeUser = systemIn.readLine();
        System.out.print("Senha de autenticacao: ");
        String senha = systemIn.readLine();
        System.out.print("Código da tarefa: ");
        String codigoTarefa = systemIn.readLine();
        System.out.print("Dados da tarefa: ");
        String dadosTarefaInput = systemIn.readLine();
        byte[] dadosTarefa = dadosTarefaInput.getBytes();

        controller.enviarPedidoController(nomeUser, senha, codigoTarefa, dadosTarefa,"null", out);
    }

    public void respostaPedidoView(BufferedReader systemIn, DataOutputStream out) throws IOException {
        System.out.print("Nome de autenticacao: ");
        String nomeUser = systemIn.readLine();
        System.out.print("Senha de autenticacao: ");
        String senha = systemIn.readLine();

        controller.respostaPedidoController(nomeUser, senha, "null", null,"null", out);
    }

    public void tarefasPendentesClienteView(BufferedReader systemIn, DataOutputStream out) throws IOException {
        System.out.print("Nome de autenticacao: ");
        String nomeUser = systemIn.readLine();
        System.out.print("Senha de autenticacao: ");
        String senha = systemIn.readLine();

        controller.tarefasPendentesClienteController(nomeUser, senha, "null", null,"null", out);
    }

    public void ocupacaoServicoView(BufferedReader systemIn, DataOutputStream out) throws IOException {
        System.out.print("Nome de autenticacao: ");
        String nomeUser = systemIn.readLine();
        System.out.print("Senha de autenticacao: ");
        String senha = systemIn.readLine();

        controller.ocupacaoServicoController(nomeUser, senha, "null", null,"null", out);
    }

    public void tarefasPendentesServicoView(BufferedReader systemIn, DataOutputStream out) throws IOException {
        System.out.print("Nome de autenticacao: ");
        String nomeUser = systemIn.readLine();
        System.out.print("Senha de autenticacao: ");
        String senha = systemIn.readLine();

        controller.tarefasPendentesServicoController(nomeUser, senha, "null", null,"null", out);
    }

    public void enviarShutDownView(BufferedReader systemIn, DataOutputStream out) throws IOException {
        System.out.print("Nome de autenticacao: ");
        String nomeUser = systemIn.readLine();
        System.out.print("Senha de autenticacao: ");
        String senha = systemIn.readLine();

        controller.enviarShutDownController(nomeUser, senha, "null", null,"null", out);
    }

    public void deposita(String string, String s1, byte[] s2){
        System.out.println(string + s1 + ": " + s2);
    }
    public void deposita2(String string, List<String> mensagem ){
        System.out.println(string + mensagem);
    }
    public void deposita3(String string, Integer numero ){
        System.out.println(string + numero);
    }
    public void deposita4(String string) {System.out.println(string);}
    public void enviaParaFicheiro(String mensagem) {
        System.out.println(mensagem);
    }
    public void shutdown(){

    }
}
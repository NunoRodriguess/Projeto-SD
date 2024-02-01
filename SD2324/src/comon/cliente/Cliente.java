package comon.cliente;

import comon.Mensagem;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Cliente {
    private final Map<String,List<String>> respostas = new HashMap<>(); // Respostas para depositar no ficheiro

    public static void main(String[] args) {
        Cliente cliente = new Cliente();
        ClienteController controller = new ClienteController(cliente);
        ClienteView view = new ClienteView(controller);
        cliente.runClient(view);
        Socket socket = null;
    }

    public void runClient(ClienteView view) {
        try {
            Socket socket = new Socket("localhost", 12345); // Conectar ao servidor na porta 12345
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());
            BufferedReader systemIn = new BufferedReader(new InputStreamReader(System.in));

            view.run(out,systemIn);
            receiveMessage(in, view, socket);

        } catch (SocketException e) {
            System.err.println("Erro na conexão com o servidor.");
        } catch (IOException e) {
            System.err.println("Erro nas streams.");
        }
    }


    // Enviar tarefa ao servidor
    public void sendJob(String nomeUser, String senha, String codigoTarefa, byte[] dadosTarefa, String depositarArquivo, DataOutputStream out, int number) {
        try {
            Mensagem mensagem = null;
            switch (number) {
                case 0 -> mensagem = new Mensagem(Mensagem.TipoMensagem.AUTENTI_C,
                        codigoTarefa,
                        nomeUser,
                        senha,
                        "null",
                        -1,
                        dadosTarefa,
                        depositarArquivo);

                case 1 -> mensagem = new Mensagem(Mensagem.TipoMensagem.ENVIO_TAREFA,
                        codigoTarefa,
                        nomeUser,
                        senha,
                        "null",
                        -1,
                        dadosTarefa,
                        depositarArquivo);

                case 2 -> mensagem = new Mensagem(Mensagem.TipoMensagem.CONSULTA_R,
                        codigoTarefa,
                        nomeUser,
                        senha,
                        "null",
                        -1,
                        dadosTarefa,
                        depositarArquivo);

                case 3 -> mensagem = new Mensagem(Mensagem.TipoMensagem.CONSULTA_P,
                        codigoTarefa,
                        nomeUser,
                        senha,
                        "null",
                        -1,
                        dadosTarefa,
                        depositarArquivo);

                case 4 -> mensagem = new Mensagem(Mensagem.TipoMensagem.CONSULTA_O,
                        codigoTarefa,
                        nomeUser,
                        senha,
                        "null",
                        -1,
                        dadosTarefa,
                        depositarArquivo);

                case 5 -> mensagem = new Mensagem(Mensagem.TipoMensagem.CONSULTA_T,
                        codigoTarefa,
                        nomeUser,
                        senha,
                        "null",
                        -1,
                        dadosTarefa,
                        depositarArquivo);
                case 6 -> mensagem = new Mensagem(Mensagem.TipoMensagem.SHUT_DOWN,
                        codigoTarefa,
                        nomeUser,
                        senha,
                        "null",
                        -1,
                        dadosTarefa,
                        depositarArquivo);
                default -> System.out.println("Insira o número válido\n");
            }
            mensagem.sendMessage(out);
        } catch (IOException e) {
            System.err.println("Erro a enviar a mensagem.");
        }
    }

    private void receiveMessage(DataInputStream in, ClienteView view, Socket socket) {
        new Thread(() -> {
            while (true) {
                Mensagem resposta;
                try {
                    resposta = Mensagem.deserialize(in);

                } catch (IOException e) {
                    System.err.println("Erro a receber a mensagem");
                    break; // Sair do loop
                }
                // Imprimir resposta no terminal
                if(resposta.getCaminho().equals("null")) depositarRespostaNoTerminalView(resposta, view, socket);

                    // Imprimir resposta no ficheiro
                else depositarRespostaEmArquivoView(resposta.getCaminho(),resposta, view, socket);

            }
        }).start();
    }

    public void depositarRespostaNoTerminalView(Mensagem resposta, ClienteView view, Socket socket){

        switch (resposta.getTipo()) {
            case ENVIO_ERRO -> view.deposita("\nErro da tarefa ", resposta.getCodigoTarefa(), resposta.getDadosTarefa());
            case ENVIO_RESPOSTA -> view.deposita("\nResultado da tarefa ", resposta.getCodigoTarefa(), resposta.getDadosTarefa());
            case CONSULTA_R -> {
                List<String> mensagem = converterBytesParaLista(resposta.getDadosTarefa());
                view.deposita2("\nRespostas obtidas: ", mensagem);
            }
            case CONSULTA_P -> {
                List<String> mensagem = converterBytesParaLista(resposta.getDadosTarefa());
                view.deposita2("\nTarefas pendentes: ", mensagem);
            }
            case CONSULTA_O -> {
                List<String> mensagem = converterBytesParaLista(resposta.getDadosTarefa());
                view.deposita2("\n", mensagem);
            }
            case CONSULTA_T -> view.deposita3("\nNúmero de tarefas pendentes: ", resposta.getAux() );
            case SHUT_DOWN -> {
                view.deposita4("Sistema desligado. Socket encerrada.");
                this.shutdown(socket);
            }
        }
    }

    public void depositarRespostaEmArquivoView(String depositarArquivo, Mensagem resposta, ClienteView view, Socket socket) {
        List<String> fileRespostas = respostas.computeIfAbsent(depositarArquivo, k -> new ArrayList<>());

        switch (resposta.getTipo()) {
            case ENVIO_ERRO -> fileRespostas.add("Erros recebidos " +
                    resposta.getCodigoTarefa() + ": " +
                    resposta.getDadosTarefa());
            case ENVIO_RESPOSTA -> fileRespostas.add("Resultado da tarefa " +
                    resposta.getCodigoTarefa() + ": " +
                    resposta.getDadosTarefa());
            case CONSULTA_R -> {
                List<String> mensagem = converterBytesParaLista(resposta.getDadosTarefa());
                fileRespostas.add("Respostas obtidas: " + mensagem);
            }
            case CONSULTA_P -> {
                List<String> mensagem = converterBytesParaLista(resposta.getDadosTarefa());
                fileRespostas.add("Tarefas pendentes: " + mensagem);
            }
            case CONSULTA_O -> {
                List<String> mensagem = converterBytesParaLista(resposta.getDadosTarefa());
                fileRespostas.add(mensagem.toString());
            }
            case CONSULTA_T -> fileRespostas.add("Número de tarefas pendentes: " + resposta.getAux());
            case SHUT_DOWN -> {
                view.deposita4("Sistema desligado. Socket encerrada.");
                this.shutdown(socket);
            }
        }
        enviaParaFicheiro(depositarArquivo, view); //Envia as respostas para o ficheiro
    }

    public List<String> converterBytesParaLista(byte[] bytes) {
        List<String> listaStrings = new ArrayList<>();
        try {
            StringBuilder strBuilder = new StringBuilder();
            if (bytes != null) {
                for (byte b : bytes) {
                    // Verifica se encontrou o byte de separação entre as strings
                    if (b == (byte) 0) {
                        String strValue = strBuilder.toString();
                        if (!strValue.isEmpty()) {
                            listaStrings.add(strValue);
                        }
                        strBuilder = new StringBuilder();
                    } else {
                        strBuilder.append((char) b);
                    }
                }
            }
            // Adiciona a última string à lista, se não for uma string vazia
            String lastStrValue = strBuilder.toString();
            if (!lastStrValue.isEmpty()) {
                listaStrings.add(lastStrValue);
            }
        } catch (Exception e) {
            System.err.println("Problema a converter para lista");
        }
        return listaStrings;
    }

    private void enviaParaFicheiro(String depositarArquivo, ClienteView view) {
        List<String> respostasParaFicheiro = respostas.getOrDefault(depositarArquivo, new ArrayList<>());
        Path path = Paths.get(depositarArquivo);

        if (!respostasParaFicheiro.isEmpty()) {
            String conteudoArquivo = String.join(System.lineSeparator(), respostasParaFicheiro);
            try {
                Files.write(path, conteudoArquivo.getBytes());
                view.enviaParaFicheiro("Respostas depositadas no arquivo com sucesso!");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            view.enviaParaFicheiro("Nenhuma resposta disponível para depositar no arquivo.");
        }
    }
    public void shutdown(Socket socket) {
        try {
            // Fecha a Socket
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            // Encerra o programa
            System.exit(0);
        } catch (IOException e) {
            System.err.println("Erro ao fechar a conexão.");
        }
    }
}




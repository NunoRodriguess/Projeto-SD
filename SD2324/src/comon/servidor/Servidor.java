package comon.servidor;

import comon.Mensagem;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Servidor {

    public static void main(String[] args) {
        try {
            ServerSocket socket = new ServerSocket(12345);
            Manager manager = new Manager();
            ClienteHandler handler = new ClienteHandler(manager);
            new Thread(handler).start();

            while (true) {
                try {
                    Socket socketC = socket.accept();
                    DataInputStream inC = new DataInputStream(socketC.getInputStream());
                    DataOutputStream ouC = new DataOutputStream(socketC.getOutputStream());
                    System.out.println("Conexão estabelecida.\n");
                    System.out.println("Aguardando mensagens...");

                    new Thread(() -> {
                        boolean isRunning = true;
                        while (isRunning) {
                            Mensagem newMensagem;
                            try {
                                newMensagem = Mensagem.deserialize(inC);
                                String nome = newMensagem.getNome();

                                switch (newMensagem.getTipo()) {
                                    case AUTENTI_W -> {
                                        PedidodoBuffer pedidoAutenticacaoW = new PedidodoBuffer(newMensagem, socketC);
                                        handler.handleAutenticacaoWorker(pedidoAutenticacaoW);
                                    }
                                    case SHUT_DOWN -> {
                                        PedidodoBuffer pedidoDesligar= new PedidodoBuffer(newMensagem, socketC);
                                        manager.adicionarEnvio(pedidoDesligar);
                                        isRunning = false;
                                    }
                                    case ENVIO_RESPOSTA -> {
                                        PedidodoBuffer pedidoResposta = new PedidodoBuffer(newMensagem, socketC);
                                        manager.adicionarEnvio(pedidoResposta);
                                    }
                                    case AUTENTI_C ->{
                                        PedidodoBuffer pedidoAutenticacaoC = new PedidodoBuffer(newMensagem, socketC);
                                        try {
                                            if(!manager.isAutenticado(pedidoAutenticacaoC)){
                                            handler.handleAutenticacaoCliente(pedidoAutenticacaoC);
                                            new Thread(() -> {
                                                while (true) {
                                                    PedidodoBuffer enviar = manager.getEnvio(nome);
                                                    handler.enviaResposta(enviar);
                                                    try {
                                                        enviar.getMessage().sendMessage(ouC);
                                                    } catch (IOException e) {
                                                        throw new RuntimeException(e);
                                                    }
                                                    if(enviar.getMessage().getTipo().equals(Mensagem.TipoMensagem.SHUT_DOWN)){
                                                        break;
                                                    }
                                                }
                                            }).start();}else {
                                                System.out.println("Cliente já se encontra autenticado");
                                            }
                                        } catch (Exception e) {
                                            System.err.println("Erro na thread do AUTENTIC_C");
                                        }
                                    }
                                    default ->{
                                        PedidodoBuffer pb = new PedidodoBuffer(newMensagem, socketC);
                                        if (manager.isAutenticado(pb)){
                                            manager.updateP(pb, false);
                                            if(!(newMensagem.getTipo().equals(Mensagem.TipoMensagem.ENVIO_TAREFA))){
                                                pb.setPrioridade(10000); // estes são atendidos primeiro
                                            }
                                            manager.updateBQ(pb);

                                        }else{
                                            System.out.println("Cliente não se encontra autenticado");
                                        }

                                    }
                                }
                            } catch (IOException e) {
                                System.err.println("Erro ao receber a mensagem");
                                break; // Sair do loop
                            }
                        }
                    }).start();

                } catch (IOException e) {
                    System.err.println("Erro na stream");
                }
            }

        } catch (IOException e) {
            System.err.println("Erro no servidor.");
        }
    }
}


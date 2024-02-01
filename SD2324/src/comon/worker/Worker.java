package comon.worker;

import comon.Mensagem;

import comon.servidor.BoundedQueue;
import comon.servidor.PedidodoBuffer;
import sd23.*;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Worker {
    private final BoundedQueue tarefasPendentes;
    List<Mensagem> pbSend;

    Lock l;

    Condition espera;
    private final int memoriaTotal;
    private int memoriaUsada;
    private String nome;
    private String senha;

    public String getNome() {
        return nome;
    }
    public void setNome(String nome) {this.nome = nome;}
    public void setSenha(String senha) {this.senha = senha;}
    public void diminuiMemoriaDisp(int memoriaGasta){this.memoriaUsada += memoriaGasta;}
    public void aumentaMemoriaDisp(int memoriaGasta){this.memoriaUsada -= memoriaGasta;}
    public BoundedQueue getTarefasPendentes() {
        return tarefasPendentes;
    }
    public int getMemoriaDisponivel() {
        return memoriaTotal - memoriaUsada;
    }


    public Worker() {
        this.tarefasPendentes = new BoundedQueue();
        this.memoriaTotal = 0;
        this.memoriaUsada = 0;
        this.nome = null;
        this.senha = null;
        this.pbSend = new ArrayList<>();
        this.l = new ReentrantLock();
        this.espera = l.newCondition(); // para bloquear se não houver novas respostas
    }

    public Worker(Mensagem m) {
        this.tarefasPendentes = new BoundedQueue();
        this.memoriaTotal = m.getAux();
        this.memoriaUsada = 0;
        this.nome = m.getNome();
        this.senha = m.getSenha();
    }

    @Override
    public String toString() {
        return "Worker { nome = " + nome +
                ", tarefasPendentes= " + this.getTarefasPendentes().toString() + " }";
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Worker worker = (Worker) obj;
        return  memoriaTotal == worker.memoriaTotal &&
                memoriaUsada == worker.memoriaUsada &&
                nome.equals(worker.nome) &&
                senha.equals(worker.senha) &&
                tarefasPendentes.equals(worker.tarefasPendentes);

    }

    public static void main(String[] args) {
        new Worker().runWorker();
    }

    public void runWorker() {
        try {
            Socket socket = new Socket("localhost", 12345); // Conectar ao servidor na porta 12345

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());
            BufferedReader systemIn = new BufferedReader(new InputStreamReader(System.in));

            //Thread para enviar respostas
            Thread sendThread = createSendThread(out, systemIn);
            sendThread.start();

            Thread escreveThread = escreveThread(out);
            escreveThread.start();

            //Thread para receber tarefas
            Thread receiveThread = createReceiveThread(in);
            receiveThread.start();

        } catch (SocketException e) {
            System.err.println("Erro na conexão com o servidor.");
        } catch (IOException e) {
            System.err.println("Erro nas streams.");
        }
    }

    private Thread createSendThread(DataOutputStream out, BufferedReader systemIn){
        return new Thread(() -> {
            sendAutenticacao(out,systemIn); //Enviar autenticaçao
                procuraTarefa(); //Tratar as tarefas

        });
    }
    private Thread escreveThread(DataOutputStream out){
        return new Thread(() -> {
            escreveTarefa(out); //Tratar as tarefas
        });
    }

    private Thread createReceiveThread(DataInputStream in){
        return new Thread(() -> {
            try {
                while (true) {
                    Mensagem resposta;
                    resposta = Mensagem.deserialize(in);

                    switch (resposta.getTipo()) {
                        case ENVIO_TAREFA -> {
                            PedidodoBuffer pb = new PedidodoBuffer(resposta, null);
                            this.tarefasPendentes.add(pb);

                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Erro ao receber mensagem.");
            }
        });
    }

    private void sendAutenticacao(DataOutputStream out, BufferedReader systemIn) {
        try {
            System.out.print("Tipo de mensagem -> AUTENTICACAO\n");
            System.out.print("Nome do worker: ");
            String nomeWorker = systemIn.readLine();
            setNome(nomeWorker);
            System.out.print("Senha: ");
            String senha = systemIn.readLine();
            setSenha(senha);
            System.out.print("Memoria total: ");
            int memoriaTotal = Integer.parseInt(systemIn.readLine());

            // Enviar a mensagem de autenticação para o servidor
            Mensagem mensagem = new Mensagem(Mensagem.TipoMensagem.AUTENTI_W,
                    "null",
                    nomeWorker,
                    senha,
                    nomeWorker,
                    memoriaTotal,
                    null,
                    "null");
            mensagem.sendMessage(out);
        } catch (IOException e) {
            System.err.println("Erro ao enviar mensagem de autenticação.");
        }
    }

    private void executaTarefa( PedidodoBuffer pb) {
        byte[] job;
        byte[] output;

            try {
                job = pb.getDados(); // obter os bytes do pedido
                output = JobFunction.execute(job);// executar a tarefa
            } catch (JobFunctionException e) {
                System.err.println("job failed: code="+e.getCode()+" message="+e.getMessage());
                output = ("Execucao do pedido " + pb.getCodigoTarefa() + " falhada. " + e.getMessage()).getBytes();

            }


            Mensagem m = pb.getMessage();
            // Enviar resposta para o servidor
            Mensagem mensagem = new Mensagem(Mensagem.TipoMensagem.ENVIO_RESPOSTA,
                    pb.getCodigoTarefa(),
                    m.getNome(),
                    m.getSenha(),
                    nome,
                    0,
                    output,
                    m.getCaminho());

            // Para não misturar as respostas
        try {
            this.l.lock();
            this.pbSend.add(mensagem);
            this.espera.signal();
        }finally {
            this.l.unlock();
        }




    }

    private void procuraTarefa() {

            while (true) {

                PedidodoBuffer pb = this.tarefasPendentes.get();
                new Thread(()-> executaTarefa(pb)).start(); // Tratar a concorrência na execução dos pedidos
            }

    }
    private void escreveTarefa(DataOutputStream out) {
        while (true){
        try{
            this.l.lock();
            while(this.pbSend.isEmpty()){

                this.espera.await();
            }
            Mensagem m = this.pbSend.get(0);
            this.pbSend.remove(0);
            m.sendMessage(out);
        }catch (InterruptedException | IOException ignored){} finally {
            this.l.unlock();
        }
        }



    }
}
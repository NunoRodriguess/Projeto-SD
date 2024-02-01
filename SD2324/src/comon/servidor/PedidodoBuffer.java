package comon.servidor;

import comon.Mensagem;

import java.io.Serializable;
import java.net.Socket;
import java.util.Arrays;

public class PedidodoBuffer implements Serializable {

    private final Mensagem mensagem;
    private int prioridade;
    private final Socket socket; //socket para saber para onde tem que enviar
    byte[] job;

    public PedidodoBuffer(Mensagem mensagem, Socket socket){
        this.mensagem = mensagem;
        byte[] task = mensagem.getDadosTarefa();
        this.prioridade = 0;
        this.job = null;
        if(task!= null){
            this.prioridade = task.length;
            this.job = Arrays.copyOf(task, task.length);
        }
        this.socket = socket;
    }

    public Mensagem getMessage() {
        return this.mensagem;
    }
    public Socket getSocket() {
        return this.socket;
    }
    public String getCodigoTarefa() {
        return this.mensagem.getCodigoTarefa();
    }
    public int getPrioridade() {
        return this.prioridade;
    }
    public byte[] getDados() {
        return this.job;
    }
    public void setPrioridade(int i) {
        this.prioridade = i;
    }
    public String toString() {
        return "PedidodoBuffer{" +
                "prioridade=" + prioridade +
                "mensagem=" + mensagem.toString() +
                ", job=" + Arrays.toString(job) +
                '}';
    }
}

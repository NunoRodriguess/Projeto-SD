package comon;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Mensagem implements Serializable {
    final private String codigoTarefa;
    final private TipoMensagem tipo;
    final private String nome;
    final private String senha;
    final private String worker;
    final private int aux;
    final private byte[] dadosTarefa;
    final private String caminho;

    public Mensagem() {
        this.tipo = null;
        this.codigoTarefa = null;
        this.nome = null;
        this.senha = null;
        this.worker = null;
        this.aux = 0;
        this.dadosTarefa = null;
        this.caminho = null;
    }

    public Mensagem(TipoMensagem tipo,String codigoTarefa, String nome, String senha, String worker, int aux, byte[] dadosTarefa, String caminho) {
        this.tipo = tipo;
        this.codigoTarefa = codigoTarefa;
        this.nome = nome;
        this.senha = senha;
        this.worker = worker;
        this.aux = aux;
        this.dadosTarefa = dadosTarefa;
        this.caminho = caminho;
    }
    public String getCodigoTarefa() { return codigoTarefa; }
    public int getAux() {
        return aux;
    }
    public TipoMensagem getTipo() {
        return tipo;
    }
    public String getNome() {
        return nome;
    }
    public String getSenha() {
        return senha;
    }
    public String getWorker(){
        return worker;
    }
    public byte[] getDadosTarefa() {
        return dadosTarefa;
    }
    public String getCaminho(){
        return  this.caminho;
    }
    public enum TipoMensagem {
        AUTENTI_C, // Autenticação do cliente
        AUTENTI_W, // Autenticação do worker
        ENVIO_ERRO, // Enviar erros para cliente
        ENVIO_TAREFA, // Enviar tarefas do cliente
        ENVIO_RESPOSTA, // Enviar respostas do worker
        CONSULTA_R, // Consultar respostas do cliente
        CONSULTA_P, // Consultar pedidos pendentes do cliente
        CONSULTA_O, // Consultar ocupação do servidor
        CONSULTA_T, // Consultar tarefas pendentes do servidor
        SHUT_DOWN, // Consultar tarefas pendentes do servidor
    }

    @Override
    public String toString() {
        return "Tipo de Mensagem: " + tipo + "\n" +
                "Código da tarefa: " + codigoTarefa + "\n" +
                "Nome: " + nome + "\n" +
                "Senha: " + senha + "\n" +
                "Worker: " + worker + "\n" +
                "Dados da Tarefa: " + dadosTarefa + "\n" +
                "Caminho: " + caminho + "\n";
    }

    public void serialize(DataOutputStream outputStream) throws IOException {
        outputStream.writeInt(this.tipo.ordinal());// Escreve o tipo de mensagem
        if(codigoTarefa!=null) outputStream.writeUTF(this.codigoTarefa);// Escreve o código da tarefa
        else outputStream.writeUTF("null");

        if(nome!=null) outputStream.writeUTF(this.nome);// Escreve o nome
        else outputStream.writeUTF("null");

        if(senha!=null) outputStream.writeUTF(this.senha);// Escreve a senha
        else outputStream.writeUTF("null");

        if(worker!=null) outputStream.writeUTF(this.worker);// Escreve o nome do worker
        else outputStream.writeUTF("null");

        outputStream.writeInt(this.aux);// Escreve a memoria total ou o numero de tarefas pendentes

        // Escreve os dados da tarefa
        if (dadosTarefa != null) {
            outputStream.writeInt(this.dadosTarefa.length);
            outputStream.write(this.dadosTarefa);
        } else outputStream.writeInt(0);

        if(caminho!=null) outputStream.writeUTF(this.caminho);// Escreve o caminho do ficheiro de respostas
        else outputStream.writeUTF("null");

    }
    public void sendMessage(DataOutputStream outputStream) throws IOException {
        serialize(outputStream);
        outputStream.flush();

    }

    public static Mensagem deserialize(DataInputStream in) throws IOException {

        int tipoMensagem = in.readInt();
        TipoMensagem tp = TipoMensagem.values()[tipoMensagem];
        String codT = in.readUTF();// Lê o código da tarefa
        String nome = in.readUTF(); // Lê o nome
        String senha = in.readUTF();// Lê a senha
        String worker = in.readUTF();// Lê o nome do worker
        int aux = in.readInt();// Lê a memoria total ou o numero de tarefas pendentes
        // Lê os dados da tarefa a realizar
        int tamanhoDadosTarefa = in.readInt();
        byte[] dadosTarefa;
        if (tamanhoDadosTarefa > 0) {
            dadosTarefa = new byte[tamanhoDadosTarefa];
            in.readFully(dadosTarefa);

        } else{ dadosTarefa = null;}

        String caminho = in.readUTF();// Lê o caminho do ficheiro de respostas

        Mensagem m = new Mensagem(tp,codT,nome,senha,worker,aux,dadosTarefa,caminho);
        return m;
    }

    /*
    public void receiveMessage(DataInputStream inputStream) throws IOException {
        int tipoMensagem = inputStream.readInt();
        this.tipo = TipoMensagem.values()[tipoMensagem]; // Lê o tipo de mensagem
        this.codigoTarefa = inputStream.readUTF();// Lê o código da tarefa
        this.nome = inputStream.readUTF(); // Lê o nome
        this.senha = inputStream.readUTF();// Lê a senha
        this.worker = inputStream.readUTF();// Lê o nome do worker
        this.aux = inputStream.readInt();// Lê a memoria total ou o numero de tarefas pendentes

        // Lê os dados da tarefa a realizar
        int tamanhoDadosTarefa = inputStream.readInt();
        if (tamanhoDadosTarefa > 0) {
            byte[] dadosTarefa = new byte[tamanhoDadosTarefa];
            inputStream.readFully(dadosTarefa);
            this.dadosTarefa = dadosTarefa;
        } else this.dadosTarefa = null;

        this.caminho = inputStream.readUTF();// Lê o caminho do ficheiro de respostas
    }
    */

}

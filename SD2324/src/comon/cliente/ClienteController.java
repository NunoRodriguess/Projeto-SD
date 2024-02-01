package comon.cliente;

import java.io.DataOutputStream;
import java.io.Serializable;

public class ClienteController implements Serializable {
    private Cliente model;

    public ClienteController(Cliente model) {
        this.model = model;
    }

    public void solicitarAutenticacaoController(String nomeUser, String senha, String codigoTarefa, byte[]dadosTarefa, String depositarArquivo, DataOutputStream out) {
        model.sendJob(nomeUser, senha, codigoTarefa, dadosTarefa, depositarArquivo, out,0);
    }
    public void enviarPedidoController(String nomeUser, String senha, String codigoTarefa, byte[]dadosTarefa, String depositarArquivo, DataOutputStream out) {
        model.sendJob(nomeUser, senha, codigoTarefa, dadosTarefa, depositarArquivo, out, 1);
    }
    public void respostaPedidoController(String nomeUser, String senha, String codigoTarefa, byte[]dadosTarefa, String depositarArquivo, DataOutputStream out) {
        model.sendJob(nomeUser, senha, codigoTarefa, dadosTarefa, depositarArquivo, out, 2);
    }
    public void tarefasPendentesClienteController(String nomeUser, String senha, String codigoTarefa, byte[]dadosTarefa, String depositarArquivo, DataOutputStream out) {
        model.sendJob(nomeUser, senha, codigoTarefa, dadosTarefa, depositarArquivo, out, 3);
    }
    public void ocupacaoServicoController(String nomeUser, String senha, String codigoTarefa, byte[]dadosTarefa, String depositarArquivo, DataOutputStream out) {
        model.sendJob(nomeUser, senha, codigoTarefa, dadosTarefa, depositarArquivo, out, 4);
    }
    public void tarefasPendentesServicoController(String nomeUser, String senha, String codigoTarefa, byte[]dadosTarefa, String depositarArquivo, DataOutputStream out) {
        model.sendJob(nomeUser, senha, codigoTarefa, dadosTarefa, depositarArquivo, out, 5);
    }
    public void enviarShutDownController(String nomeUser, String senha, String codigoTarefa, byte[]dadosTarefa, String depositarArquivo, DataOutputStream out) {
        model.sendJob(nomeUser, senha, codigoTarefa, dadosTarefa, depositarArquivo, out, 6);
    }
}
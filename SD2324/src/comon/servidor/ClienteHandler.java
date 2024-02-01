package comon.servidor;

import comon.Mensagem;

import comon.worker.Worker;

import java.util.List;

public class ClienteHandler implements Runnable {
    private final Manager manager;

    public ClienteHandler(Manager manager){
        this.manager = manager;
    }

    public void run() {
        while (true) {
            try {
                // PedidodoBuffer pedidoBuffer = this.manager.getPedido();
                List<PedidodoBuffer> pedidos = this.manager.getPedidos();

                // Mensagem de erro quando não há workers no sistema
               if (this.manager.getWorkersDB().isEmpty()) {
                   for (PedidodoBuffer pedidoBuffer : pedidos){
                    Mensagem mensagem = pedidoBuffer.getMessage();

                    // Criar mensagem de erro

                    String erro = "Não foi possível enviar a mensagem";
                    byte[] dadosErro = erro.getBytes();
                    Mensagem mNova = new Mensagem(Mensagem.TipoMensagem.ENVIO_ERRO,mensagem.getCodigoTarefa(), mensagem.getNome(), mensagem.getSenha(), mensagem.getWorker(), mensagem.getAux(), dadosErro, mensagem.getCaminho());
                    PedidodoBuffer pedidoResposta = new PedidodoBuffer(mNova, pedidoBuffer.getSocket());
                    this.manager.adicionarEnvio(pedidoResposta);
                   }

                } else{
                   pedidos.sort((p1, p2) -> Integer.compare(p2.getPrioridade(), p1.getPrioridade()));
                   for (PedidodoBuffer pedidoBuffer : pedidos){
                   trataPedido(pedidoBuffer);
                   }

               } // Quando tem workers no sistema
            }catch (Exception e){
                throw new RuntimeException(e);
            }
        }
    }

    private void trataPedido(PedidodoBuffer pedidodoBuffer) throws Exception {
        switch (pedidodoBuffer.getMessage().getTipo()) {
            case AUTENTI_C -> handleAutenticacaoCliente(pedidodoBuffer);
            case AUTENTI_W -> handleAutenticacaoWorker(pedidodoBuffer);
            case ENVIO_TAREFA -> enviaTarefa(pedidodoBuffer);
            case ENVIO_RESPOSTA -> enviaResposta(pedidodoBuffer);
            case CONSULTA_R -> this.manager.consultaRespostasC(pedidodoBuffer);
            case CONSULTA_P -> this.manager.retornaPedidosC(pedidodoBuffer);
            case CONSULTA_O -> this.manager.ocupacaoServidor(pedidodoBuffer);
            case CONSULTA_T -> this.manager.nTPendentes(pedidodoBuffer);
        }
    }

    // Verifica as credênciais(nao permite que autentique duas vezes com o mesmo cliente/worker)
    public boolean autenticacao(PedidodoBuffer pb) {
        Mensagem m = pb.getMessage();
        if(m.getTipo().equals(Mensagem.TipoMensagem.AUTENTI_C)) {
            return !this.manager.getUsersDB().containsKey(m.getNome());
        }
        if(m.getTipo().equals(Mensagem.TipoMensagem.AUTENTI_W)) {
            return !this.manager.getWorkersDB().containsKey(pb.getSocket());
        }
        return true;
    }

    // Registo dos clientes no sistema
    public void handleAutenticacaoCliente(PedidodoBuffer pb) throws Exception{
        Mensagem mensagem = pb.getMessage();
        if (autenticacao(pb)) {
            this.manager.updateUserDB(mensagem);
            System.out.println("Cliente autenticado com sucesso");

        } else {
            System.out.println("Cliente já se encontra autenticado");
            throw new Exception();
        }
    }

    // Registo dos workers no sistema
    public void handleAutenticacaoWorker(PedidodoBuffer pb){
        Mensagem mensagem = pb.getMessage();
        if (autenticacao(pb)) {
            Worker worker = new Worker(mensagem);
            this.manager.updateWorkerDB(pb.getSocket(), worker); //Atualiza a base de dados
            System.out.println("Worker autenticado com sucesso");
        } else {
            System.out.println("Worker já se encontra autenticado");
        }
    }

    private void enviaTarefa(PedidodoBuffer pb) {
        Mensagem mensagem = pb.getMessage();
        this.manager.workerRecebeTarefa(pb); // Atribui tarefa ao worker

    }

    public void enviaResposta(PedidodoBuffer pb) {
        if(!pb.getMessage().getWorker().equals("null")){
            this.manager.aumentaMemoriaWorker(pb);
        }
        try{
            this.manager.updateP(pb, true); // Atualiza os pedidos pendentes do cliente
            this.manager.updateR(pb); // Atualiza a map de respostas do cliente
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}


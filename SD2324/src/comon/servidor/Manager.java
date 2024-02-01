package comon.servidor;
import comon.Mensagem;
import comon.worker.Worker;
import comon.worker.WorkerWrapper;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.locks.*;
import java.util.stream.Collectors;

class Manager {
    private final ReentrantLock lockAutent = new ReentrantLock();
    private final ReentrantReadWriteLock lockPedidos = new ReentrantReadWriteLock ();
    private final ReentrantReadWriteLock  lockRespostas = new ReentrantReadWriteLock ();
    private final ReentrantLock lockEnvios = new ReentrantLock();
    private final ReentrantLock workerLock = new ReentrantLock();
    private final Condition melhorWorker = workerLock.newCondition();
    private final HashMap<String, String> userDataBase = new HashMap<>();
    private final HashMap<Socket, WorkerWrapper> workersDataBase = new HashMap<>();
    private final Map<String, Conta> respostas = new HashMap<>(); //nome cliente, value = respostas ao pedido
    private final Map<String, Wrapper> envios = new HashMap<>(); //nome cliente, wrapper com as repostas que vão ser enviadas
    private final Map<String, Conta> pedidos = new HashMap<>(); //nome cliente, value = pedidos dos clientes
    private final BoundedQueue q = new BoundedQueue(); //pedidos gerais

    // Atualiza a map de clientes do sistema e incializa as outras listas
    public void updateUserDB(Mensagem m) {
        lockAutent.lock();
        lockEnvios.lock();
        lockPedidos.writeLock().lock();
        lockRespostas.writeLock().lock();
        try {
            userDataBase.put(m.getNome(), m.getSenha());
            Wrapper w = new Wrapper(lockEnvios.newCondition());
            envios.put(m.getNome(), w);
            Conta cR = new Conta();
            respostas.put(m.getNome(), cR);
            Conta cP = new Conta();
            pedidos.put(m.getNome(), cP);

        } finally {

            lockAutent.unlock();
            lockEnvios.unlock();
            lockPedidos.writeLock().unlock();
            lockRespostas.writeLock().unlock();

        }
    }


    // Atualiza a map de workers do sistema
    public void updateWorkerDB(Socket socket, Worker newWorker) {
        workerLock.lock();
        try {
            WorkerWrapper w = new WorkerWrapper();
            w.worker = newWorker;
            workersDataBase.put(socket,w);
            melhorWorker.signal();
        }finally {
            workerLock.unlock();

        }
    }

    // Método para adicionar a mensagem de resposta à lista de respostas do cliente
    public void updateR(PedidodoBuffer pb) {
        Conta c;
        lockRespostas.readLock().lock();
        try {
            // Adiciona a mensagem à lista do cliente
             c = respostas.get(pb.getMessage().getNome());
            c.l.lock();
        } finally {
            lockRespostas.readLock().unlock();
        }
        try{
            c.pl.add(pb);
        }finally {
            c.l.unlock();

        }
    }

    // Método que atualiza os pedidos do cliente(remove ou adiciona)
    public void updateP(PedidodoBuffer pedido, boolean removerPedido) {
        Mensagem mensagem = pedido.getMessage();
        Conta c;
        if (removerPedido){
            try{
            lockPedidos.readLock().lock();

            c = pedidos.get(pedido.getMessage().getNome());
            c.l.lock();
            }finally {
                lockPedidos.readLock().unlock();
            }
            try{
                Iterator<PedidodoBuffer> it = c.pl.iterator();
                while(it.hasNext()){
                    PedidodoBuffer p = it.next();
                    if (p.getCodigoTarefa().equals(pedido.getCodigoTarefa()))
                        it.remove();
                }

            }finally {
                c.l.unlock();
            }
        }else {
            try{
                lockPedidos.readLock().lock();

                c = pedidos.get(pedido.getMessage().getNome());
                c.l.lock();
            }finally {
                lockPedidos.readLock().unlock();
            }try{
                c.pl.add(pedido);
            }finally {
                c.l.unlock();
            }

        }

    }

    // Método para atualizar a boundedqueue do sistema
    public void updateBQ(PedidodoBuffer pb) {
        this.q.add(pb);
    }

    // Método para retornar todos os clientes do sistema
    public HashMap<String, String> getUsersDB() {
        lockAutent.lock();
        try{
            return userDataBase;
        }finally {
            lockAutent.unlock();
        }
    }

    // Método para retornar todos os workers do sistema
    public HashMap<Socket, WorkerWrapper> getWorkersDB() {
        workerLock.lock();
        try {
            return workersDataBase;
        } finally {
            workerLock.unlock();
        }
    }

    // Método para retornar todos os pedidos da boundedqueue do sistema
    public PedidodoBuffer getPedido(){
        return this.q.get();
    }

    public List<PedidodoBuffer> getPedidos(){
        return this.q.getAll();
    }

    // Método para retornar o melhor worker do sistema tendo em conta a sua memoria disponível
    public Worker getWorkerByMemory() {
        workerLock.lock();
        try{
            WorkerWrapper ww = workersDataBase.values().stream()
                    .max(Comparator.comparingInt(WorkerWrapper ::getMemoriaDisponivel))
                    .orElse(null);
            if (ww ==null)
                return null;
            else return ww.worker;
        } finally {
            workerLock.unlock();
        }

    }
    public Worker getWorkerWithLeastAvailableMemory(int neededMemory) {
        workerLock.lock();
        try {
            List<WorkerWrapper> capableWorkers = workersDataBase.values()
                    .stream()
                    .filter(worker -> worker.getMemoriaDisponivel() >= neededMemory)
                    .collect(Collectors.toList());

            return capableWorkers.stream()
                    .min(Comparator.comparingInt(WorkerWrapper::getMemoriaDisponivel))
                    .map(WorkerWrapper::getWorker)
                    .orElse(null);
        } finally {
            workerLock.unlock();
        }
    }

    // Método para atribuir uma tarefa ao melhor worker encontrado
    public void workerRecebeTarefa(PedidodoBuffer pb) {

        try {
            workerLock.lock();
            Mensagem mensagem = pb.getMessage();
            int memoriaNecessaria = mensagem.getDadosTarefa().length;
            Worker worker;
            /* Isto corresponde à versão onde o pedido vai para o com mais memória disponível (Desde que possa receber)
            while((worker = getWorkerByMemory()).getMemoriaDisponivel() < memoriaNecessaria){
                melhorWorker.await(); // Até que haja algum worker que consiga realizar a tarefa
            }
            */ // Esta versão escolhe dentro dos que podem receber o que têm menos memória disponível (best fit)
            while ((worker = getWorkerWithLeastAvailableMemory(memoriaNecessaria)) == null) {
                melhorWorker.await(); // Wait until there's a worker capable of handling the task
            }
            // Encontrar a socket do worker em questão
            for (Map.Entry<Socket, WorkerWrapper> entry : this.workersDataBase.entrySet()) {
                if (entry.getValue().worker.equals(worker)) {
                    DataOutputStream outW = new DataOutputStream(entry.getKey().getOutputStream());

                    // Envia a tarefa para o worker
                    pb.getMessage().sendMessage(outW);
                    entry.getValue().pb.add(pb);
                    worker.diminuiMemoriaDisp(memoriaNecessaria); //Atualizar a memoria do worker
                    break;
                }
            }
        } catch (IOException | InterruptedException e){

        } finally {
            workerLock.unlock();
        }
    }

    private byte[] converterListaParaBytes(List<String> listaStrings) {
        List<Byte> listaBytes = new ArrayList<>();

        for (String listaString : listaStrings) {
            byte[] bytesString = listaString.getBytes(StandardCharsets.UTF_8);
            for (byte b : bytesString) {
                listaBytes.add(b);
            }
            // Adiciona um byte de separação entre as strings (opcional)
            listaBytes.add((byte) 0);
        }
        byte[] resultado = new byte[listaBytes.size()];
        for (int i = 0; i < listaBytes.size(); i++) {
            resultado[i] = listaBytes.get(i);
        }
        return resultado;
    }

    // Método para enviar mensagem ao cliente com as suas repostas obtidas
    public void consultaRespostasC(PedidodoBuffer pedido) {
        Mensagem mensagem = pedido.getMessage();
        List<String> respostasCliente = new ArrayList<>();
        Conta c;

            try{
                lockRespostas.readLock().lock();
                c = this.respostas.get(mensagem.getNome());
                c.l.lock();

            } finally {
                lockRespostas.readLock().unlock();
            }
            try{

                if (c.pl!= null && !c.pl.isEmpty()) {
                    for (PedidodoBuffer p : c.pl) {
                        respostasCliente.add("Pedido " + p.getCodigoTarefa() +
                                ": " + p.getMessage().getDadosTarefa());
                    }
                }
                else respostasCliente.add("Cliente ainda nao tem respostas. ");

            }finally {
                c.l.unlock();
            }
            byte[] respostas = converterListaParaBytes(respostasCliente);

            Mensagem m = new Mensagem(Mensagem.TipoMensagem.CONSULTA_R, "ConsultaR", mensagem.getNome(),mensagem.getSenha(), mensagem.getWorker(), 0, respostas, mensagem.getCaminho());
            PedidodoBuffer pedidoResposta = new PedidodoBuffer(m, pedido.getSocket());
            updateP(pedido,true);
            adicionarEnvio(pedidoResposta);


    }

    // Método para enviar mensagem ao cliente com os seus pedidos pendentes
    public void retornaPedidosC(PedidodoBuffer pedido) {
        Mensagem mensagem = pedido.getMessage();
        List<String> pedidosCliente = new ArrayList<>();
        Conta c;
            try{
                lockPedidos.readLock().lock();
                c = this.pedidos.get(mensagem.getNome());
                c.l.lock();

            } finally {
                lockPedidos.readLock().unlock();
            }
            try{

                if (c.pl!= null && !c.pl.isEmpty()) {
                    for (PedidodoBuffer p : c.pl) {
                        if ( !(p.getCodigoTarefa().equals(pedido.getCodigoTarefa())) ){
                        pedidosCliente.add("Pedido " + p.getCodigoTarefa() +
                                ": " + p.getMessage().getDadosTarefa());
                    }
                    }
                }
                else pedidosCliente.add("Cliente não tem pedidos. ");

            }finally {
                c.l.unlock();
            }
            byte[] pedidos = converterListaParaBytes(pedidosCliente);

            Mensagem m = new Mensagem(Mensagem.TipoMensagem.CONSULTA_P, "ConsultaP", mensagem.getNome(),mensagem.getSenha(), mensagem.getWorker(), 0, pedidos, mensagem.getCaminho());
            PedidodoBuffer pedidoResposta = new PedidodoBuffer(m, pedido.getSocket());
            updateP(pedido,true);
            adicionarEnvio(pedidoResposta);


    }

    // Método para obter informações sobre a disponibilidade dos workers no sistema
    public void ocupacaoServidor(PedidodoBuffer pedido) {
        List<String> informacoes = new ArrayList<>();
        Mensagem mensagem = pedido.getMessage();
        int memoriaDisponivelTotal = 0;

                try{
                    workerLock.lock();
                for (Map.Entry<Socket, WorkerWrapper> entry : workersDataBase.entrySet()) {
                    Worker worker = entry.getValue().worker;
                    String nomeWorker = worker.getNome();
                    int memoriaDisponivel = worker.getMemoriaDisponivel();

                    informacoes.add(String.format("(%s, %d)", nomeWorker, memoriaDisponivel));
                    memoriaDisponivelTotal += memoriaDisponivel;
                }
                informacoes.add(", Memoria Disponivel Total: " + memoriaDisponivelTotal);
                byte[] ocupacao = converterListaParaBytes(informacoes);

                Mensagem m = new Mensagem(Mensagem.TipoMensagem.CONSULTA_O, "OcupacaoS", mensagem.getNome(),mensagem.getSenha(), mensagem.getWorker(), 0, ocupacao, mensagem.getCaminho());
                PedidodoBuffer pedidoResposta = new PedidodoBuffer(m, pedido.getSocket());
                updateP(pedido,true);
                adicionarEnvio(pedidoResposta);
            } finally {
                workerLock.unlock();
            }


    }

    // Método para retornar o numero de pedidos pendentes no sistema(congestionamento)
    public void nTPendentes(PedidodoBuffer pedido) {
        Mensagem mensagem = pedido.getMessage();
        boolean b;
                int pedidosPendentes = contarTotalPedidos();

                Mensagem m = new Mensagem(Mensagem.TipoMensagem.CONSULTA_T, "TarefasP", mensagem.getNome(),mensagem.getSenha(), mensagem.getWorker(), pedidosPendentes, null, mensagem.getCaminho());
                PedidodoBuffer pedidoResposta = new PedidodoBuffer(m, pedido.getSocket());
                updateP(pedido,true);
                adicionarEnvio(pedidoResposta);


    }
    public boolean isAutenticado(PedidodoBuffer pedido){
        try {
            lockAutent.lock();
            if (userDataBase.containsKey(pedido.getMessage().getNome())){

                return userDataBase.get(pedido.getMessage().getNome()).equals(pedido.getMessage().getSenha());

            }else return false;


        }finally {
            lockAutent.unlock();
        }

    }

    // Método auxiliar de "nTPendentes"
    private int contarTotalPedidos() {
        int totalPedidos = 0;
        try {
            lockPedidos.readLock().lock();
            for (Conta listaPedidos : this.pedidos.values()) {
                listaPedidos.l.lock();

            }
        }finally {
            lockPedidos.readLock().unlock();
        }

        for (Conta listaPedidos : this.pedidos.values()) {
            totalPedidos += listaPedidos.pl.size();
            listaPedidos.l.unlock();
        }
            return totalPedidos-1; // assim não aparece o pedido de consulta


    }

    public void aumentaMemoriaWorker(PedidodoBuffer pedido) {
        workerLock.lock();
        try {
            // Atualiza a memória disponível do worker
            WorkerWrapper ww = this.workersDataBase.get(pedido.getSocket());
            Worker worker = ww.worker;
            int mais = 0;
            PedidodoBuffer rem = null;
            for(PedidodoBuffer p:ww.pb) {

                if (p.getCodigoTarefa().equals(pedido.getCodigoTarefa())) {
                    mais = p.getDados().length;
                    rem = p;
                    break;

                }

            }
            ww.pb.remove(rem);
            worker.aumentaMemoriaDisp(mais);
            melhorWorker.signal();
        } finally {
            workerLock.unlock();
        }
    }

    public void adicionarEnvio(PedidodoBuffer pedido){
        lockEnvios.lock();
        try {
            Wrapper w = envios.get(pedido.getMessage().getNome());
            w.pb.add(pedido);
            w.c.signal();
        } finally {
            lockEnvios.unlock();
        }

    }
    public PedidodoBuffer getEnvio(String nome){
        lockEnvios.lock();
        try {
            Wrapper w = envios.get(nome);
            while(w.pb.isEmpty()){
                w.c.await();
            }
            PedidodoBuffer p = w.pb.get(0);
            w.pb.remove(p);
            return p;

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lockEnvios.unlock();
        }

    }
}

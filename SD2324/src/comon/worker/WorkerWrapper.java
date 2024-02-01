package comon.worker;

import comon.servidor.PedidodoBuffer;

import java.util.ArrayList;
import java.util.List;

public class WorkerWrapper {
    public Worker worker;
    public List<PedidodoBuffer> pb = new ArrayList<>();

    public int getMemoriaDisponivel() {
        return worker.getMemoriaDisponivel();
    }

    public Worker getWorker() {
        return worker;
    }
}

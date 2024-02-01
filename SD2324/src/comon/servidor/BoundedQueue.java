package comon.servidor;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class BoundedQueue {
    ReentrantLock l = new ReentrantLock();
    Condition handlerC = l.newCondition(); // Condição das Threads que retiram pedidos do buffer
    PriorityQueue<PedidodoBuffer> pq;

    public BoundedQueue(){
        //Inicializa a priorityQueue usando um comparador de prioridades(menos prioritário fica na cabeça)
        //Menos prioritário = mais memoria necessária
        this.pq = new PriorityQueue<>(Comparator.comparingInt(PedidodoBuffer::getPrioridade));
    }

    @Override
    public String toString() {
        try {
            l.lock();
            return "\nBoundedQueue{" +
                    "pq=" + pq.toString() +
                    '}';
        } finally {
            l.unlock();
        }
    }

    public void add(PedidodoBuffer pedido) {
        try {
            l.lock();
            pq.add(pedido);
            handlerC.signal();
        }finally {
            l.unlock();
        }
    }

    public PedidodoBuffer get(){
        try {
            l.lock();
            while(pq.isEmpty()){
                handlerC.await();
            }
            PedidodoBuffer item = pq.poll();

            int newPriority = item.getPrioridade();
            for (PedidodoBuffer buffer : pq) {
                int currentPriority = buffer.getPrioridade();
                if (currentPriority > newPriority) {
                    buffer.setPrioridade(currentPriority - newPriority); // Reduz a prioridade
                }
            }
            return item;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            l.unlock();
        }
    }

    public List<PedidodoBuffer> getAll() {
        try {
            l.lock();
            List<PedidodoBuffer> items = new ArrayList<>();

            while (!pq.isEmpty()) {
                PedidodoBuffer item = pq.poll();
                items.add(item);
            }

            return items;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            l.unlock();
        }
    }


    public boolean isEmptyBQ() {
        try {
            l.lock();
            return pq.isEmpty();
        } finally {
            l.unlock();
        }
    }
}

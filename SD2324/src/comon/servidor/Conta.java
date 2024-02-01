package comon.servidor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Conta {

    public Lock l;
    public List<PedidodoBuffer> pl;

    public Conta(){
        l  = new ReentrantLock();
        pl = new ArrayList<>();
    }
}

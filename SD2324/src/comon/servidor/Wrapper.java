package comon.servidor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;

public class Wrapper {
    public Condition c;
    public List<PedidodoBuffer> pb;

    public Wrapper(Condition nc){
        c = nc;
        pb = new ArrayList<>();
    }
}

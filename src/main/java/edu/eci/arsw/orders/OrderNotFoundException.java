package edu.eci.arsw.orders;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException (String id){
        super("Order " + id + " does not exist");
    }
}

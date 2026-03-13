package com.example.model;

import lombok.*;

@Getter
@Setter
public class Node {

    private final String key;
    private int value;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Node prev;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Node next;

    public Node(String key, int value){
        this.key = key;
        this.value = value;
    }

}

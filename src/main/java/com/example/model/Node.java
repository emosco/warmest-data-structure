package com.example.model;

import lombok.Data;
import lombok.ToString;
import lombok.EqualsAndHashCode;

@Data
public class Node {

    private String key;
    private Integer value;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Node prev;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Node next;

    public Node(String key, Integer value){
        this.key = key;
        this.value = value;
    }

}

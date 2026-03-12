package com.example.service;

import com.example.api.WarmestDataStructureInterface;
import com.example.model.Node;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class WarmestDataStructureService implements WarmestDataStructureInterface {

    private Map<String, Node> map = new HashMap<>();
    private Node warmest;

    @Override
    public Integer put(String key, int value) {
        Node node = map.get(key);
        if (node == null){
            Node newNode = new Node(key, value);
            map.put(key, newNode);
            setWarmest(newNode);
            return null;
        }
        Integer oldValue = node.getValue();
        node.setValue(value);
        setWarmest(node);
        return oldValue;
    }

    @Override
    public Integer remove(String key) {
        Node node = map.get(key);
        if (node == null){
            return null;
        }
        if (node.equals(this.warmest)) {
            this.warmest = this.warmest.getPrev();
        }
        disconnectNode(node);
        map.remove(key);
        return node.getValue();
    }

    @Override
    public Integer get(String key) {
        Node node = map.get(key);
        if (node == null) {
            return null;
        }
        setWarmest(node);
        return node.getValue();
    }

    @Override
    public String getWarmest() {
        if (warmest == null){
            return null;
        }
        return warmest.getKey();
    }

    private void setWarmest(Node node){
        if (this.warmest == null){
            this.warmest = node;
        }
        else if (node != null && !this.warmest.equals(node)){
            disconnectNode(node);
            this.warmest.setNext(node);
            node.setPrev(this.warmest);
            this.warmest = node;
        }
    }

    private void disconnectNode(Node node) {
        if (node != null) {
            Node prev = node.getPrev();
            Node next = node.getNext();
            if (prev != null) {
                prev.setNext(node.getNext());
            }
            if (next != null) {
                next.setPrev(node.getPrev());
            }
            node.setPrev(null);
            node.setNext(null);
        }
    }
}

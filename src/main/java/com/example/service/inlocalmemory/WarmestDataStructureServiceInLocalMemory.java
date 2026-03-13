package com.example.service.inlocalmemory;

import com.example.api.WarmestDataStructureInterface;
import com.example.model.Node;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
@Profile("inlocalmemory")
public class WarmestDataStructureServiceInLocalMemory implements WarmestDataStructureInterface {

    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Map<String, Node> map = new HashMap<>();
    private Node warmest;

    @Override
    public Integer put(String key, int value) {
        readWriteLock.writeLock().lock();
        try {
            Node node = map.get(key);
            //create new
            if (node == null) {
                Node newNode = new Node(key, value);
                map.put(key, newNode);
                setWarmest(newNode);
                return null;
            }
            //updating existing
            Integer oldValue = node.getValue();
            node.setValue(value);
            setWarmest(node);
            return oldValue;
        }
        finally {
            readWriteLock.writeLock().unlock();
        }
    }

    @Override
    public Integer remove(String key) {
        readWriteLock.writeLock().lock();
        try {
            Node node = map.get(key);
            if (node == null){
                return null;
            }
            if (node == warmest) {
                this.warmest = this.warmest.getPrev();
            }
            disconnectNode(node);
            map.remove(key);
            return node.getValue();
        }
        finally {
            readWriteLock.writeLock().unlock();
        }
    }

    @Override
    public Integer get(String key) {
        readWriteLock.writeLock().lock();
        try {
            Node node = map.get(key);
            if (node == null) {
                return null;
            }
            setWarmest(node);
            return node.getValue();
        }
        finally {
            readWriteLock.writeLock().unlock();
        }
    }

    @Override
    public String getWarmest() {
        readWriteLock.readLock().lock();
        try {
            if (warmest == null){
                return null;
            }
            return warmest.getKey();
        }
        finally {
            readWriteLock.readLock().unlock();
        }
    }

    private void setWarmest(Node node){
        if (this.warmest == null){
            this.warmest = node;
        }
        else if (node != null && this.warmest != node){
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
                prev.setNext(next);
            }
            if (next != null) {
                next.setPrev(prev);
            }
            node.setPrev(null);
            node.setNext(null);
        }
    }
}

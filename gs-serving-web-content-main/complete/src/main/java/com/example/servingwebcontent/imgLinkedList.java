package com.example.servingwebcontent;

public class imgLinkedList {
    imgNode head;

    public void add(imgNode node){
        if(head == null){
            head = node;
        }else{
            node.next = head.next;
            head.next = node;
        }
    }

    public imgLinkedList(imgNode node){
        head = node;
    }

    public boolean hasNext(imgNode node){
        if(node.next == null){
            return false;
        }
        return true;
    }
}

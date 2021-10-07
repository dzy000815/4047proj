package com.example.servingwebcontent;

public class LinkedList {

    Node head;

    public void add(Node node){
        if(head == null){
            head = node;
        }else{
            node.next = head.next;
            head.next = node;
        }
    }

    public LinkedList(Node node){
        head = node;
    }
}

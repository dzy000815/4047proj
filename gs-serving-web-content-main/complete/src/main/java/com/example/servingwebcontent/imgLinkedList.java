package com.example.servingwebcontent;

public class imgLinkedList {
    imgNode head;
    imgNode next;

    public void add(imgNode node){
        if(head == null){
            head = node;
        }else{
            node.next = head.next;
            head.next = node;
        }
    }
}

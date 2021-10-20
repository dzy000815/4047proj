package com.example.servingwebcontent;

import java.util.ArrayList;
import java.util.List;

public class image {
    String src;
    String url;
    List<String> alt = new ArrayList<>() ;

    public image(String src, String url, List<String> alt){

        this.src = src;
        this.url = url;
        this.alt = alt;
    }

}
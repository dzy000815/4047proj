package com.example.servingwebcontent;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import javax.swing.text.html.*;
import javax.swing.text.html.HTML.*;
import javax.swing.text.html.HTMLEditorKit.*;
import javax.swing.text.html.parser.*;
import javax.swing.text.*;


@Controller
public class GreetingController {

	Stack<String> URLPool = new Stack<>();
	List<String> ProcessedPool = new ArrayList<>();
	public Hashtable wordList = new Hashtable();
	public Hashtable imgList = new Hashtable();
	@GetMapping("load")
	@ResponseBody
	String loadWebPage(@RequestParam(name = "query", required = false, defaultValue = "there")
							   String urlString) {
		byte[] buffer = new byte[1024];
		String content = new String();
		URLPool.push(urlString);
		List<String> uniqueContent = new ArrayList<>() ;
		List<String> urls = new ArrayList<>() ;
		List<String> imgs = new ArrayList<>() ;
		try {

			URL url = new URL(urlString);
			InputStream in = url.openStream();

			uniqueContent = getUniqueWords(loadPlainText(urlString));
			for(String word : uniqueContent){
				if(!wordList.contains(word)){
					wordList.put(word,new LinkedList(new Node(urlString)));
				}else{
					((LinkedList) wordList.get(word)).add(new Node(urlString));
				}

			}
			urls = getURLs(urlString);
			for(String u : urls){
				if(!ProcessedPool.contains(u)){
					ProcessedPool.add(u);
				}
			}
			imgs = getimgs(urlString);
			for(String i : imgs){
				if(!imgList.contains(i)){
					imgList.put(i,new LinkedList(new Node(urlString)));
				}else{
					((LinkedList) imgList.get(i)).add(new Node(urlString));
				}

			}
			if(ProcessedPool.size() <= 100){
				ProcessedPool.add(URLPool.pop());
			}

		} catch (IOException e) {
			e.printStackTrace();
			content = "<h1>Unable to download the page</h1>" + urlString;

		}

		return content;
	}

	class MyParserCallback extends HTMLEditorKit.ParserCallback {
		public String content = new String();
		public List<String> urls = new ArrayList<String>();
		public List<String> imgs = new ArrayList<String>();

		@Override
		public void handleText(char[] data, int pos) {
			content += " " + new String(data);
		}

		@Override
		public void handleStartTag(Tag tag, MutableAttributeSet attrSet, int pos)
		{
			if (tag.toString().equals("a")) {

				Enumeration e = attrSet.getAttributeNames();

				while (e.hasMoreElements()) {

					Object aname = e.nextElement();

					if (aname.toString().equals("href")) {
						String u = (String) attrSet.getAttribute(aname);
						if (URLPool.size() < 11 && !urls.contains(u) && !URLPool.contains(u) && !ProcessedPool.contains(u) && isAbsURL(u)){
							urls.add(u);
							URLPool.push(u);
						}
					}
				}
			}

		}

		@Override
		public void handleSimpleTag(Tag tag, MutableAttributeSet attrSet, int pos)
		{
			if (tag.toString().equals("img")) {

				Enumeration e = attrSet.getAttributeNames();

				while (e.hasMoreElements()) {

					Object aname = e.nextElement();

					if (aname.toString().equals("src")) {
						String u = (String) attrSet.getAttribute(aname);

						if (!imgs.contains(u))
							imgs.add(u);
					}
				}
			}
		}

	}

	String loadPlainText(String urlString) throws IOException {
		MyParserCallback callback = new MyParserCallback();
		ParserDelegator parser = new ParserDelegator();

		URL url = new URL(urlString);
		InputStreamReader reader = new InputStreamReader(url.openStream());
		parser.parse(reader, callback, true);

		return callback.content;
	}

	public static List<String> getUniqueWords(String text) {
		String[] words = text.split("[0-9\\W]+");
		ArrayList<String> uniqueWords = new ArrayList<String>();

		for (String w : words) {
			w = w.toLowerCase();

			if (!uniqueWords.contains(w))
				uniqueWords.add(w);
		}

		uniqueWords.sort(new Comparator<String>() {
			@Override
			public int compare(String a, String b) {
				return a.compareTo(b);
			}
		});

		return uniqueWords;
	}

	List<String> getURLs(String srcPage) throws IOException {
		URL url = new URL(srcPage);
		InputStreamReader reader = new InputStreamReader(url.openStream());

		ParserDelegator parser = new ParserDelegator();
		MyParserCallback callback = new MyParserCallback();
		parser.parse(reader, callback, true);

		for (int i=0; i<callback.urls.size(); i++) {
			String str = callback.urls.get(i);
			if (!isAbsURL(str))
				callback.urls.set(i, toAbsURL(str, url).toString());
		}

		return callback.urls;

	}

	boolean isAbsURL(String str) {
		return str.matches("^[a-z0-9]+://.+");
	}

	URL toAbsURL(String str, URL ref) throws MalformedURLException {
		URL url = null;

		String prefix = ref.getProtocol() + "://" + ref.getHost();

		if (ref.getPort() > -1)
			prefix += ":" + ref.getPort();

		if (!str.startsWith("/")) {
			int len = ref.getPath().length() - ref.getFile().length();
			String tmp = "/" + ref.getPath().substring(0, len) + "/";
			prefix += tmp.replace("//", "/");
		}
		url = new URL(prefix + str);

		return url;
	}

	List<String> getimgs(String srcPage) throws IOException {
		URL url = new URL(srcPage);
		InputStreamReader reader = new InputStreamReader(url.openStream());

		ParserDelegator parser = new ParserDelegator();
		MyParserCallback callback = new MyParserCallback();
		parser.parse(reader, callback, true);

		return callback.imgs;

	}


}

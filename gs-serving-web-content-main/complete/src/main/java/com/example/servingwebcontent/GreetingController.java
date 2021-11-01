package com.example.servingwebcontent;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
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

import java.io.*;


@Controller
public class GreetingController {

	Stack<String> URLPool = new Stack<>();
	List<String> ProcessedPool = new ArrayList<>();
	public Hashtable wordList = new Hashtable();
	public Hashtable imgList = new Hashtable();
	public String WebURL;
	public List<String> BlackListUrls = new ArrayList<>();
	public List<String> BlackListWords = new ArrayList<>();
	@RequestMapping(value = "/load",method = RequestMethod.GET)
	public String loadWebPage(@RequestParam(name = "query", required = false, defaultValue = "there")
							   String urlString, Model model) throws ServletException, IOException{

		try{
			String filename1 = "/Users/lusi/Desktop/4047proj/blackListUrls.txt";
			String filename2 = "/Users/lusi/Desktop//4047proj/blackListWords.txt";
			File BlackUrl = new File(filename1);
			File BlackWord = new File(filename2);
			FileInputStream in1 = new FileInputStream(BlackUrl);
			FileInputStream in2 = new FileInputStream(BlackWord);
			InputStreamReader reader1 = new InputStreamReader(in1);
			BufferedReader buffReader1 = new BufferedReader(reader1);
			InputStreamReader reader2 = new InputStreamReader(in2);
			BufferedReader buffReader2 = new BufferedReader(reader2);

			String line = "";
			while((line = buffReader1.readLine()) != null){
				BlackListUrls.add(line);
			}
			while ((line = buffReader2.readLine()) != null){
				BlackListWords.add(line);
			}

		}catch (Exception e){
			e.printStackTrace();
		}
		for(String url:BlackListUrls){
			if(url.endsWith("*")){
				if(urlString.startsWith(url.substring(0,url.lastIndexOf('*')))){
					return "BlackSeed";
				}
			}else if(url.equals(urlString)){
				return "BlackSeed";
			}

		}
		try{
			URL url_test = new URL(urlString);
			URLPool.push(urlString);
			load(urlString);
		}catch (IOException e){
			return "BlackSeed";
		}


		while(ProcessedPool.size() < 5 && !URLPool.empty()){
			load(URLPool.peek());
		}

		try {
			File writename = new File("word.txt");
			File writename2 = new File("url.txt");
			File writename3 = new File("image.txt");
			BufferedWriter out = new BufferedWriter(new FileWriter(writename));
			BufferedWriter out2 = new BufferedWriter(new FileWriter(writename2));
			BufferedWriter out3 = new BufferedWriter(new FileWriter(writename3));
			Enumeration keys1 = wordList.keys();
			while (keys1.hasMoreElements()){
				String key = keys1.nextElement().toString();
				out.write(String.valueOf(key));
				out.write(" ");
				LinkedList linkedList = (LinkedList)wordList.get(key);
				Node n = linkedList.head;
				out.write(n.value);
				out.write(", ");
				while(linkedList.hasNext(n)){
					n = n.next;
					out.write(n.value);
					out.write(", ");
				}
				out.write("\n");
			}
			out.close();

			for(String url:ProcessedPool){
				out2.write(url);
				out2.write("\n");
			}

			out2.close();

			Enumeration keys2 = imgList.keys();
			while (keys2.hasMoreElements()){
				String key = keys2.nextElement().toString();
				out3.write(String.valueOf(key));
				out3.write(" ");
				imgLinkedList linkedList = (imgLinkedList)imgList.get(key);
				imgNode n = linkedList.head;
				out3.write(n.img.src);
				out3.write(", ");
				out3.write(n.img.url);
				out3.write(", ");
				while(linkedList.hasNext(n)){
					n = n.next;
					out3.write(n.img.src);
					out3.write(", ");
					out3.write(n.img.url);
					out3.write(", ");
				}
				out3.write("\n");
			}
			out3.close();

		} catch (IOException e) {
		}

		model.addAttribute("query",urlString);

		return "SearchKey";
	}



	@GetMapping("SearchKey")
	@ResponseBody
	String SearchKey(@RequestParam(name = "type", required = false, defaultValue = "there") String type,
					 @RequestParam(name = "keyword", required = false, defaultValue = "there") String keyword) {

		switch (type){
			case "word":
				break;
			case "image":
				break;
			default:
		}

		return type + keyword;
	}

	public void SearchWord(String keyword){
		for(int i=0; i < wordList.size(); i++){
			if(wordList.contains(keyword)){
				wordList.get(keyword);
			}
		}
	}

	public void SearchImage(String keyword){

	}

	public void load(String urlString){
		URLPool.pop();
		WebURL = urlString;
		byte[] buffer = new byte[1024];
		String content = new String();
		List<String> uniqueContent = new ArrayList<>() ;
		List<String> urls = new ArrayList<>() ;
		List<image> imgs = new ArrayList<>() ;

		try {

			MyParserCallback callback = new MyParserCallback();

			uniqueContent = getUniqueWords(loadPlainText(urlString,callback));
			for(String word : uniqueContent){
				if(BlackListWords.contains(word)){

				}else if(!wordList.contains(word)){
					wordList.put(word,new LinkedList(new Node(urlString)));
				}else{
					((LinkedList) wordList.get(word)).add(new Node(urlString));
				}
			}
			urls = getURLs(urlString,callback);
			for(String u : urls){
				if(BlackListUrls.contains(u)){

				}else if(URLPool.size() >= 10){
					break;
				}else{
					if(!URLPool.contains(u)){
						URLPool.push(u);
					}
				}
			}
			imgs = getimgs(urlString,callback);
			for(image i : imgs){

				String file = i.src.substring(i.src.lastIndexOf('/')+1);
				if(!imgList.contains(file)){
					imgList.put(file,new imgLinkedList(new imgNode(i)));
				}else{
					((imgLinkedList) imgList.get(i)).add(new imgNode(i));
				}
				if(!i.alt.isEmpty()){
					for(int j=0; j < i.alt.size(); j++){
						if(!imgList.contains(i.alt.get(j))){
							imgList.put(i.alt.get(j),new imgLinkedList(new imgNode(i)));
						}else{
							((imgLinkedList) imgList.get(i)).add(new imgNode(i));
						}
					}
				}

			}

			if(ProcessedPool.size() <= 5){
				ProcessedPool.add(urlString);
			}

		} catch (IOException e) {
			e.printStackTrace();
			content = "<h1>Unable to download the page</h1>" + urlString;

		}


	}


	class MyParserCallback extends HTMLEditorKit.ParserCallback {


		public String content = new String();
		public List<String> urls = new ArrayList<String>();
		public List<image> imgs = new ArrayList<image>();

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
						}
					}
				}
			}
		}

		@Override
		public void handleSimpleTag(Tag tag, MutableAttributeSet attrSet, int pos) {

			String u = "";
			List<String> alt = new ArrayList<>() ;

			if (tag.toString().equals("img")) {

				Enumeration e = attrSet.getAttributeNames();

				while (e.hasMoreElements()) {

					Object aname = e.nextElement();

					if (aname.toString().equals("src")) {
						u = (String) attrSet.getAttribute(aname);
					}
					if(aname.toString().equals("alt")){
						alt = Arrays.asList (((String) attrSet.getAttribute(aname)).split(" "));
					}

					}
				if(imgs.size() == 0){
					imgs.add(new image(u,WebURL,alt));

				}else{
					for(int i = 0; i < imgs.size(); i++){

						if(imgs.get(i).src.equals(u)){
							break;
						}else if(!imgs.get(i).src.equals(u) && i == imgs.size() - 1){
							imgs.add(new image(u,WebURL,alt));
						}
					}
				}


				}
			}
		}


	String loadPlainText(String urlString, MyParserCallback callback) throws IOException {
		//MyParserCallback callback = new MyParserCallback();
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

	List<String> getURLs(String srcPage,MyParserCallback callback) throws IOException {
		URL url = new URL(srcPage);
		InputStreamReader reader = new InputStreamReader(url.openStream());

		ParserDelegator parser = new ParserDelegator();
		//MyParserCallback callback = new MyParserCallback();
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

	List<image> getimgs(String srcPage, MyParserCallback callback) throws IOException {
		URL url = new URL(srcPage);
		InputStreamReader reader = new InputStreamReader(url.openStream());

		ParserDelegator parser = new ParserDelegator();
		//MyParserCallback callback = new MyParserCallback();
		parser.parse(reader, callback, true);

		return callback.imgs;

	}


}

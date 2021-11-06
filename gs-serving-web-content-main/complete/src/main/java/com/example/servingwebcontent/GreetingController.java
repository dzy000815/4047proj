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
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Controller
public class GreetingController {

	int X = 5;
	int Y = 3;
	Stack<String> URLPool = new Stack<>();
	List<String> ProcessedPool = new ArrayList<>();
	public Hashtable<String,LinkedList> wordList = new Hashtable<>();
	public Hashtable<String,imgLinkedList> imgList = new Hashtable<>();
	public String WebURL;
	public String WebTitle=" ";
	public List<String> BlackListUrls = new ArrayList<>();
	public List<String> BlackListWords = new ArrayList<>();
	List<Word> WordResult = new ArrayList<>();
	List<image> ImageResult = new ArrayList<>();

	//Gathering information
	List<String> ImageSrc = new ArrayList<>();
	List<String> ImageUrl = new ArrayList<>();
	@RequestMapping(value = "/load",method = RequestMethod.GET)
	public String loadWebPage(@RequestParam(name = "query", required = false, defaultValue = "there")
							   String urlString, Model model) throws ServletException, IOException{

		//Read the blacklist files
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

			//Generate lists for banned urls and words
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

		//Judge whether the input is involved in the blacklist
		for(String url:BlackListUrls){
			if(url.endsWith("*")){
				if(urlString.startsWith(url.substring(0,url.lastIndexOf('*')))){
					return "BlackSeed";
				}
			}else if(url.equals(urlString)){
				return "BlackSeed";
			}

		}

		//Judge whether the input is a valid url with protocol
		try{
			URL url_test = new URL(urlString);
			URLPool.push(urlString); //If yes, push the url into the URLPool stack
			load(urlString); //call the function to gather information of the web page
		}catch (IOException e){
			return "BlackSeed"; //If not, return to the error page to input the url again
		}

        //If the processed url pool is not full or the URL Pool is not empty, the program continues
		while(ProcessedPool.size() < Y && !URLPool.empty()){
			load(URLPool.peek());
		}

		//Output the scanning result to txt files
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
				LinkedList linkedList = wordList.get(key);
				Node n = linkedList.head;
				out.write(n.word.title);
				out.write(" ");
				out.write(n.word.url);
				out.write(", ");
				while(linkedList.hasNext(n)){
					n = n.next;
					out.write(n.word.title);
					out.write(" ");
					out.write(n.word.url);
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
				imgLinkedList linkedList = imgList.get(key);
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


	//Serving request
	@RequestMapping(value = "/SearchKey", method = RequestMethod.GET)
	String SearchKey(@RequestParam(name = "type", required = false, defaultValue = "there") String type,
					 @RequestParam(name = "keyword", required = false, defaultValue = "there") String keyword, Model model) {

		String[] key;
		//Divide the search types to keyword search and image search
		switch (type){
			//If user choose to do keyword search
			case "word":
				key = keyword.split(" ");
				WordResult = SearchWord(key[0]); //Search with the first word in the input

				//If there are multiple words, case by case discussion
				for(int i=1; i < key.length; i++){
					List<Word> result = new ArrayList<>();
					//If there is "OR" in the input
					if(key[i].equals("OR")){
						List<Word> removeList = new ArrayList<>();
						result = SearchWord(key[++i]);//Search with the word following "OR"
						System.out.println(result);
						//Get the union set of results from two words centered by "OR"
						for(Word word1:result){
							for(Word word2:WordResult){
								if(word2.url.equals(word1.url)){
									removeList.add(word1);
								}
							}
						}
						for(Word wordRemove:removeList) {
							result.remove(wordRemove);
						}
						//Add the new result to the list
						WordResult.addAll(result);

					//If there is "-" in the input
					}else if(key[i].equals("-")){
						result = SearchWord(key[++i]); //Search with the world following the "-"
						//Delete the items in the list which equal to the new result
						for(Word word1:result){
							WordResult.removeIf(word2 -> word2.equals(word1));
						}

					//If there is other content except for "OR" or "-" in the input
					}else{
						List<Word> andList = new ArrayList<>();
						result = SearchWord(key[i]);//Search with the word

						//Get the intersection set between the list and the new result
						for(Word word1:result){
							for(Word word2:WordResult){
								if(word2.url.equals(word1.url)){
									andList.add(word1);
								}
							}
						}

						//Replace the list with the new intersection set
						WordResult = andList;
					}
				}

				if(WordResult==null){
					return "NoResult";
				}else{
					model.addAttribute("WordResult",WordResult);
					return "WordResult";
				}


			//If user choose to do image search
			case "image":
				key = keyword.split(" ");//Split the input by " "
				ImageResult = SearchImage(key[0]);//Search with the first word in the input

				//If there are multiple words, case by case discussion
				for(int i=1; i < key.length; i++){
					List<image> result = new ArrayList<>();

					//If there is "OR" in the input
					if(key[i].equals("OR")){
						List<image> removeList = new ArrayList<>();
						result = SearchImage(key[++i]);//Search with the world following the "OR", and i plus 1

						//Get the union set of results from two words centered by "OR"
						for(image img1:result){
							for(image img2:ImageResult){
								if(img2.url.equals(img1.url)){
									removeList.add(img1);
								}
							}
						}
						for(image imgRemove:removeList){
							result.remove(imgRemove);
						}
						//Add the new result to the list
						ImageResult.addAll(result);

					//If there is "-" in the input
					}else if(key[i].equals("-")){
						result = SearchImage(key[++i]);//Search with the world following the "-", and i plus 1
						//Delete the items in the list which equal to the new result
						for(image img1:result){
							ImageResult.removeIf(img2 -> img2.url.equals(img1.url));
						}

					//If there is other content except for "OR" or "-" in the input
					}else{
						List<image> andList = new ArrayList<>();
						result = SearchImage(key[i]);//Search with the word

						//Get the intersection set between the list and the new result
						for(image img1:result){
							for(image img2:ImageResult){
								if(img2.url.equals(img1.url)){
									andList.add(img1);
								}
							}
						}
						//Replace the list with the new intersection set
						ImageResult = andList;
					}
				}


				if(ImageResult==null){
					return "NoResult";
				}else{
					for(image img:ImageResult){
						img.src = img.url + "/" + img.src;
					}
					model.addAttribute("ImageResult",ImageResult);
					return "ImageResult";
				}

			default:
		}



		//model.addAttribute("result",WordResult);
		return "WordResult";
	}
	//Search for urls with one word
	public List SearchWord(String keyword){

		List<Word> result = new ArrayList<>();
       try{
		   Node n = (wordList.get(keyword)).head;
		   result.add(n.word);
		   while((wordList.get(keyword)).hasNext(n)){
			   n = n.next;
			   result.add(n.word);
		   }
		   return result;
	   }catch(Exception e){
		   return null;
	   }

	}

	//Search for image objects with one word
	public List SearchImage(String keyword){
		List<image> result = new ArrayList<>();

		try{
			imgNode n = (imgList.get(keyword)).head;
			result.add(n.img);
			while((imgList.get(keyword)).hasNext(n)){
				n = n.next;
				result.add(n.img);
			}
			return result;
		}catch (Exception e){
			return null;
		}

	}


	//The function to gather information of a website
	public void load(String urlString) throws IOException {
		URLPool.pop();//pop out the url being processed

		WebURL = urlString;
		byte[] buffer = new byte[1024];
		String content = new String();
		URL url1 = new URL(urlString);
		InputStream in = url1.openStream();
		int len;

		while((len = in.read(buffer)) != -1)
			content += new String(buffer);

		List<String> uniqueContent = new ArrayList<>() ;//Store the keyword
		List<String> urls = new ArrayList<>() ;//Store the urls
		List<image> imgs = new ArrayList<>() ;//Store the images

		ParserDelegator parser = new ParserDelegator();
		MyParserCallback callback = new MyParserCallback();
		try {
			//Get the unique keywords in the website being processed
			String pageContent=loadPlainText(urlString,parser,callback);
			Pattern pa = Pattern.compile("<title>.*?</title>",Pattern.CANON_EQ);
			Matcher ma = pa.matcher(content);
			while (ma.find()) {
				WebTitle = ma.group().substring(ma.group().indexOf('>')+1,ma.group().lastIndexOf('<'));
			}
			uniqueContent = getUniqueWords(pageContent);
			//If the word is in blacklist or already in the list, the word cannot be added to the list
			for(String word : uniqueContent){
				if(BlackListWords.contains(word)){

				}else if(!wordList.containsKey(word)){

					wordList.put(word,new LinkedList(new Node(new Word(WebTitle,urlString))));
				}else{
					wordList.get(word).add(new Node(new Word(WebTitle,urlString)));
				}
			}

			//Get the urls in the website being processed
			urls = getURLs(urlString,parser,callback);
			for(String u : urls){
				boolean add= true;
				//Give a boolean to the url to judge whether it is in the blacklist
				for(String url:BlackListUrls){
					if(url.endsWith("*")){
						if(u.startsWith(url.substring(0,url.lastIndexOf('*')))){
							add = false;
						}
					}else if(url.equals(u)){
						add = false;
					}
				}

				//If the url already in the URLPool or ProcessedPool or equals to the url being processed,
				//it cannot be added to the list
				if(!add){

				}else if(URLPool.size() >= X){
					break;
				}else{
					if(!URLPool.contains(u) && !u.equals(urlString) && !ProcessedPool.contains(u)){
						URLPool.push(u);
					}
				}
			}

			//Get the images in the website being processed
			imgs = getimgs(urlString,parser,callback);
			for(image i : imgs){

				String file = i.src.substring(i.src.lastIndexOf('/')+1);
				//If the filename and alts of the image are not already in the list,
				//the keywords of them will be added to the list separately
				if(!imgList.contains(file)){
					imgList.put(file,new imgLinkedList(new imgNode(i)));
				}else{
					(imgList.get(i)).add(new imgNode(i));
				}
				if(!i.alt.isEmpty()){
					for(int j=0; j < i.alt.size(); j++){
						if(!imgList.contains(i.alt.get(j))){
							imgList.put(i.alt.get(j),new imgLinkedList(new imgNode(i)));
						}else{
							(imgList.get(i)).add(new imgNode(i));
						}
					}
				}

			}

			//If the ProcessedPool is not full, the url will be added to the ProcessedPool list
			if(ProcessedPool.size() <= Y){
				ProcessedPool.add(urlString);
			}
			System.out.println(URLPool);

		} catch (IOException e) {
			URLPool.pop();
			ProcessedPool.add(urlString);
			e.printStackTrace();
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
						if (URLPool.size() < 11 && !urls.contains(u) && !URLPool.contains(u) ){
							urls.add(u);
						}
					}
				}
			}
		}

		//Handel the front tag in the html file
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


	//Get the text content in the website
	String loadPlainText(String urlString, ParserDelegator parser,MyParserCallback callback) throws IOException {
		//MyParserCallback callback = new MyParserCallback();
		//ParserDelegator parser = new ParserDelegator();

		URL url = new URL(urlString);
		InputStreamReader reader = new InputStreamReader(url.openStream());
		parser.parse(reader, callback, true);

		return callback.content;
	}

	//Get the unique word list of text content
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

	//Get the urls involved in the website
	List<String> getURLs(String srcPage, ParserDelegator parser,MyParserCallback callback) throws IOException {
		URL url = new URL(srcPage);
		InputStreamReader reader = new InputStreamReader(url.openStream());

		//ParserDelegator parser = new ParserDelegator();
		//MyParserCallback callback = new MyParserCallback();
		parser.parse(reader, callback, true);

		for (int i=0; i<callback.urls.size(); i++) {
			String str = callback.urls.get(i);
			if (!isAbsURL(str)){
				callback.urls.set(i, toAbsURL(str, url).toString());
			}

		}

		return callback.urls;

	}

	//Judge whether the URL is valid with protocol
	boolean isAbsURL(String str) {
		return str.matches("^[a-z0-9]+://.+");
	}

	//Add protocol to not absolute URL
	URL toAbsURL(String str, URL ref) throws MalformedURLException {
		URL url = null;

		String prefix = ref.getProtocol() + "://" + ref.getHost();

		if (ref.getPort() > -1)
			prefix += ":" + ref.getPort();

		if (!str.startsWith("/") && !str.startsWith("?") && !str.startsWith("#")) {
			int len = ref.getPath().length() - ref.getFile().length();
			String tmp = "/" + ref.getPath().substring(0, len) + "/";
			prefix += tmp.replace("//", "/");
		}
		url = new URL(prefix + str);

		return url;
	}

	//Get images involved in the website
	List<image> getimgs(String srcPage, ParserDelegator parser,MyParserCallback callback) throws IOException {
		URL url = new URL(srcPage);
		InputStreamReader reader = new InputStreamReader(url.openStream());

		//ParserDelegator parser = new ParserDelegator();
		//MyParserCallback callback = new MyParserCallback();
		parser.parse(reader, callback, true);

		return callback.imgs;

	}


}

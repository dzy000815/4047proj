package com.example.servingwebcontent;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

@Controller
public class GreetingController {

	@GetMapping("load")
	@ResponseBody
	String load(HttpServletRequest request) {
		return String.format("You are browsing %s with %s!",
				request.getRequestURI(), request.getQueryString());
	}
	@GetMapping("s")
	@ResponseBody
	String s(HttpServletRequest request) {
		return String.format("You are browsing %s with %s!",
				request.getRequestURI(), request.getQueryString());
	}


}

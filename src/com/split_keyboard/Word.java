package com.split_keyboard;

import com.split_keyboard.Word;

public class Word {
	String str;
	String str_show;
	double value;
	
	public Word(String str, String str_show, double value) {
		this.str = str;
		this.str_show = str_show;
		this.value = value;
	}
	
	public Word(String str, double value) {
		this(str, "", value);
	}
}
package com.split_keyboard;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

import com.split_keyboard.R;

import android.app.Activity;
import android.graphics.Color;
import android.opengl.Visibility;
import android.os.Bundle;
import android.os.Environment;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

	Random random = new Random();
			
	Button startButton, stopButton;
	TextView stateView, textView, candidateView, candidateViewL, candidateViewR;
	ImageView leftEyesfree, rightEyesfree, leftAddition, rightAddition;
	CheckBox oovCheck, checkCheck, wysiwygCheck, tapOnlyCheck;
	
	boolean started = false;
	boolean addition_keyboard = false;
	boolean oov_insert = false;
	boolean check = false;
	boolean show_wysiwyg = false;
	boolean tap_only = false;
	
	final int MAX_SENTENCE = 40;
	int sentenceID = 0;
	String sentence = "";
	String inputted = "";
	String sentenceColored = "";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		textView = (TextView)findViewById(R.id.text);
		candidateView = (TextView)findViewById(R.id.candidate);
		candidateViewL = (TextView)findViewById(R.id.candidateL);
		candidateViewR = (TextView)findViewById(R.id.candidateR);
		leftEyesfree = (ImageView)findViewById(R.id.leftkeys_eyesfree);
		rightEyesfree = (ImageView)findViewById(R.id.rightkeys_eyesfree);
		leftAddition = (ImageView)findViewById(R.id.addition_keyboard_left);
		rightAddition = (ImageView)findViewById(R.id.addition_keyboard_right);

		stateView = (TextView)findViewById(R.id.state);
		stateView.setTextColor(Color.GRAY);
		startButton = (Button)findViewById(R.id.startbutton);
		stopButton = (Button)findViewById(R.id.stopbutton);
		startButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v){
				if (started == true) return;
				started = true;
				stateView.setText("STARTED");
				stateView.setTextColor(Color.GREEN);
				sentenceID = 1;
				logStart();
				generateSentence();
				renewCandidate();
				renewCandidateLR();
				renewText();
				oovCheck.setClickable(false);
				checkCheck.setClickable(false);
				wysiwygCheck.setClickable(false);
				tapOnlyCheck.setClickable(false);
			}
		});
		stopButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v){
				if (started == false) return;
				started = false;
				stateView.setText("UNSTARTED");
				stateView.setTextColor(Color.GRAY);
				plist.clear();
				wysiwyg = "";
				wlist.clear();
				addition_keyboard = false;
				onChangeKeyboard();
				renewCandidate();
				renewText();
				logStop();
				oovCheck.setClickable(true);
				checkCheck.setClickable(true);
				wysiwygCheck.setClickable(true);
				tapOnlyCheck.setClickable(true);
			}
		});
		
		oovCheck = (CheckBox)findViewById(R.id.oov);
		oovCheck.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (started == true) return;
				oov_insert = isChecked;
			}
		});

		checkCheck = (CheckBox)findViewById(R.id.check);
		checkCheck.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (started == true) return;
				check = isChecked;
			}
		});
		
		wysiwygCheck = (CheckBox)findViewById(R.id.wyswyg);
		wysiwygCheck.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (started == true) return;
				show_wysiwyg = isChecked;
			}
		});
		
		tapOnlyCheck = (CheckBox)findViewById(R.id.taponly);
		tapOnlyCheck.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				tap_only = isChecked;
			}
		});
		
		load();
		centroid();
		centroid_addition();
	}
	
	void onChangeKeyboard() {
		if (addition_keyboard) {
			leftAddition.setVisibility(View.VISIBLE);
			rightAddition.setVisibility(View.VISIBLE);
			leftEyesfree.setVisibility(View.INVISIBLE);
			rightEyesfree.setVisibility(View.INVISIBLE);
		} else {
			leftEyesfree.setVisibility(View.VISIBLE);
			rightEyesfree.setVisibility(View.VISIBLE);
			leftAddition.setVisibility(View.INVISIBLE);
			rightAddition.setVisibility(View.INVISIBLE);
		}
	}
	
	
	
	final int CANDIDATE_SIZE = 5;
	final int CANDIDATE_LR_SPAN = 6;
	ArrayList<String> wlist = new ArrayList<String>();
	ArrayList<Point> plist = new ArrayList<Point>();
	ArrayList<Word> candidates = new ArrayList<Word>();
	ArrayList<Word> candidatesLR = new ArrayList<Word>();
	int selected = 0;
	String wysiwyg = "";
	
	int getWlistLength() {
		int len = 0;
		for (int i = 0; i < wlist.size(); i++) {
			len += wlist.get(i).length();
			if (!oov_insert && i < wlist.size() - 1) len++;
		}
		return len;
	}
	
	String filterUppercase(String s) {
		String t = "";
		int j = 0;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c < 'a' || c > 'z') {
				t += c;
				continue;
			}
			t += (plist.get(j).uppercase) ? (char)(c - 'a' + 'A') : c;
			j++;
		}
		return t;
	}
	
	ArrayList<Word> getCandidates(BivariateGaussian[][] model) {
		ArrayList<Word> candidates = new ArrayList<Word>();
		candidates.clear();
		int n = plist.size();
		if (n > 0) {
			for (int i = 0; i < dict[n].size(); i++) {
				Word word = dict[n].get(i);
				String str = word.str;
				double p = word.value;
				for (int j = 0; j < n; j++) {
					Point q = plist.get(j);
					p *= model[q.hand][(int)(str.charAt(j) - 'a')].probability(q.x, q.y);
				}
				candidates.add(new Word(filterUppercase(word.str_show), p));
				int j = candidates.size() - 1;
				while (j > 0 && candidates.get(j).value > candidates.get(j - 1).value) {
					Word w0 = candidates.get(j);
					Word w1 = candidates.get(j - 1);
					String tmpstr = w0.str;
					w0.str = w1.str;
					w1.str = tmpstr;
					double tmpvalue = w0.value;
					w0.value = w1.value;
					w1.value = tmpvalue;
					j--;
				}
				if (candidates.size() > CANDIDATE_SIZE) {
					candidates.remove(candidates.size() - 1);
				}
			}
			if (candidates.size() == 0) candidates.add(new Word(filterUppercase(wysiwyg), 0));
		}
		return candidates;
	}
	
 	void renewText() {
		String text = "sentence: " + sentenceID + "/" + MAX_SENTENCE + "<br/><br/>";
		text += sentenceColored + "<br/><br/>";
		for (int i = 0; i < wlist.size(); i++) {
			String s = wlist.get(i);
			switch (s.charAt(0)) {
			case '<':
				s = "&lt;";
				break;
			case '>':
				s = "&gt;";
				break;
			case '\"':
				s = "&quot;";
				break;
			case ' ':
				s = "&nbsp;";
				break;
			case '&':
				s = "&amp;";
				break;
			}
			text += s;
			if (!oov_insert) text += " ";
		}
		if (plist.size() > 0) {
			text += candidates.get(selected).str;
		}
		text += "<font color='#aaaaaa'>_</font>";
		textView.setText(Html.fromHtml(text));
	}
	
	void renewCandidate() {
		candidates = getCandidates(modelEyesfree);
		String text = "";
		for (int i = 0; i < candidates.size(); i++) {
			if (i == selected) text += "<font color='#ff0000'>";
			text += candidates.get(i).str + "  ";
			if (i == selected) text += "</font>";
		}
		candidateView.setText(Html.fromHtml(text));
		int len = getWlistLength();
		if (wlist.size() > 0 && !oov_insert) len++;
		candidateView.setX(650 + len * 30);
	}

	void renewCandidateLR() {
		String sL = "", sR = "";
		if (show_wysiwyg) {
			sL += wysiwyg;
			sR += wysiwyg;
		} else {
			candidatesLR = getCandidates(modelEyesfocus);
			for (int i = 0; i < candidatesLR.size(); i++) {
				sL += candidatesLR.get(i).str + "&nbsp;";
				for (int j = 0; j < Math.max(0, CANDIDATE_LR_SPAN - plist.size()); j++) sL += "&nbsp;";
			}
			for (int i = 0; i < candidatesLR.size(); i++) {
				sR = "&nbsp;" + candidatesLR.get(i).str + sR;
				for (int j = 0; j < Math.max(0, CANDIDATE_LR_SPAN - plist.size()); j++) sR = "&nbsp;" + sR;
			}
			Log.d("sr", sR);
		}
		candidateViewL.setText(Html.fromHtml("<font color='#e7e8e9'>" + sL + "</font>"));
		candidateViewR.setText(Html.fromHtml("<font color='#e7e8e9'>" + sR + "</font>"));
	}
	
	void confirmSelection(ArrayList<Word> candidates, int selected) {
		if (selected == -1) {
			if (wysiwyg.length() > 0) {
				log("select " + selected + " " + wysiwyg);
				wlist.add(wysiwyg);
				plist.clear();
				wysiwyg = "";
			}
		} else {
			if (plist.size() > 0) {
				String selectedStr = candidates.get(selected).str;
				log("select " + selected + " " + selectedStr);
				wlist.add(selectedStr);
				plist.clear();
				wysiwyg = "";
			}
		}
		this.selected = 0;
	}

	void generateSentence() {
		sentence = sentences.get(random.nextInt(sentences.size()));
		sentenceColored = sentence;
		if (oov_insert) {
			/*String[] arr = sentence.split(" ");
			sentence = sentenceColored = "";
			String oov = dict_oov.get(random.nextInt(dict_oov.size())).str;
			int oov_index = random.nextInt(arr.length + 1);
			for (int i = 0; i < oov_index; i++) {
				sentence += arr[i] + " ";
				sentenceColored += arr[i] + " ";
			}
			sentence += oov;
			sentenceColored += "<font color='#ff0000'>" + oov + "</font>";
			for (int i = oov_index; i < arr.length; i++) {
				sentence += " " + arr[i];
				sentenceColored += " " + arr[i];
			}*/
			if (sentenceID > oov_sentences.size()) {
				stopButton.performClick();
				return;
			}
			sentence = oov_sentences.get(sentenceID - 1);
			sentenceColored = sentence;
		}
		log("sentence " + sentence);
	}
	
	void nextSentence() {
		sentenceID++;
		generateSentence();
		if (sentenceID > MAX_SENTENCE) {
			stopButton.performClick();
		}
		plist.clear();
		wysiwyg = "";
		wlist.clear();
	}
	
	void click(int x, int y, boolean rub) {
		//Log.d("xy", x + " " + y);
		double bestDist = 1e20;
		int best = -1;
		Point[] pos = addition_keyboard ? posAddition : posEyesfree;
		for (int i = 0; i < pos.length; i++) {
			double dist = Math.pow(x - pos[i].x, 2) + Math.pow(y - pos[i].y, 2);
			if (bestDist > dist) {
				bestDist = dist;
				best = i;
			}
		}

		if (addition_keyboard) {
			switch (best) {
			case 34:
				swipeLeft();
				break;
			case 38:
			case 39:
				break;
			default:
				wlist.add("" + charAddition.charAt(best));
				break;
			}
		} else {
			if (y < 1120) {
				if (show_wysiwyg) {
					log("tap");
					confirmSelection(null, -1);
				} else {
					int span = (x < 1280) ? x : (2560 - x);
					int q = span / (Math.max(plist.size(), CANDIDATE_LR_SPAN) + 1) / 18;
					if (q >= 0 && q < candidatesLR.size()) {
						log("tap " + q + " " + (x < 1280 ? "L" : "R"));
						confirmSelection(candidatesLR, q);
					}
				}
			} else {
				log("click " + x + " " + y);
				plist.add(new Point(x, y, rub));
				switch (best) {
				case 26:
					wysiwyg += ',';
					break;
				case 27:
					wysiwyg += '.';
					break;
				default:
					wysiwyg += (plist.get(plist.size() - 1).uppercase) ? (char)(best + 'A') : (char)(best + 'a');
					break;
				}
			}
		}

		renewCandidate();
		renewCandidateLR();
		renewText();
	}
	
	void swipeLeft() {
		if (plist.size() > 0) {
			log("swipeleft");
			plist.remove(plist.size() - 1);
			wysiwyg = wysiwyg.substring(0, wysiwyg.length() - 1);
			renewCandidate();
			renewCandidateLR();
		} else if (wlist.size() > 0) {
			log("swipeleft");
			wlist.remove(wlist.size() - 1);
		}
		renewText();
	}
	
	void swipeDown() {
		if (addition_keyboard) {
			addition_keyboard = false;
			onChangeKeyboard();
		} else {
			log("swipedown");
			plist.clear();
			wysiwyg = "";
			renewCandidate();
			renewCandidateLR();
			renewText();
		}
	}
	
	void swipeRight() {
		if (plist.size() == 0) {
			if (oov_insert) {
				if (getWlistLength() == sentence.length()) {
					log("nextsentence");
					nextSentence();
				} else {
					wlist.add(" ");
				}
			} else {
				if (!check || getWlistLength() == sentence.length()) {
					log("nextsentence");
					nextSentence();
				}
			}
		} else {
			if (!tap_only) {
				log("swiperight");
				confirmSelection(candidates, 0);
				renewCandidate();
				renewCandidateLR();
			}
		}
		renewText();
	}

	void swipeUp() {
		if (plist.size() == 0) {
			addition_keyboard = !addition_keyboard;
			onChangeKeyboard();
		}
	}
	
	void drag(int x, int y, int downX, int downY) {
		if (tap_only) return;
		double span = (2550 - downX) / 5.0;
		span = Math.min(span, 50);
		double q = (x - 20 - downX) / span;
		q = Math.max(q, 0);
		q = Math.min(q, 4);
		selected = (int)Math.round(q);
		renewCandidate();
	}
	
	void dragFinish() {
		if (tap_only) return;
		log("drag");
		confirmSelection(candidates, selected);
		renewCandidate();
		renewCandidateLR();
		renewText();
	}
	

	
	
	
	TouchEvent[] touchEvent = new TouchEvent[10];
	
	public boolean onTouchEvent(MotionEvent event){
		if (started == false) return super.onTouchEvent(event);
		int n = event.getPointerCount();
		int index = event.getActionIndex();
		int pointerID = event.getPointerId(index);
		int x = (int)event.getX(index);
		int y = (int)event.getY(index);
		//Log.d("pressure", event.getPressure(index) + "");
		
		switch (event.getActionMasked()){
		case MotionEvent.ACTION_DOWN:
		case MotionEvent.ACTION_POINTER_DOWN:
			log("down");
			touchEvent[pointerID] = new TouchEvent(x, y);
			break;
			
		case MotionEvent.ACTION_MOVE:
			for (int i = 0; i < n; i++) {
				int j = event.getPointerId(i);
				int xx = (int)event.getX(i);
				int yy = (int)event.getY(i);
				if (touchEvent[j].anyMove(xx, yy)) {
					drag(xx, yy, touchEvent[j].downX, touchEvent[j].downY);
				}
				//drag(xx, yy, touchEvent[j].downX, touchEvent[j].downY);
			}
			break;
			
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_POINTER_UP:
			log("up");
			TouchEvent q = touchEvent[pointerID];
			int op = q.up(x, y);
			switch (op) {
			case TouchEvent.EVENT_CLICK:
				click((q.x + q.downX) / 2, (q.y + q.downY) / 2, false);
				break;
			case TouchEvent.EVENT_RUB:
				click((q.x + q.downX) / 2, (q.y + q.downY) / 2, true);
				break;
			case TouchEvent.EVENT_SWIPE_LEFT:
				if (n == 1) swipeLeft();
				break;
			case TouchEvent.EVENT_SWIPE_RIGHT:
				if (n == 1) swipeRight();
				break;
			case TouchEvent.EVENT_SWIPE_DOWN:
				if (n == 1) swipeDown();
				break;
			case TouchEvent.EVENT_SWIPE_UP:
				if (n == 1) swipeUp();
				break;
			case TouchEvent.EVENT_DRAG:
				if (n == 1) dragFinish();
				break;
			}
			touchEvent[pointerID] = null;
			break;
		}
		return super.onTouchEvent(event);
	}

	
	
	
	
	final int DICT = R.raw.dict;
	final int MAX_WORD_LENGTH = 99;
	final int DICT_SIZE = (DICT == R.raw.dict) ? 10003 : 50000;
	//final int OOV_SIZE = 1000;
	ArrayList<String> sentences = new ArrayList<String>();
	ArrayList<Word>[] dict = new ArrayList[MAX_WORD_LENGTH];
	//ArrayList<Word> dict_oov = new ArrayList<Word>();
	ArrayList<String> oov_sentences = new ArrayList<String>();
	BivariateGaussian[][] modelEyesfree = new BivariateGaussian[2][26];
	BivariateGaussian[][] modelEyesfocus = new BivariateGaussian[2][26];
	
	PrintWriter logger;
	
	void load() {
		BufferedReader reader = new BufferedReader(new InputStreamReader(getResources().openRawResource(R.raw.sentences)));
		String line;
		try {
			while ((line = reader.readLine()) != null) {
				sentences.add(line.toLowerCase());
			}
			reader.close();
			Log.d("load", "finish read sentences");
		} catch (Exception e) {
			Log.d("error", "read sentences");
		}
		
		for (int i = 0; i < MAX_WORD_LENGTH; i++) {
			dict[i] = new ArrayList<Word>();
		}
		reader = new BufferedReader(new InputStreamReader(getResources().openRawResource(DICT)));
		try {
			int lineNo = 0;
			while ((line = reader.readLine()) != null) {
				lineNo++;
				String[] arr = line.split(" ");
				String str = arr[0];
				double freq = Double.parseDouble(arr[1]);
				if (str.length() >= MAX_WORD_LENGTH) continue;
				if (lineNo <= DICT_SIZE) {
					String s = "";
					for (int i = 0; i < str.length(); i++) {
						char c = str.charAt(i);
						if (c >= 'a' && c <= 'z') s += c;
					}
					dict[s.length()].add(new Word(s, str, freq));
				//} else if (lineNo <= DICT_SIZE + OOV_SIZE) {
				//	dict_oov.add(new Word(str, freq));
				} else {
					break;
				}
			}
			reader.close();
			Log.d("load", "finish read dictionary");
		} catch (Exception e) {
			Log.d("error", "read dictionary");
		}
		
		reader = new BufferedReader(new InputStreamReader(getResources().openRawResource(R.raw.model)));
		try {
			int lineNo = 0;
			while ((line = reader.readLine()) != null) {
				lineNo++;
				if (lineNo <= 1) continue;
				String[] arr = line.split(",");
				int mode = Integer.parseInt(arr[0]);
				int hand = Integer.parseInt(arr[1]);
				int index = Integer.parseInt(arr[2]);
				if (mode == 2) {
					modelEyesfocus[hand][index] = new BivariateGaussian(Double.parseDouble(arr[3]), Double.parseDouble(arr[4]), Double.parseDouble(arr[5]), Double.parseDouble(arr[6]));
				} else if (mode == 1) {
					modelEyesfree[hand][index] = new BivariateGaussian(Double.parseDouble(arr[3]), Double.parseDouble(arr[4]), Double.parseDouble(arr[5]), Double.parseDouble(arr[6]));
				}
			}
			reader.close();
			Log.d("load", "finish read model");
		} catch (Exception e) {
			Log.d("error", "read model");
		}
		
		reader = new BufferedReader(new InputStreamReader(getResources().openRawResource(R.raw.chat)));
		try {
			while ((line = reader.readLine()) != null) {
				oov_sentences.add(line.toLowerCase());
			}
			reader.close();
			Log.d("load", "finish read sentences");
		} catch (Exception e) {
			Log.d("error", "read sentences");
		}
	}
	
	void logStart() {
		Calendar calendar = Calendar.getInstance();
		int year = calendar.get(Calendar.YEAR);
		int month = calendar.get(Calendar.MONTH) + 1;
		int day = calendar.get(Calendar.DATE);
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		int minute = calendar.get(Calendar.MINUTE);
		int second = calendar.get(Calendar.SECOND);
		String fileName = "" + year + month + day + hour + minute + second + ".txt";
		try {
			logger = new PrintWriter(new OutputStreamWriter(new FileOutputStream(Environment.getExternalStorageDirectory().getPath() + "/" + fileName, true)), true);
		} catch (Exception e) {
			Log.d("ERROR: open logger", e.toString());
		}
	}
	
	void logStop() {
		logger.close();
	}
	
	void log(String s) {
		s = System.currentTimeMillis() + " " + s;
		logger.println(s);
	}

	
	
	Point[] posEyesfree = new Point[28];
	Point[] posAddition = new Point[50];
	String charAddition = "12345!@*_?+-/\\#~():;'\"   67890^&$% |<>  `{}[],.¡Á¡Â=";
	
	void centroid() {
		RelativeLayout layout = (RelativeLayout)findViewById(R.id.main_layout);
		DrawView drawView = new DrawView(this);
		drawView.setBackgroundColor(Color.BLACK);
		drawView.setAlpha(0.5f);
        //layout.addView(drawView);
		
		final int Y0 = 1250;
		final int[] LX = new int[] {133, 175, 235};
		final int[] RX = new int[] {1453, 1495, 1555};
		final int Y = 130;
		final int X = 104;
		final String[] keyboard = new String[] { "qwertyuiop", "asdfghjkl", "zxcvbnm" };
		final int[] gap = new int[] { 5, 5, 4 };
		
		for (int y = 0; y < 3; y++) {
			for (int x = 0; x < gap[y]; x++) {
				int index = keyboard[y].charAt(x) - 'a';
				posEyesfree[index] = new Point(LX[y] + x * X, Y0 + y * Y);
			}
			for (int x = gap[y]; x < keyboard[y].length(); x++) {
				int index = keyboard[y].charAt(x) - 'a';
				posEyesfree[index] = new Point(RX[y] + x * X, Y0 + y * Y);
			}
		}
		
		// , and .
		posEyesfree[26] = new Point(RX[2] + 7 * X, Y0 + 2 * Y);
		posEyesfree[27] = new Point(RX[2] + 8 * X, Y0 + 2 * Y);
	}
	
	void centroid_addition() {
		final int Y0 = 1067;
		final int LX = 93;
		final int RX = 1530;
		final int Y = 115;
		final int X = 104;
		
		for (int y = 0; y < 5; y++) {
			for (int x = 0; x <  5; x++) posAddition[y * 5 + x     ] = new Point(LX + x * X, Y0 + y * Y);
			for (int x = 5; x < 10; x++) posAddition[y * 5 + x + 20] = new Point(RX + x * X, Y0 + y * Y);
		}
	}
}
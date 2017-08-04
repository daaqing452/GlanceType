package com.split_keyboard;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Random;

import com.split_keyboard.R;

import android.app.Activity;
import android.graphics.Color;
import android.opengl.Visibility;
import android.os.Bundle;
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
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

	Random random = new Random();
			
	Button startButton, stopButton;
	TextView stateView, textView, candidateView, candidateViewL, candidateViewR;
	ImageView leftEyesfree, rightEyesfree, leftEyesfocus, rightEyesfocus;
	
	boolean started = false;
	boolean eyes_free = true;
	boolean oov_insert = false;
	
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
		leftEyesfocus = (ImageView)findViewById(R.id.leftkeys_eyesfocus);
		rightEyesfocus = (ImageView)findViewById(R.id.rightkeys_eyesfocus);

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
				generateSentence();
				renewCandidate();
				renewCandidateLR();
				renewText();
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
				renewCandidate();
				renewText();
			}
		});
		
		RadioGroup techniqueGroup = (RadioGroup)findViewById(R.id.technique);
		techniqueGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				if (started == true) return;
				if (checkedId == R.id.eyes_free) {
					eyes_free = true;
					leftEyesfree.setVisibility(View.VISIBLE);
					rightEyesfree.setVisibility(View.VISIBLE);
					leftEyesfocus.setVisibility(View.INVISIBLE);
					rightEyesfocus.setVisibility(View.INVISIBLE);
					candidateViewL.setY(875);
					candidateViewR.setY(875);
				} else if (checkedId == R.id.eyes_focus) {
					eyes_free = false;
					leftEyesfree.setVisibility(View.INVISIBLE);
					rightEyesfree.setVisibility(View.INVISIBLE);
					leftEyesfocus.setVisibility(View.VISIBLE);
					rightEyesfocus.setVisibility(View.VISIBLE);
					candidateViewL.setY(700);
					candidateViewR.setY(700);
				} else {
					Log.d("error", "radio group");
				}
			}
		});
		
		CheckBox oovCheck = (CheckBox)findViewById(R.id.oov);
		oovCheck.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (started == true) return;
				oov_insert = isChecked;
			}
		});
		
		load();
		centroid();
	}
	
	
	
	
	final int CANDIDATE_SIZE = 5;
	ArrayList<String> wlist = new ArrayList<String>();
	ArrayList<Point> plist = new ArrayList<Point>();
	ArrayList<Word> candidates = new ArrayList<Word>();
	int selected = 0;
	String wysiwyg = "";
	
	void renewText() {
		String text = "sentence: " + sentenceID + "/" + MAX_SENTENCE + "<br/><br/>";
		text += sentenceColored + "<br/><br/>";
		for (int i = 0; i < wlist.size(); i++) text += wlist.get(i) + " ";
		if (plist.size() > 0) {
			text += candidates.get(selected).str;
		} else {
			text += wysiwyg;
		}
		text += "<font color='#aaaaaa'>_</font>";
		textView.setText(Html.fromHtml(text));
	}
	
	void renewCandidate() {
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
				candidates.add(new Word(str, p));
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
			if (candidates.size() == 0) candidates.add(new Word(wysiwyg, 0));
		}
		
		String text = "";
		for (int i = 0; i < candidates.size(); i++) {
			if (i == selected) text += "<font color='#ff0000'>";
			text += candidates.get(i).str + "  ";
			if (i == selected) text += "</font>";
		}
		candidateView.setText(Html.fromHtml(text));
		int cnt = wlist.size();
		for (int i = 0; i < wlist.size(); i++) cnt += wlist.get(i).length();
		candidateView.setX(650 + cnt * 30);
	}

	void renewCandidateLR() {
		if (eyes_free) {
			candidateViewL.setText(wysiwyg);
			candidateViewR.setText(wysiwyg);
		}
	}
	
	void confirmSelection(int selected) {
		if (selected == -1) {
			wlist.add(wysiwyg);
			plist.clear();
			wysiwyg = "";
		} else {
			if (plist.size() > 0) {
				wlist.add(candidates.get(selected).str);
				plist.clear();
				wysiwyg = "";
			}
		}
		selected = 0;
	}

	void generateSentence() {
		sentence = sentences.get(random.nextInt(sentences.size()));
		sentenceColored = sentence;
		if (oov_insert) {
			String[] arr = sentence.split(" ");
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
			}
		}
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
	
	void click(int x, int y) {
		//Log.d("xy", x + " " + y);
		double bestDist = 1e20;
		int best = -1;
		Point[] pos = (eyes_free ? posEyesfree : posEyesfocus);
		for (int i = 0; i < pos.length; i++) {
			double dist = Math.pow(x - pos[i].x, 2) + Math.pow(y - pos[i].y, 2);
			if (bestDist > dist) {
				bestDist = dist;
				best = i;
			}
		}
		
		if (eyes_free) {
			if (y < 1100 && (x < 120 || x > 2300)) {
				confirmSelection(-1);
			} else {
				plist.add(new Point(x, y));
				wysiwyg += (char)(best + 'a');
			}
		} else {
			switch (best) {
			case 26:
			case 27:
			case 28:
				wysiwyg += " ";
				break;
			case 29:
				if (wysiwyg.length() > 0) wysiwyg = wysiwyg.substring(0, wysiwyg.length() - 1);
				break;
			case 30:
				if (wysiwyg.length() == sentence.length()) nextSentence();
				break;
			default:
				wysiwyg += (char)(best + 'a');
				break;
			}
		}
		renewCandidate();
		renewCandidateLR();
		renewText();
	}
	
	void swipeLeft() {
		if (!eyes_free) return;
		if (plist.size() > 0) {
			plist.remove(plist.size() - 1);
			wysiwyg = wysiwyg.substring(0, wysiwyg.length() - 1);
			renewCandidate();
			renewCandidateLR();
		} else if (wlist.size() > 0) {
			wlist.remove(wlist.size() - 1);
		}
		renewText();
	}

	void swipeDown() {
		if (!eyes_free) return;
		plist.clear();
		wysiwyg = "";
		renewCandidate();
		renewCandidateLR();
		renewText();
	}
	
	void swipeRight() {
		if (!eyes_free) return;
		if (plist.size() == 0) {
			int totalLength = wlist.size() - 1;
			for (int i = 0; i < wlist.size(); i++) totalLength += wlist.get(i).length();
			if (totalLength == sentence.length()) nextSentence();
		} else {
			confirmSelection(0);
			renewCandidate();
			renewCandidateLR();
		}
		renewText();
	}

	void drag(int x, int y, int downX, int downY) {
		if (!eyes_free) return;
		double span = (2550 - downX) / 5.0;
		span = Math.min(span, 50);
		double q = (x - 20 - downX) / span;
		q = Math.max(q, 0);
		q = Math.min(q, 4);
		selected = (int)Math.round(q);
		renewCandidate();
	}
	
	void dragFinish() {
		if (!eyes_free) return;
		confirmSelection(selected);
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
		
		switch (event.getActionMasked()){
		case MotionEvent.ACTION_DOWN:
		case MotionEvent.ACTION_POINTER_DOWN:
			touchEvent[pointerID] = new TouchEvent(x, y);
			break;
			
		case MotionEvent.ACTION_MOVE:
			for (int i = 0; i < n; i++) {
				int j = event.getPointerId(i);
				int xx = (int)event.getX(i);
				int yy = (int)event.getY(i);
				/*if (touchEvent[j].anyMove(xx, yy)) {
					drag(xx, yy, touchEvent[j].downX, touchEvent[j].downY);
				}*/
				drag(xx, yy, touchEvent[j].downX, touchEvent[j].downY);
			}
			break;
			
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_POINTER_UP:
			TouchEvent q = touchEvent[pointerID];
			int op = q.up(x, y);
			switch (op) {
			case TouchEvent.EVENT_CLICK:
				click((q.x + q.downX) / 2, (q.y + q.downY) / 2);
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
			case TouchEvent.EVENT_DRAG:
				if (n == 1) dragFinish();
				break;
			}
			touchEvent[pointerID] = null;
			break;
		}
		return super.onTouchEvent(event);
	}

	
	
	
	
	final int MAX_WORD_LENGTH = 99;
	final int DICT_SIZE = 10000;
	ArrayList<String> sentences = new ArrayList<String>();
	ArrayList<Word>[] dict = new ArrayList[MAX_WORD_LENGTH];
	ArrayList<Word> dict_oov = new ArrayList<Word>();
	BivariateGaussian[][] model = new BivariateGaussian[2][26];
	
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
		reader = new BufferedReader(new InputStreamReader(getResources().openRawResource(R.raw.dict)));
		try {
			int lineNo = 0;
			while ((line = reader.readLine()) != null) {
				lineNo++;
				String[] arr = line.split(" ");
				String str = arr[0];
				double freq = Double.parseDouble(arr[1]);
				if (str.length() >= MAX_WORD_LENGTH) continue;
				if (lineNo <= DICT_SIZE) {
					dict[str.length()].add(new Word(str, freq));
				} else {
					dict_oov.add(new Word(str, freq));
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
				if (arr[0].equals("1")) {
					int hand = Integer.parseInt(arr[1]);
					int index = Integer.parseInt(arr[2]);
					model[hand][index] = new BivariateGaussian(Double.parseDouble(arr[3]), Double.parseDouble(arr[4]), Double.parseDouble(arr[5]), Double.parseDouble(arr[6]));
				}
			}
			reader.close();
			Log.d("load", "finish read model");
		} catch (Exception e) {
			Log.d("error", "read model");
		}
	}
	
	
	
	
	
	Point[] posEyesfree = new Point[26];
	Point[] posEyesfocus = new Point[31];
	
	void centroid() {
		RelativeLayout layout = (RelativeLayout)findViewById(R.id.main_layout);
		DrawView drawView = new DrawView(this);
		drawView.setBackgroundColor(Color.BLACK);
		drawView.setAlpha(0.5f);
        //layout.addView(drawView);
		
		final int Y0_Eyesfree = 1250;
		final int Y0_Eyesfocus = 1140;
		final int[] LX = new int[] {133, 175, 235};
		final int[] RX = new int[] {1453, 1495, 1555};
		final int Y = 130;
		final int X = 104;
		final String[] keyboard = new String[] { "qwertyuiop", "asdfghjkl", "zxcvbnm" };
		final int[] gap = new int[] { 5, 5, 4 };
		
		for (int y = 0; y < 3; y++) {
			for (int x = 0; x < gap[y]; x++) {
				int index = keyboard[y].charAt(x) - 'a';
				posEyesfree[index] = new Point(LX[y] + x * X, Y0_Eyesfree + y * Y);
				posEyesfocus[index] = new Point(LX[y] + x * X, Y0_Eyesfocus + y * Y);
			}
			for (int x = gap[y]; x < keyboard[y].length(); x++) {
				int index = keyboard[y].charAt(x) - 'a';
				posEyesfree[index] = new Point(RX[y] + x * X, Y0_Eyesfree + y * Y);
				posEyesfocus[index] = new Point(RX[y] + x * X, Y0_Eyesfocus + y * Y);
			}
		}
		
		// space
		posEyesfocus[26] = new Point(LX[1] + 2 * X, Y0_Eyesfocus + 3 * Y);
		posEyesfocus[27] = new Point(LX[1] + 3 * X, Y0_Eyesfocus + 3 * Y);
		posEyesfocus[28] = new Point(LX[1] + 4 * X, Y0_Eyesfocus + 3 * Y);
		// backspace
		posEyesfocus[29] = new Point(RX[0] + 10 * X, Y0_Eyesfocus + 0 * Y);
		// enter
		posEyesfocus[30] = new Point(RX[1] + 9 * X, Y0_Eyesfocus + 1 * Y);
	}
}
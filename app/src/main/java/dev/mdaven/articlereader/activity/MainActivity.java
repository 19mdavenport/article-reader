package dev.mdaven.articlereader.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Locale;

import dev.mdaven.articlereader.R;
import dev.mdaven.articlereader.fragment.TextFragment;
import dev.mdaven.articlereader.fragment.WebFragment;

public class MainActivity extends AppCompatActivity implements WebFragment.WebListener {
    private static String firstParagraphText;
    private static String lastParagraphText;

    public static void setFirstParagraphText(String text) {
        firstParagraphText = text;
    }

    public static void setLastParagraphText(String text) {
        lastParagraphText = text;
    }

    private LinearLayout loadLayout;
    private LinearLayout readLayout;
    private Button loadContentsButton;
    private Button readButton;
    private Button pauseResumeButton;
    private boolean isPaused;

    private SwitchCompat javascriptSwitch;
    private EditText urlEditText;
    private EditText beginEditText;
    private EditText endEditText;
    private TextToSpeech textToSpeech;
    private String url;
    private ViewPagerAdapter adapter;

    private TabLayout tabLayout;

    private ViewPager2 viewPager;

    private WebFragment webFragment;
    private TextFragment pageFragment;
    private TextFragment articleFragment;

    private String articleContent;
    private String pageContent;
    private Deque<String> paragraphs;
    private String currentParagraph;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        readButton = findViewById(R.id.read_button);
        readButton.setEnabled(false);

        textToSpeech = new TextToSpeech(getApplicationContext(), status -> {
            if(status != TextToSpeech.ERROR) {
                readButton.setEnabled(true);
                textToSpeech.setLanguage(Locale.US);
                textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String s) {
                        if(webFragment != null) webFragment.scrollToText(s);
                    }
                    @Override
                    public void onDone(String s) {
                        readNextParagraph();
                    }
                    @Override
                    public void onError(String s) {}
                });
                loadIntentUrl(getIntent());
            }
            else {
                Toast.makeText(getApplicationContext(),
                        "Error creating/using text to speech. Please close and try again. " +
                                "If this error persists, please contact Michael.",
                        Toast.LENGTH_LONG).show();
            }
        });

        readLayout = findViewById(R.id.linear_layout_read_contents);
        loadLayout = findViewById(R.id.linear_layout_url_load);
        javascriptSwitch = findViewById(R.id.switch_javascript);
        urlEditText = findViewById(R.id.url_edit_text);
        beginEditText = findViewById(R.id.begin_edit_text);
        endEditText = findViewById(R.id.end_edit_text);

        Button loadButton = findViewById(R.id.load_button);
        loadButton.setOnClickListener(v -> loadUrl(urlEditText.getText().toString()));

        loadContentsButton = findViewById(R.id.button_load_content);
        loadContentsButton.setOnClickListener(v -> loadContents());

        readButton.setOnClickListener(v -> {
            hideKeyboard(this, readButton);
            isPaused = false;
            pauseResumeButton.setEnabled(true);
            pauseResumeButton.setText(R.string.pause_button);
            if(pageContent == null) {
                Toast.makeText(getApplicationContext(), "Content still loading", Toast.LENGTH_LONG).show();
                return;
            }
            String content = pageContent;

            String beginStr = beginEditText.getText().toString();
            String endStr = endEditText.getText().toString();
            int startIndex = content.indexOf(beginStr);
            int endIndex = content.lastIndexOf(endStr);
            if(startIndex == -1) {
                Toast.makeText(getApplicationContext(), "Could not find start text in page", Toast.LENGTH_LONG).show();
                return;
            }
            if(endIndex == -1) {
                Toast.makeText(getApplicationContext(), "Could not find end text in page", Toast.LENGTH_LONG).show();
                return;
            }
            if(startIndex > endIndex) {
                Toast.makeText(getApplicationContext(), "Start text is after end text", Toast.LENGTH_LONG).show();
                return;
            }
            content = content.substring(startIndex, endIndex + endStr.length());
            String[] paragraphsArr = content.split("\n");
            paragraphs = new LinkedList<>();
            textToSpeech.stop();
            for(String p : paragraphsArr) {
                if(!p.isBlank()) paragraphs.add(p);
            }
            readNextParagraph();
        });

        pauseResumeButton = findViewById(R.id.pause_button);
        pauseResumeButton.setEnabled(false);
        pauseResumeButton.setOnClickListener(v -> {
            if(isPaused) {
                readNextParagraph();
                pauseResumeButton.setText(R.string.pause_button);
            } else {
                textToSpeech.stop();
                paragraphs.addFirst(currentParagraph);
                pauseResumeButton.setText(R.string.resume_button);
            }
            isPaused = !isPaused;
        });

        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);
    }

    private void loadIntentUrl(Intent intent) {
        if(Intent.ACTION_SEND.equals(intent.getAction()) && intent.getType() != null) {
            loadUrl(intent.getStringExtra(Intent.EXTRA_TEXT));
            urlEditText.setText(url);
        }
    }

    private void readNextParagraph() {
        currentParagraph = paragraphs.poll();
        if(currentParagraph != null) {
            textToSpeech.speak(currentParagraph, TextToSpeech.QUEUE_ADD, null, currentParagraph);
        } else {
            pauseResumeButton.setEnabled(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(firstParagraphText != null) {
            setText(beginEditText, firstParagraphText);
            firstParagraphText = null;
        }
        if(lastParagraphText != null) {
            setText(endEditText, lastParagraphText);
            lastParagraphText = null;
        }
    }

    private void loadContents() {
        loadContentsButton.setVisibility(View.GONE);
        readLayout.setVisibility(View.VISIBLE);
        tabLayout.setVisibility(View.VISIBLE);
        hideKeyboard(this, loadContentsButton);
        if(webFragment != null) webFragment.runFindContents();
    }

    private void setText(EditText editText, String text) {
        if(text == null || text.isBlank()) editText.getText().clear();
        else editText.setText(text);
    }

    private void loadUrl(String url) {
        if(webFragment != null) {
            getSupportFragmentManager().beginTransaction().remove(webFragment).commit();
            webFragment = null;
        }
        if(pageFragment != null) {
            getSupportFragmentManager().beginTransaction().remove(pageFragment).commit();
            pageFragment = null;
        }
        pageContent = null;
        if(articleFragment != null) {
            getSupportFragmentManager().beginTransaction().remove(articleFragment).commit();
            articleFragment = null;
        }
        loadLayout.setVisibility(View.GONE);
        viewPager.setVisibility(View.VISIBLE);
        loadContentsButton.setVisibility(View.VISIBLE);
        getOnBackPressedDispatcher().addCallback(this, callback);
        articleContent = null;
        beginEditText.getText().clear();
        endEditText.getText().clear();
        textToSpeech.stop();
        this.url = url;
        adapter = new ViewPagerAdapter(getSupportFragmentManager(), getLifecycle());
        viewPager.setAdapter(adapter);
        viewPager.setUserInputEnabled(false);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            String title;
            if(position == 0 && url != null) title = "Web Content";
            else if(position == 1 && pageContent != null) title = "Page Content";
            else if(position == 2 && articleContent != null) title = "Article Content";
            else throw new IllegalArgumentException("Invalid fragment position: " + position);
            tab.setText(title);
        }).attach();

        hideKeyboard(this, loadContentsButton);
        if(!javascriptSwitch.isChecked()) loadContents();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void notifyChange() {
        runOnUiThread(() -> adapter.notifyDataSetChanged());
    }

    private void setBeginAndEnd(String content) {
        String[] split = content.split("\n");
        setText(beginEditText, split[0]);
        setText(endEditText, split[split.length - 1]);
    }

    @Override
    public void articleContent(String content) {
        articleContent = content;
        setBeginAndEnd(content);
        notifyChange();
    }


    @Override
    public void pageContent(String content) {
        pageContent = content;
        if(beginEditText.length() == 0 && endEditText.length() == 0) setBeginAndEnd(content);
        notifyChange();
    }

    public static void hideKeyboard(Activity activity, View view) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        loadIntentUrl(intent);
    }

    OnBackPressedCallback callback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            url = null;
            articleContent = null;
            firstParagraphText = null;
            lastParagraphText = null;
            webFragment = null;
            loadLayout.setVisibility(View.VISIBLE);
            viewPager.setVisibility(View.GONE);
            tabLayout.setVisibility(View.GONE);
            loadContentsButton.setVisibility(View.GONE);
            readLayout.setVisibility(View.GONE);
            textToSpeech.stop();
            remove();
            notifyChange();
        }
    };

    private class ViewPagerAdapter extends FragmentStateAdapter {

        public ViewPagerAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
            super(fragmentManager, lifecycle);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if(position == 0 && url != null) {
                webFragment = WebFragment.newInstance();
                webFragment.initialize(url, MainActivity.this, javascriptSwitch.isChecked());
                return webFragment;
            }
            else if(position == 1 && pageContent != null) {
                if(pageFragment == null) {
                    pageFragment = TextFragment.newInstance();
                    pageFragment.setContent(pageContent);
                }
                return pageFragment;
            }
            else if(position == 2 && articleContent != null) {
                if(articleFragment == null) {
                    articleFragment = TextFragment.newInstance();
                    articleFragment.setContent(articleContent);
                }
                return articleFragment;
            }
            throw new IllegalArgumentException("Invalid fragment position: " + position);
        }

        @Override
        public int getItemCount() {
            int count = 0;
            if(url != null) {
                count++;
                if (pageContent != null) {
                    count++;
                    if (articleContent != null) count++;
                }
            }
            return count;
        }
    }
}
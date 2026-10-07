package dev.mdaven.articlereader.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import dev.mdaven.articlereader.R;

public class TextFragment extends Fragment {
    private String content;

    public static TextFragment newInstance() {
        return new TextFragment();
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_text, container, false);

        TextView textView = view.findViewById(R.id.text_view);
        textView.setText(content);

        return view;
    }
}
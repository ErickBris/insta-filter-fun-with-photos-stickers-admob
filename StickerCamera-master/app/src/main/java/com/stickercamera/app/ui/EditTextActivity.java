package com.stickercamera.app.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.agile.libads.ads.AdsWrapper;
import com.common.util.StringUtils;
import com.github.skykai.stickercamera.R;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.firebase.iid.FirebaseInstanceId;
import com.stickercamera.AppConstants;
import com.stickercamera.base.BaseActivity;

import butterknife.ButterKnife;
import butterknife.InjectView;


/**
 * Edit text
 * Created by sky on 2015/7/20.
 * Weibo: http://weibo.com/2030683111
 * Email: 1132234509@qq.com
 */
public class EditTextActivity extends BaseActivity {

    private final static int MAX = 10;
    private int maxlength = MAX;
    @InjectView(R.id.text_input)
    EditText contentView;
    @InjectView(R.id.tag_input_tips)
    TextView numberTips;
    @InjectView(R.id.adView)
    AdView adView;

    public static void openTextEdit(Activity mContext, String defaultStr, int maxLength, int reqCode) {
        Intent i = new Intent(mContext, EditTextActivity.class);
        i.putExtra(AppConstants.PARAM_EDIT_TEXT, defaultStr);
        if (maxLength != 0) {
            i.putExtra(AppConstants.PARAM_MAX_SIZE, maxLength);
        }
        mContext.startActivityForResult(i, reqCode);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_text);
        ButterKnife.inject(this);
        maxlength = getIntent().getIntExtra(AppConstants.PARAM_MAX_SIZE, MAX);

        String token = FirebaseInstanceId.getInstance().getToken();
        AdsWrapper adsWrapper = new AdsWrapper.Builder().with(EditTextActivity.this).addTestDeviceIds(new String[]{AdRequest.DEVICE_ID_EMULATOR, token}).build();
        adsWrapper.requestBannerAd(adView);

        String defaultStr = getIntent().getStringExtra(AppConstants.PARAM_EDIT_TEXT);
        if (StringUtils.isNotEmpty(defaultStr)) {
            contentView.setText(defaultStr);
            if (defaultStr.length() <= maxlength) {
                numberTips.setText("You can also enter" + (maxlength - defaultStr.length()) + "Words  ("
                        + defaultStr.length() + "/" + maxlength + ")");
            }
        }
        titleBar.setRightBtnOnclickListener(v -> {
            Intent intent = new Intent();
            String inputTxt = contentView.getText().toString();
            intent.putExtra(AppConstants.PARAM_EDIT_TEXT, inputTxt);
            setResult(RESULT_OK, intent);
            finish();
        });
        contentView.addTextChangedListener(mTextWatcher);
    }

    TextWatcher mTextWatcher = new TextWatcher() {
        private int editStart;
        private int editEnd;

        @Override
        public void beforeTextChanged(CharSequence s, int arg1, int arg2,
                                      int arg3) {
        }

        @Override
        public void onTextChanged(CharSequence s, int arg1, int arg2,
                                  int arg3) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            editStart = contentView.getSelectionStart();
            editEnd = contentView.getSelectionEnd();
            if (s.toString().length() > maxlength) {
                toast("The number of words you entered has exceeded the limit!", Toast.LENGTH_LONG);
                s.delete(editStart - 1, editEnd);
                int tempSelection = editStart;
                contentView.setText(s);
                contentView.setSelection(tempSelection);
            }
            numberTips.setText("You can also enter"
                    + (maxlength - s.toString().length())
                    + "Words  (" + s.toString().length() + "/"
                    + maxlength + ")");
        }
    };
}
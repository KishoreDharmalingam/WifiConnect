package com.kishore.androidqbeta4;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BuildClickableMessage {

    Intent toIntent;
    Context context;
    Boolean isBoldable = false;
    int colorCode;
    public SpannableStringBuilder buildClickableMessageUsingRegEx(String regEx,
                                                                  String message,
                                                                  String defaultClickableText,
                                                                  Context context,
                                                                  Intent toIntent,
                                                                  Boolean isBoldable,
                                                                  int colorCode){

        int clickableIndex = -1;
        String clickableText = "";
        SpannableStringBuilder spannableStringBuilder = null;
        HashMap<Integer,Integer> hm = null;
        Pattern pattern = null;
        Matcher matcher = null;
        this.toIntent = toIntent;
        this.context = context;
        this.isBoldable = isBoldable;
        this.colorCode = colorCode;

        try{
            hm = new HashMap<Integer, Integer>();/*Hashmap for placeholder index(Key) & clickable text length(Value)*/
            pattern = Pattern.compile(regEx);
            matcher = pattern.matcher(message);

            while(matcher.find()) {
                clickableText = matcher.group(1);
                clickableIndex = matcher.start();
                message = message.replaceFirst(regEx,clickableText);
                matcher = pattern.matcher(message);
                hm.put(clickableIndex,clickableText.length());
            }

            if(clickableText.equals("")) {
                clickableText = defaultClickableText;
                clickableIndex = message.indexOf(clickableText);
                hm.put(clickableIndex,clickableText.length());
            }

            spannableStringBuilder = new SpannableStringBuilder();
            spannableStringBuilder.append(message);
            for (Map.Entry me:hm.entrySet() ){
                spannableStringBuilder = makeTextSpannable(spannableStringBuilder,(Integer)me.getKey(),(Integer)me.getValue());
            }

        }catch (Exception e){}
        return spannableStringBuilder;
    }

    public SpannableStringBuilder makeTextSpannable(SpannableStringBuilder sString, int startIndex, int textSize){
        sString.setSpan(new MakeTextLinkable(), startIndex,startIndex+textSize, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return sString;
    }

    public class MakeTextLinkable extends ClickableSpan{

        @Override
        public void onClick(View view) {
            context.startActivity(toIntent);
        }

        @Override
        public void updateDrawState(TextPaint textPaint) {
            super.updateDrawState(textPaint);

            textPaint.setColor(context.getResources().getColor(colorCode));
            textPaint.setUnderlineText(false);
            if(isBoldable)
                textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        }
    }
}

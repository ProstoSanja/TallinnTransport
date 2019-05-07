package com.psanja.tallinntransport.Utils;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.TransitionDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageButton;

import com.psanja.tallinntransport.R;

public class Tst extends AppCompatEditText {

    private boolean animstate = true;

    public Tst(Context context) {
        super(context);
    }
    public Tst(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Tst(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        if (focused) {
            switchanim(false);
        } else  {
            switchanim(true);
        }
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        if (lengthAfter > 0) {
            switchanim(false);
        } else if (lengthBefore > 0) {
            close();
        }
        super.onTextChanged(text,start,lengthBefore,lengthAfter);
    }

    void close() {
        switchanim(true);
        clearFocus();
        Keyboard.hide(getContext(), this);
    }

    void switchanim(boolean state) {
        if (!state && animstate) {
            ((TransitionDrawable) this.getBackground()).startTransition(100);
            animstate = false;
        } else if (state && !animstate) {
            ((TransitionDrawable) this.getBackground()).reverseTransition(100);
            animstate = true;
        }
    }
}

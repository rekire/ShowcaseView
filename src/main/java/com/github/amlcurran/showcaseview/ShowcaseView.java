/*
 * Copyright 2014 Alex Curran
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.amlcurran.showcaseview;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.github.amlcurran.showcaseview.targets.Target;
import com.github.amlcurran.showcaseview.targets.ViewTarget;

import java.util.ArrayList;

import static com.github.amlcurran.showcaseview.AnimationFactory.AnimationEndListener;
import static com.github.amlcurran.showcaseview.AnimationFactory.AnimationStartListener;

/**
 * A view which allows you to showcase areas of your app with an explanation.
 */
public class ShowcaseView extends RelativeLayout
        implements View.OnClickListener, View.OnTouchListener, ViewTreeObserver.OnPreDrawListener, ViewTreeObserver.OnGlobalLayoutListener {

    private static final int HOLO_BLUE = 0xff33B5E5;

    private final Button mEndButton;
    private final TextDrawer textDrawer;
    private final ShowcaseDrawer showcaseDrawer;
    private final ShowcaseAreaCalculator showcaseAreaCalculator;
    private final AnimationFactory animationFactory;
    private final ShotStateStore shotStateStore;

    // Touch items
    private boolean blockTouches = true;
    private OnShowcaseEventListener mEventListener = OnShowcaseEventListener.NONE;

    private boolean hasAlteredText = false;
    private boolean hasNoTarget = false;
    private boolean shouldCentreText;
    private Bitmap bitmapBuffer;

    // Animation items
    private long fadeInMillis;
    private long fadeOutMillis;

    private ShowcaseStep currentStep = new ShowcaseStep();
    private Target target;
    private float animationProgress = 0;
    private RectF start = null;

    protected ShowcaseView(Context context) {
        this(context, null, R.styleable.CustomTheme_showcaseViewStyle);
    }

    protected ShowcaseView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        ApiUtils apiUtils = new ApiUtils();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            animationFactory = new HoneycombAnimationFactory();
        } else {
            animationFactory = new LegacyAnimationFactory();
        }
        showcaseAreaCalculator = new ShowcaseAreaCalculator();
        shotStateStore = new ShotStateStore(context);

        apiUtils.setFitsSystemWindowsCompat(this);
        getViewTreeObserver().addOnPreDrawListener(this);
        getViewTreeObserver().addOnGlobalLayoutListener(this);

        // Get the attributes for the ShowcaseView
        final TypedArray styled = context.getTheme()
                .obtainStyledAttributes(attrs, R.styleable.ShowcaseView, R.attr.showcaseViewStyle,
                        R.style.ShowcaseView);

        // Set the default animation times
        fadeInMillis = getResources().getInteger(android.R.integer.config_mediumAnimTime);
        fadeOutMillis = getResources().getInteger(android.R.integer.config_mediumAnimTime);

        mEndButton = (Button) LayoutInflater.from(context).inflate(R.layout.showcase_button, null);
        //showcaseDrawer = new StandardShowcaseDrawer(getResources());
        showcaseDrawer = new FancyShowcaseDrawer(getResources());
        textDrawer = new TextDrawer(getResources(), showcaseAreaCalculator, getContext());

        updateStyle(styled, false);

        init();
    }

    private void init() {

        setOnTouchListener(this);

        if (mEndButton.getParent() == null) {
            int margin = (int) getResources().getDimension(R.dimen.button_margin);
            RelativeLayout.LayoutParams lps = (LayoutParams) generateDefaultLayoutParams();
            lps.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            lps.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            lps.setMargins(margin, margin, margin, margin);
            mEndButton.setLayoutParams(lps);
            mEndButton.setText(android.R.string.ok);
            mEndButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    currentStep.onCancelButtonClick(ShowcaseView.this);
                }
            });
            addView(mEndButton);
        }

    }

    public void setTarget(final Target target) {
        setShowcase(target, false);
    }

    public void setShowcase(final Target target, final boolean animate) {
        if(this.target != null) {
            this.start = this.target.getRect();
        }
        this.target = target;
        postDelayed(new Runnable() {
            @Override
            public void run() {
                    updateBitmap();
                    Point targetPoint = target.getPoint();
                    if (targetPoint != null) {
                        hasNoTarget = false;
                        if (animate) {
                            animationFactory.animateTargetToPoint(ShowcaseView.this, targetPoint);
                        } else {
                            // TODO check if this call here is important
                            setShowcasePosition(targetPoint);
                        }
                    } else {
                        hasNoTarget = true;
                        invalidate();
                    }
                }
        }, 100);
    }

    private void updateBitmap() {
        if(bitmapBuffer == null || haveBoundsChanged()) {
            if(bitmapBuffer != null) {
                bitmapBuffer.recycle();
            }
            if(getMeasuredWidth() > 0 && getMeasuredHeight() > 0) {
                bitmapBuffer = Bitmap.createBitmap(getMeasuredWidth(), getMeasuredHeight(), Bitmap.Config.ARGB_8888);
            }
        }
    }

    private boolean haveBoundsChanged() {
        return getMeasuredWidth() != bitmapBuffer.getWidth() ||
                getMeasuredHeight() != bitmapBuffer.getHeight();
    }

    // this setters and getters are required for animation
    void setShowcasePosition(Point point) {
        invalidate();
    }

    public void setProgress(float progress) {
        animationProgress = progress;
        invalidate();
    }

    public float getProgress() {
        return animationProgress;
    }

    public void setOnShowcaseEventListener(OnShowcaseEventListener listener) {
        if (listener != null) {
            mEventListener = listener;
        } else {
            mEventListener = OnShowcaseEventListener.NONE;
        }
    }

    public void setButtonText(CharSequence text) {
        if (mEndButton != null) {
            mEndButton.setText(text);
        }
    }

    @Override
    public boolean onPreDraw() {
        boolean recalculatedCling = showcaseAreaCalculator.calculateShowcaseRect(target.getPoint(), showcaseDrawer);
        boolean recalculateText = recalculatedCling || hasAlteredText;
        if(getMeasuredWidth() == 0 || getMeasuredHeight() == 0) {
            hasAlteredText = true;
            invalidate();
        } else {
            if(recalculateText) {
                textDrawer.calculateTextPosition(getMeasuredWidth(), getMeasuredHeight(), this, shouldCentreText);
            }
            hasAlteredText = false;
        }
        return true;
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        // TODO check if this old check is necessary: showcaseX < 0 || showcaseY < 0 ||
        if (shotStateStore.hasShot()) {
            super.dispatchDraw(canvas);
            return;
        }

        //Draw background color
        showcaseDrawer.erase(bitmapBuffer);

        // Draw the showcase drawable
        if (!hasNoTarget && bitmapBuffer != null) {
            showcaseDrawer.drawShowcase(bitmapBuffer, start, target.getRect(), animationProgress);
            showcaseDrawer.drawToCanvas(canvas, bitmapBuffer);
        }

        // Draw the text on the screen, recalculating its position if necessary
        textDrawer.draw(canvas);

        super.dispatchDraw(canvas);

    }

    @Override
    public void onClick(View view) {
        hide();
    }

    public void hide() {
        clearBitmap();
        // If the type is set to one-shot, store that it has shot
        shotStateStore.storeShot();
        mEventListener.onShowcaseViewHide(this);
        fadeOutShowcase();
    }

    private void clearBitmap() {
        if (bitmapBuffer != null && !bitmapBuffer.isRecycled()) {
            bitmapBuffer.recycle();
            bitmapBuffer = null;
        }
    }

    private void fadeOutShowcase() {
        animationFactory.fadeOutView(this, fadeOutMillis, new AnimationEndListener() {
            @Override
            public void onAnimationEnd() {
                setVisibility(View.GONE);
                mEventListener.onShowcaseViewDidHide(ShowcaseView.this);
            }
        });
    }

    public void show() {
        mEventListener.onShowcaseViewShow(this);
        fadeInShowcase();
    }

    private void fadeInShowcase() {
        animationFactory.fadeInView(this, fadeInMillis,
                new AnimationStartListener() {
                    @Override
                    public void onAnimationStart() {
                        setVisibility(View.VISIBLE);
                    }
                }
        );
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {


        boolean block = showcaseDrawer.shouldBeenBlocked((int)motionEvent.getRawX(), (int)motionEvent.getRawY());
        if (MotionEvent.ACTION_UP == motionEvent.getAction() && block) {
            currentStep.onClickOutside(this);
            return true;
        }

        return blockTouches && block;
    }

    private static void insertShowcaseView(ShowcaseView showcaseView, Activity activity) {
        ((ViewGroup) activity.getWindow().getDecorView()).addView(showcaseView);
            showcaseView.show();
    }

    private void hideImmediate() {
        setVisibility(GONE);
    }

    public void setContentTitle(CharSequence title) {
        textDrawer.setContentTitle(title);
    }

    public void setContentText(CharSequence text) {
        textDrawer.setContentText(text);
    }

    @Override
    public void onGlobalLayout() {
        if (!shotStateStore.hasShot()) {
            updateBitmap();
        }
    }

    public void hideButton() {
        mEndButton.setVisibility(GONE);
    }

    public void showButton() {
        mEndButton.setVisibility(VISIBLE);
    }

    /**
     * Builder class which allows easier creation of {@link ShowcaseView}s.
     * It is recommended that you use this Builder class.
     */
    public static class Builder {
        private int step = 0;
        private ArrayList<ShowcaseStep> steps = new ArrayList<ShowcaseStep>();
        private ShowcaseStep.OnNextStepListener stepListener = new ShowcaseStep.OnNextStepListener() {
            @Override
            public void nextStep() {
                if(step < steps.size() - 1) {
                    steps.get(++step).showStep(showcaseView, false);
                } else {
                    showcaseView.hide();
                }
            }
        };
        final ShowcaseView showcaseView;
        private final Activity activity;

        public Builder(Activity activity) {
            this.activity = activity;
            this.showcaseView = new ShowcaseView(activity);
            this.showcaseView.setTarget(Target.NONE);
        }

        public StepBuilder createStep() {
            return new StepBuilder(this);
        }

        /**
         * Create the {@link com.github.amlcurran.showcaseview.ShowcaseView} and show it.
         *
         * @return the created ShowcaseView
         */
        public ShowcaseView build() {
            if(!steps.isEmpty()) {
                showcaseView.currentStep.onStepGone(showcaseView);
                showcaseView.currentStep = steps.get(0);
                showcaseView.currentStep.showStep(showcaseView, true);
            }
            insertShowcaseView(showcaseView, activity);
            return showcaseView;
        }

        /**
         * Set the title text shown on the ShowcaseView.
         */
        public Builder setContentTitle(int resId) {
            return setContentTitle(activity.getString(resId));
        }

        /**
         * Set the title text shown on the ShowcaseView.
         */
        public Builder setContentTitle(CharSequence title) {
            showcaseView.setContentTitle(title);
            return this;
        }

        /**
         * Set the descriptive text shown on the ShowcaseView.
         */
        public Builder setContentText(int resId) {
            return setContentText(activity.getString(resId));
        }

        /**
         * Set the descriptive text shown on the ShowcaseView.
         */
        public Builder setContentText(CharSequence text) {
            showcaseView.setContentText(text);
            return this;
        }

        /**
         * Set the target of the showcase.
         *
         * @param target a {@link com.github.amlcurran.showcaseview.targets.Target} representing
         *               the item to showcase (e.g., a button, or action item).
         */
        public Builder setTarget(Target target) {
            showcaseView.setTarget(target);
            return this;
        }

        /**
         * Set the style of the ShowcaseView. See the sample app for example styles.
         */
        public Builder setStyle(int theme) {
            showcaseView.setStyle(theme);
            return this;
        }

        /**
         * Don't make the ShowcaseView block touches on itself. This doesn't
         * block touches in the showcased area.
         * <p/>
         * By default, the ShowcaseView does block touches
         */
        public Builder doNotBlockTouches() {
            showcaseView.setBlocksTouches(false);
            return this;
        }

        /**
         * Set the ShowcaseView to only ever show once.
         *
         * @param shotId a unique identifier (<em>across the app</em>) to store
         *               whether this ShowcaseView has been shown.
         */
        public Builder singleShot(long shotId) {
            showcaseView.setSingleShot(shotId);
            return this;
        }

        public Builder setShowcaseEventListener(OnShowcaseEventListener showcaseEventListener) {
            showcaseView.setOnShowcaseEventListener(showcaseEventListener);
            return this;
        }

        private String getString(int resId) {
            return showcaseView.getResources().getString(resId);
        }

        public Builder addStep(ShowcaseStep step) {
            step.setNextStepListener(stepListener);
            steps.add(step);
            return this;
        }
    }

    public static class StepBuilder extends ShowcaseStep {
        private Builder parent;

        private StepBuilder(Builder parent) {
            this.parent=parent;
        }

        public StepBuilder setTitle(int stringId) {
            this.title = parent.getString(stringId);
            return this;
        }

        public StepBuilder setTitle(String title) {
            this.title = title;
            return this;
        }

        public StepBuilder setMessage(String message) {
            this.message = message;
            return this;
        }

        public StepBuilder setMessage(int stringId) {
            this.message = parent.getString(stringId);
            return this;
        }

        public StepBuilder setTarget(ViewTarget view) {
            this.target = view;
            return this;
        }

        public Builder addThisStep() {
            parent.addStep(this);
            return parent;
        }

        public StepBuilder createNextStep() {
            parent.addStep(this);
            return new StepBuilder(parent);
        }

        public StepBuilder enableClickOutsideToSkip() {
            this.clickToSkip = true;
            return this;
        }
    }

    /**
     * Set whether the text should be centred in the screen, or left-aligned (which is the default).
     */
    public void setShouldCentreText(boolean shouldCentreText) {
        this.shouldCentreText = shouldCentreText;
        hasAlteredText = true;
        invalidate();
    }

    /**
     * @see com.github.amlcurran.showcaseview.ShowcaseView.Builder#setSingleShot(long)
     */
    private void setSingleShot(long shotId) {
        shotStateStore.setSingleShot(shotId);
    }

    /**
     * Change the position of the ShowcaseView's button from the default bottom-right position.
     *
     * @param layoutParams a {@link android.widget.RelativeLayout.LayoutParams} representing
     *                     the new position of the button
     */
    public void setButtonPosition(RelativeLayout.LayoutParams layoutParams) {
        mEndButton.setLayoutParams(layoutParams);
    }

    /**
     * Set the duration of the fading in and fading out of the ShowcaseView
     */
    private void setFadeDurations(long fadeInMillis, long fadeOutMillis) {
        this.fadeInMillis = fadeInMillis;
        this.fadeOutMillis = fadeOutMillis;
    }

    /**
     * @see com.github.amlcurran.showcaseview.ShowcaseView.Builder#doNotBlockTouches()
     */
    public void setBlocksTouches(boolean blockTouches) {
        this.blockTouches = blockTouches;
    }

    /**
     * @see com.github.amlcurran.showcaseview.ShowcaseView.Builder#setStyle(int)
     */
    public void setStyle(int theme) {
        TypedArray array = getContext().obtainStyledAttributes(theme, R.styleable.ShowcaseView);
        updateStyle(array, true);
    }

    private void updateStyle(TypedArray styled, boolean invalidate) {
        int backgroundColor = styled.getColor(R.styleable.ShowcaseView_sv_backgroundColor, Color.argb(128, 80, 80, 80));
        int showcaseColor = styled.getColor(R.styleable.ShowcaseView_sv_showcaseColor, HOLO_BLUE);
        String buttonText = styled.getString(R.styleable.ShowcaseView_sv_buttonText);
        if (TextUtils.isEmpty(buttonText)) {
            buttonText = getResources().getString(android.R.string.ok);
        }
        boolean tintButton = styled.getBoolean(R.styleable.ShowcaseView_sv_tintButtonColor, true);

        int titleTextAppearance = styled.getResourceId(R.styleable.ShowcaseView_sv_titleTextAppearance,
                R.style.TextAppearance_ShowcaseView_Title);
        int detailTextAppearance = styled.getResourceId(R.styleable.ShowcaseView_sv_detailTextAppearance,
                R.style.TextAppearance_ShowcaseView_Detail);

        styled.recycle();

        showcaseDrawer.setShowcaseColour(showcaseColor);
        showcaseDrawer.setBackgroundColour(backgroundColor);
        tintButton(showcaseColor, tintButton);
        mEndButton.setText(buttonText);
        textDrawer.setTitleStyling(titleTextAppearance);
        textDrawer.setDetailStyling(detailTextAppearance);
        hasAlteredText = true;

        if (invalidate) {
            invalidate();
        }
    }

    private void tintButton(int showcaseColor, boolean tintButton) {
        if (tintButton) {
            mEndButton.getBackground().setColorFilter(showcaseColor, PorterDuff.Mode.MULTIPLY);
        } else {
            mEndButton.getBackground().setColorFilter(HOLO_BLUE, PorterDuff.Mode.MULTIPLY);
        }
    }

}

/*
 * Copyright (c) 2014 TOMORROW FOCUS News+ GmbH. All rights reserved.
 */

package com.github.amlcurran.showcaseview;

import com.github.amlcurran.showcaseview.targets.ViewTarget;

/**
 * One step of the showcase process.
 * Created on 27.08.2014.
 *
 * @author René Kilczan
 */
public class ShowcaseStep {
    protected String title;
    protected String message;
    protected ViewTarget target;
    protected boolean clickToSkip;
    private OnNextStepListener listener;

    public void onClickOutside(ShowcaseView showcaseView) {
        if(clickToSkip && listener != null) {
            listener.nextStep();
        }
    }

    public void onCancelButtonClick(ShowcaseView showcaseView) {
        showcaseView.hide();
    }

    public final void showStep(ShowcaseView showcaseView, boolean isFirst) {
        showcaseView.setShowcase(target, !isFirst);
        showcaseView.setContentTitle(title);
        showcaseView.setContentText(message);
        //onPostShowStep(showcaseView);
    }

    //protected void onPostShowStep(ShowcaseView showcaseView) {
    //}

    public final void setNextStepListener(OnNextStepListener listener) {
        this.listener = listener;
    }

    public interface OnNextStepListener {
        void nextStep();
    }
}
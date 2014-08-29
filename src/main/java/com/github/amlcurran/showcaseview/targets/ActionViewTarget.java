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

package com.github.amlcurran.showcaseview.targets;

import android.app.Activity;
import android.graphics.Point;
import android.graphics.RectF;
import android.view.ViewParent;

public class ActionViewTarget implements Target {

    private final Activity mActivity;
    private final Type mType;

    ActionBarViewWrapper mActionBarWrapper;
    Reflector mReflector;

    public ActionViewTarget(Activity activity, Type type) {
        mActivity = activity;
        mType = type;
    }

    protected void setUp() {
        mReflector = ReflectorFactory.getReflectorForActivity(mActivity);
        ViewParent p = mReflector.getActionBarView(); //ActionBarView
        mActionBarWrapper = new ActionBarViewWrapper(p);
    }

    @Override
    public Point getPoint() {
        return getView().getPoint();
    }

    private ViewTarget getView() {
        setUp();
        switch (mType) {
        case SPINNER:
            return new ViewTarget(mActionBarWrapper.getSpinnerView());
        case HOME:
            return new ViewTarget(mReflector.getHomeButton());
        case OVERFLOW:
            return new ViewTarget(mActionBarWrapper.getOverflowView());
        case TITLE:
            return new ViewTarget(mActionBarWrapper.getTitleView());
        case MEDIA_ROUTE_BUTTON:
            return new ViewTarget(mActionBarWrapper.getMediaRouterButtonView());
        }
        return null;
    }

    @Override
    public RectF getRect() {
        return getView().getRect();
    }

    public enum Type {
        SPINNER, HOME, TITLE, OVERFLOW, MEDIA_ROUTE_BUTTON
    }
}

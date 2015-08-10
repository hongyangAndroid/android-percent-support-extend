/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zhy.android.percent.support;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.v4.view.MarginLayoutParamsCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Helper for layouts that want to support percentage based dimensions.
 * <p/>
 * <p>This class collects utility methods that are involved in extracting percentage based dimension
 * attributes and applying them to ViewGroup's children. If you would like to implement a layout
 * that supports percentage based dimensions, you need to take several steps:
 * <p/>
 * <ol>
 * <li> You need a {@link ViewGroup.LayoutParams} subclass in your ViewGroup that implements
 * {@link android.support.percent.PercentLayoutHelper.PercentLayoutParams}.
 * <li> In your {@code LayoutParams(Context c, AttributeSet attrs)} constructor create an instance
 * of {@link PercentLayoutHelper.PercentLayoutInfo} by calling
 * {@link PercentLayoutHelper#getPercentLayoutInfo(Context, AttributeSet)}. Return this
 * object from {@code public PercentLayoutHelper.PercentLayoutInfo getPercentLayoutInfo()}
 * method that you implemented for {@link android.support.percent.PercentLayoutHelper.PercentLayoutParams} interface.
 * <li> Override
 * {@link ViewGroup.LayoutParams#setBaseAttributes(TypedArray, int, int)}
 * with a single line implementation {@code PercentLayoutHelper.fetchWidthAndHeight(this, a,
 * widthAttr, heightAttr);}
 * <li> In your ViewGroup override {@link ViewGroup#generateLayoutParams(AttributeSet)} to return
 * your LayoutParams.
 * <li> In your {@link ViewGroup#onMeasure(int, int)} override, you need to implement following
 * pattern:
 * <pre class="prettyprint">
 * protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
 * mHelper.adjustChildren(widthMeasureSpec, heightMeasureSpec);
 * super.onMeasure(widthMeasureSpec, heightMeasureSpec);
 * if (mHelper.handleMeasuredStateTooSmall()) {
 * super.onMeasure(widthMeasureSpec, heightMeasureSpec);
 * }
 * }
 * </pre>
 * <li>In your {@link ViewGroup#onLayout(boolean, int, int, int, int)} override, you need to
 * implement following pattern:
 * <pre class="prettyprint">
 * protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
 * super.onLayout(changed, left, top, right, bottom);
 * mHelper.restoreOriginalParams();
 * }
 * </pre>
 * </ol>
 */
public class PercentLayoutHelper
{
    private static final String TAG = "PercentLayout";

    private final ViewGroup mHost;

    public PercentLayoutHelper(ViewGroup host)
    {
        mHost = host;
    }

    /**
     * Helper method to be called from {@link ViewGroup.LayoutParams#setBaseAttributes} override
     * that reads layout_width and layout_height attribute values without throwing an exception if
     * they aren't present.
     */
    public static void fetchWidthAndHeight(ViewGroup.LayoutParams params, TypedArray array,
                                           int widthAttr, int heightAttr)
    {
        params.width = array.getLayoutDimension(widthAttr, 0);
        params.height = array.getLayoutDimension(heightAttr, 0);
    }

    /**
     * Iterates over children and changes their width and height to one calculated from percentage
     * values.
     *
     * @param widthMeasureSpec  Width MeasureSpec of the parent ViewGroup.
     * @param heightMeasureSpec Height MeasureSpec of the parent ViewGroup.
     */
    public void adjustChildren(int widthMeasureSpec, int heightMeasureSpec)
    {
        if (Log.isLoggable(TAG, Log.DEBUG))
        {
            Log.d(TAG, "adjustChildren: " + mHost + " widthMeasureSpec: "
                    + View.MeasureSpec.toString(widthMeasureSpec) + " heightMeasureSpec: "
                    + View.MeasureSpec.toString(heightMeasureSpec));
        }
        int widthHint = View.MeasureSpec.getSize(widthMeasureSpec);
        int heightHint = View.MeasureSpec.getSize(heightMeasureSpec);
        if (Log.isLoggable(TAG, Log.DEBUG))
            Log.d(TAG, "widthHint = " + widthHint + " , heightHint = " + heightHint);
        for (int i = 0, N = mHost.getChildCount(); i < N; i++)
        {
            View view = mHost.getChildAt(i);
            ViewGroup.LayoutParams params = view.getLayoutParams();
            if (Log.isLoggable(TAG, Log.DEBUG))
            {
                Log.d(TAG, "should adjust " + view + " " + params);
            }
            if (params instanceof PercentLayoutParams)
            {
                PercentLayoutInfo info =
                        ((PercentLayoutParams) params).getPercentLayoutInfo();
                if (Log.isLoggable(TAG, Log.DEBUG))
                {
                    Log.d(TAG, "using " + info);
                }
                if (info != null)
                {
                    supportTextSize(widthHint, heightHint, view, info);
                    supportMinOrMaxDimesion(widthHint, heightHint, view, info);

                    if (params instanceof ViewGroup.MarginLayoutParams)
                    {
                        info.fillMarginLayoutParams((ViewGroup.MarginLayoutParams) params,
                                widthHint, heightHint);
                    } else
                    {
                        info.fillLayoutParams(params, widthHint, heightHint);
                    }
                }
            }
        }


    }

    private void supportMinOrMaxDimesion(int widthHint, int heightHint, View view, PercentLayoutInfo info)
    {
        try
        {
            Class clazz = view.getClass();
            invokeMethod("setMaxWidth", widthHint, heightHint, view, clazz, info.maxWidthPercent);
            invokeMethod("setMaxHeight", widthHint, heightHint, view, clazz, info.maxHeightPercent);
            invokeMethod("setMinWidth", widthHint, heightHint, view, clazz, info.minWidthPercent);
            invokeMethod("setMinHeight", widthHint, heightHint, view, clazz, info.minHeightPercent);

        } catch (NoSuchMethodException e)
        {
            e.printStackTrace();
        } catch (InvocationTargetException e)
        {
            e.printStackTrace();
        } catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }

    }

    private void invokeMethod(String methodName, int widthHint, int heightHint, View view, Class clazz, PercentLayoutInfo.PercentVal percentVal) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
    {
        if (Log.isLoggable(TAG, Log.DEBUG))
            Log.d(TAG, methodName + " ==> " + percentVal);
        if (percentVal != null)
        {
            Method setMaxWidthMethod = clazz.getMethod(methodName, int.class);
            setMaxWidthMethod.setAccessible(true);
            int base = percentVal.isBaseWidth ? widthHint : heightHint;
            setMaxWidthMethod.invoke(view, (int) (base * percentVal.percent));
        }
    }

    private void supportTextSize(int widthHint, int heightHint, View view, PercentLayoutInfo info)
    {
        //textsize percent support
        if (view instanceof TextView)
        {
            PercentLayoutInfo.PercentVal textSizePercent = info.textSizePercent;
            if (textSizePercent != null)
            {
                int base = textSizePercent.isBaseWidth ? widthHint : heightHint;
                float textSize = (int) (base * textSizePercent.percent);
                ((TextView) view).setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
            }
        }
    }


    /**
     * Constructs a PercentLayoutInfo from attributes associated with a View. Call this method from
     * {@code LayoutParams(Context c, AttributeSet attrs)} constructor.
     */
    public static PercentLayoutInfo getPercentLayoutInfo(Context context,
                                                         AttributeSet attrs)
    {
        PercentLayoutInfo info = null;
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.PercentLayout_Layout);

        int index = R.styleable.PercentLayout_Layout_layout_widthPercent;
        String sizeStr = array.getString(index);
        PercentLayoutInfo.PercentVal percentVal = getPercentVal(sizeStr, true);
        if (percentVal != null)
        {
            if (Log.isLoggable(TAG, Log.VERBOSE))
            {
                Log.v(TAG, "percent width: " + percentVal.percent);
            }
            info = checkForInfoExists(info);
            info.widthPercent = percentVal;
        }
        sizeStr = array.getString(R.styleable.PercentLayout_Layout_layout_heightPercent);
        percentVal = getPercentVal(sizeStr, false);

        if (sizeStr != null)
        {
            if (Log.isLoggable(TAG, Log.VERBOSE))
            {
                Log.v(TAG, "percent height: " + percentVal.percent);
            }
            info = checkForInfoExists(info);
            info.heightPercent = percentVal;
        }

        // value = array.getFraction(R.styleable.PercentLayout_Layout_layout_marginPercent, 1, 1, -1f);
        sizeStr = array.getString(R.styleable.PercentLayout_Layout_layout_marginPercent);
        // just for judge
        percentVal = getPercentVal(sizeStr, false);

        if (percentVal != null)
        {
            if (Log.isLoggable(TAG, Log.VERBOSE))
            {
                Log.v(TAG, "percent margin: " + percentVal.percent);
            }
            info = checkForInfoExists(info);
            info.leftMarginPercent = getPercentVal(sizeStr, true);
            info.topMarginPercent = getPercentVal(sizeStr, false);
            info.rightMarginPercent = getPercentVal(sizeStr, true);
            info.bottomMarginPercent = getPercentVal(sizeStr, false);
        }
        //value = array.getFraction(R.styleable.PercentLayout_Layout_layout_marginLeftPercent, 1, 1,
        //      -1f);
        sizeStr = array.getString(R.styleable.PercentLayout_Layout_layout_marginLeftPercent);
        percentVal = getPercentVal(sizeStr, true);
        if (percentVal != null)
        {
            if (Log.isLoggable(TAG, Log.VERBOSE))
            {
                Log.v(TAG, "percent left margin: " + percentVal.percent);
            }
            info = checkForInfoExists(info);
            info.leftMarginPercent = percentVal;
        }

        //  value = array.getFraction(R.styleable.PercentLayout_Layout_layout_marginTopPercent, 1, 1,
        //        -1f);
        sizeStr = array.getString(R.styleable.PercentLayout_Layout_layout_marginTopPercent);
        percentVal = getPercentVal(sizeStr, false);
        if (percentVal != null)
        {
            if (Log.isLoggable(TAG, Log.VERBOSE))
            {
                Log.v(TAG, "percent top margin: " + percentVal.percent);
            }
            info = checkForInfoExists(info);
            info.topMarginPercent = percentVal;
        }
        // value = array.getFraction(R.styleable.PercentLayout_Layout_layout_marginRightPercent, 1, 1,
        //       -1f);
        sizeStr = array.getString(R.styleable.PercentLayout_Layout_layout_marginRightPercent);
        percentVal = getPercentVal(sizeStr, true);
        if (percentVal != null)
        {
            if (Log.isLoggable(TAG, Log.VERBOSE))
            {
                Log.v(TAG, "percent right margin: " + percentVal.percent);
            }
            info = checkForInfoExists(info);
            info.rightMarginPercent = percentVal;
        }
        //value = array.getFraction(R.styleable.PercentLayout_Layout_layout_marginBottomPercent, 1, 1,
        //  -1f);
        sizeStr = array.getString(R.styleable.PercentLayout_Layout_layout_marginBottomPercent);
        percentVal = getPercentVal(sizeStr, false);
        if (percentVal != null)
        {
            if (Log.isLoggable(TAG, Log.VERBOSE))
            {
                Log.v(TAG, "percent bottom margin: " + percentVal.percent);
            }
            info = checkForInfoExists(info);
            info.bottomMarginPercent = percentVal;
        }
        // value = array.getFraction(R.styleable.PercentLayout_Layout_layout_marginStartPercent, 1, 1,
        //       -1f);
        sizeStr = array.getString(R.styleable.PercentLayout_Layout_layout_marginStartPercent);
        percentVal = getPercentVal(sizeStr, true);
        if (percentVal != null)
        {
            if (Log.isLoggable(TAG, Log.VERBOSE))
            {
                Log.v(TAG, "percent start margin: " + percentVal.percent);
            }
            info = checkForInfoExists(info);
            info.startMarginPercent = percentVal;
        }
        //value = array.getFraction(R.styleable.PercentLayout_Layout_layout_marginEndPercent, 1, 1,
        //      -1f);
        sizeStr = array.getString(R.styleable.PercentLayout_Layout_layout_marginEndPercent);
        percentVal = getPercentVal(sizeStr, true);
        if (percentVal != null)
        {
            if (Log.isLoggable(TAG, Log.VERBOSE))
            {
                Log.v(TAG, "percent end margin: " + percentVal.percent);
            }
            info = checkForInfoExists(info);
            info.endMarginPercent = percentVal;
        }

        //textSizePercent
        sizeStr = array.getString(R.styleable.PercentLayout_Layout_layout_textSizePercent);
        percentVal = getPercentVal(sizeStr, false);
        if (percentVal != null)
        {
            if (Log.isLoggable(TAG, Log.VERBOSE))
            {
                Log.v(TAG, "percent text size: " + percentVal.percent);
            }
            info = checkForInfoExists(info);
            info.textSizePercent = percentVal;
        }

        //maxWidth
        percentVal = getPercentVal(array,
                R.styleable.PercentLayout_Layout_layout_maxWidthPercent,
                true);
        if (percentVal != null)
        {
            checkForInfoExists(info);
            info.maxWidthPercent = percentVal;
        }
        //maxHeight
        percentVal = getPercentVal(array,
                R.styleable.PercentLayout_Layout_layout_maxHeightPercent,
                false);
        if (percentVal != null)
        {
            checkForInfoExists(info);
            info.maxHeightPercent = percentVal;
        }
        //minWidth
        percentVal = getPercentVal(array,
                R.styleable.PercentLayout_Layout_layout_minWidthPercent,
                true);
        if (percentVal != null)
        {
            checkForInfoExists(info);
            info.minWidthPercent = percentVal;
        }
        //minHeight
        percentVal = getPercentVal(array,
                R.styleable.PercentLayout_Layout_layout_minHeightPercent,
                false);
        Log.d(TAG, "minHeight = " + percentVal);
        if (percentVal != null)
        {
            checkForInfoExists(info);
            info.minHeightPercent = percentVal;
        }

        array.recycle();
        if (Log.isLoggable(TAG, Log.DEBUG))
        {
            Log.d(TAG, "constructed: " + info);
        }
        return info;
    }

    private static PercentLayoutInfo.PercentVal getPercentVal(TypedArray array, int index, boolean baseWidth)
    {
        String sizeStr = array.getString(index);
        PercentLayoutInfo.PercentVal percentVal = getPercentVal(sizeStr, baseWidth);
        return percentVal;
    }


    @NonNull
    private static PercentLayoutInfo checkForInfoExists(PercentLayoutInfo info)
    {
        info = info != null ? info : new PercentLayoutInfo();
        return info;
    }


    private static final String REGEX_PERCENT = "^(([0-9]+)([.]([0-9]+))?|([.]([0-9]+))?)%([wh]?)$";

    /**
     * widthStr to PercentVal
     * <br/>
     * eg: 35%w => new PercentVal(35, true)
     *
     * @param percentStr
     * @param isOnWidth
     * @return
     */
    private static PercentLayoutInfo.PercentVal getPercentVal(String percentStr, boolean isOnWidth)
    {
        //valid param
        if (percentStr == null)
        {
            return null;
        }
        Pattern p = Pattern.compile(REGEX_PERCENT);
        Matcher matcher = p.matcher(percentStr);
        if (!matcher.matches())
        {
            throw new RuntimeException("the value of layout_xxxPercent invalid! ==>" + percentStr);
        }
        int len = percentStr.length();
        //extract the float value
        String floatVal = matcher.group(1);
        String lastAlpha = percentStr.substring(len - 1);

        float percent = Float.parseFloat(floatVal) / 100f;
        boolean isBasedWidth = (isOnWidth && !lastAlpha.equals("h")) || lastAlpha.equals("w");

        return new PercentLayoutInfo.PercentVal(percent, isBasedWidth);
    }

    /**
     * Iterates over children and restores their original dimensions that were changed for
     * percentage values. Calling this method only makes sense if you previously called
     * {@link PercentLayoutHelper#adjustChildren(int, int)}.
     */

    public void restoreOriginalParams()
    {
        for (int i = 0, N = mHost.getChildCount(); i < N; i++)
        {
            View view = mHost.getChildAt(i);
            ViewGroup.LayoutParams params = view.getLayoutParams();
            if (Log.isLoggable(TAG, Log.DEBUG))
            {
                Log.d(TAG, "should restore " + view + " " + params);
            }
            if (params instanceof PercentLayoutParams)
            {
                PercentLayoutInfo info =
                        ((PercentLayoutParams) params).getPercentLayoutInfo();
                if (Log.isLoggable(TAG, Log.DEBUG))
                {
                    Log.d(TAG, "using " + info);
                }
                if (info != null)
                {
                    if (params instanceof ViewGroup.MarginLayoutParams)
                    {
                        info.restoreMarginLayoutParams((ViewGroup.MarginLayoutParams) params);
                    } else
                    {
                        info.restoreLayoutParams(params);
                    }
                }
            }
        }
    }

    /**
     * Iterates over children and checks if any of them would like to get more space than it
     * received through the percentage dimension.
     * <p/>
     * If you are building a layout that supports percentage dimensions you are encouraged to take
     * advantage of this method. The developer should be able to specify that a child should be
     * remeasured by adding normal dimension attribute with {@code wrap_content} value. For example
     * he might specify child's attributes as {@code app:layout_widthPercent="60%p"} and
     * {@code android:layout_width="wrap_content"}. In this case if the child receives too little
     * space, it will be remeasured with width set to {@code WRAP_CONTENT}.
     *
     * @return True if the measure phase needs to be rerun because one of the children would like
     * to receive more space.
     */
    public boolean handleMeasuredStateTooSmall()
    {
        boolean needsSecondMeasure = false;
        for (int i = 0, N = mHost.getChildCount(); i < N; i++)
        {
            View view = mHost.getChildAt(i);
            ViewGroup.LayoutParams params = view.getLayoutParams();
            if (Log.isLoggable(TAG, Log.DEBUG))
            {
                Log.d(TAG, "should handle measured state too small " + view + " " + params);
            }
            if (params instanceof PercentLayoutParams)
            {
                PercentLayoutInfo info =
                        ((PercentLayoutParams) params).getPercentLayoutInfo();
                if (info != null)
                {
                    if (shouldHandleMeasuredWidthTooSmall(view, info))
                    {
                        needsSecondMeasure = true;
                        params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                    }
                    if (shouldHandleMeasuredHeightTooSmall(view, info))
                    {
                        needsSecondMeasure = true;
                        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    }
                }
            }
        }
        if (Log.isLoggable(TAG, Log.DEBUG))
        {
            Log.d(TAG, "should trigger second measure pass: " + needsSecondMeasure);
        }
        return needsSecondMeasure;
    }

    private static boolean shouldHandleMeasuredWidthTooSmall(View view, PercentLayoutInfo info)
    {
        int state = ViewCompat.getMeasuredWidthAndState(view) & ViewCompat.MEASURED_STATE_MASK;
        return state == ViewCompat.MEASURED_STATE_TOO_SMALL && info.widthPercent.percent >= 0 &&
                info.mPreservedParams.width == ViewGroup.LayoutParams.WRAP_CONTENT;
    }

    private static boolean shouldHandleMeasuredHeightTooSmall(View view, PercentLayoutInfo info)
    {
        int state = ViewCompat.getMeasuredHeightAndState(view) & ViewCompat.MEASURED_STATE_MASK;
        return state == ViewCompat.MEASURED_STATE_TOO_SMALL && info.heightPercent.percent >= 0 &&
                info.mPreservedParams.height == ViewGroup.LayoutParams.WRAP_CONTENT;
    }


    /**
     * Container for information about percentage dimensions and margins. It acts as an extension
     * for {@code LayoutParams}.
     */
    public static class PercentLayoutInfo
    {

        public static class PercentVal
        {

            public float percent = -1;
            public boolean isBaseWidth;

            public PercentVal()
            {
            }

            public PercentVal(float percent, boolean isBaseWidth)
            {
                this.percent = percent;
                this.isBaseWidth = isBaseWidth;
            }
        }

        public PercentVal widthPercent;

        public PercentVal heightPercent;

        public PercentVal leftMarginPercent;

        public PercentVal topMarginPercent;

        public PercentVal rightMarginPercent;

        public PercentVal bottomMarginPercent;

        public PercentVal startMarginPercent;

        public PercentVal endMarginPercent;

        public PercentVal textSizePercent;

        //1.0.4 those attr for some views' setMax/min Height/Width method
        public PercentVal maxWidthPercent;
        public PercentVal maxHeightPercent;
        public PercentVal minWidthPercent;
        public PercentVal minHeightPercent;


        /* package */ final ViewGroup.MarginLayoutParams mPreservedParams;


        public PercentLayoutInfo()
        {
            mPreservedParams = new ViewGroup.MarginLayoutParams(0, 0);
        }

        /**
         * Fills {@code ViewGroup.LayoutParams} dimensions based on percentage values.
         */
        public void fillLayoutParams(ViewGroup.LayoutParams params, int widthHint,
                                     int heightHint)
        {
            // Preserve the original layout params, so we can restore them after the measure step.
            mPreservedParams.width = params.width;
            mPreservedParams.height = params.height;
            /*
            if (widthPercent >= 0) {
                params.width = (int) (widthHint * widthPercent);
            }
            if (heightPercent >= 0) {
                params.height = (int) (heightHint * heightPercent);
            }*/
            if (widthPercent != null)
            {
                int base = widthPercent.isBaseWidth ? widthHint : heightHint;
                params.width = (int) (base * widthPercent.percent);
            }
            if (heightPercent != null)
            {
                int base = heightPercent.isBaseWidth ? widthHint : heightHint;
                params.height = (int) (base * heightPercent.percent);
            }

            if (Log.isLoggable(TAG, Log.DEBUG))
            {
                Log.d(TAG, "after fillLayoutParams: (" + params.width + ", " + params.height + ")");
            }
        }

        /**
         * Fills {@code ViewGroup.MarginLayoutParams} dimensions and margins based on percentage
         * values.
         */
        public void fillMarginLayoutParams(ViewGroup.MarginLayoutParams params, int widthHint,
                                           int heightHint)
        {
            fillLayoutParams(params, widthHint, heightHint);

            // Preserver the original margins, so we can restore them after the measure step.
            mPreservedParams.leftMargin = params.leftMargin;
            mPreservedParams.topMargin = params.topMargin;
            mPreservedParams.rightMargin = params.rightMargin;
            mPreservedParams.bottomMargin = params.bottomMargin;
            MarginLayoutParamsCompat.setMarginStart(mPreservedParams,
                    MarginLayoutParamsCompat.getMarginStart(params));
            MarginLayoutParamsCompat.setMarginEnd(mPreservedParams,
                    MarginLayoutParamsCompat.getMarginEnd(params));

            if (leftMarginPercent != null)
            {
                int base = leftMarginPercent.isBaseWidth ? widthHint : heightHint;
                params.leftMargin = (int) (base * leftMarginPercent.percent);
            }
            if (topMarginPercent != null)
            {
                int base = topMarginPercent.isBaseWidth ? widthHint : heightHint;
                params.topMargin = (int) (base * topMarginPercent.percent);
            }
            if (rightMarginPercent != null)
            {
                int base = rightMarginPercent.isBaseWidth ? widthHint : heightHint;
                params.rightMargin = (int) (base * rightMarginPercent.percent);
            }
            if (bottomMarginPercent != null)
            {
                int base = bottomMarginPercent.isBaseWidth ? widthHint : heightHint;
                params.bottomMargin = (int) (base * bottomMarginPercent.percent);
            }
            if (startMarginPercent != null)
            {
                int base = startMarginPercent.isBaseWidth ? widthHint : heightHint;
                MarginLayoutParamsCompat.setMarginStart(params,
                        (int) (base * startMarginPercent.percent));
            }
            if (endMarginPercent != null)
            {
                int base = endMarginPercent.isBaseWidth ? widthHint : heightHint;
                MarginLayoutParamsCompat.setMarginEnd(params,
                        (int) (base * endMarginPercent.percent));
            }
            if (Log.isLoggable(TAG, Log.DEBUG))
            {
                Log.d(TAG, "after fillMarginLayoutParams: (" + params.width + ", " + params.height
                        + ")");
            }
        }

        @Override
        public String toString()
        {
            return String.format("PercentLayoutInformation width: %f height %f, margins (%f, %f, "
                            + " %f, %f, %f, %f)", widthPercent, heightPercent, leftMarginPercent,
                    topMarginPercent, rightMarginPercent, bottomMarginPercent, startMarginPercent,
                    endMarginPercent);

        }

        /**
         * Restores original dimensions and margins after they were changed for percentage based
         * values. Calling this method only makes sense if you previously called
         * {@link PercentLayoutHelper.PercentLayoutInfo#fillMarginLayoutParams}.
         */
        public void restoreMarginLayoutParams(ViewGroup.MarginLayoutParams params)
        {
            restoreLayoutParams(params);
            params.leftMargin = mPreservedParams.leftMargin;
            params.topMargin = mPreservedParams.topMargin;
            params.rightMargin = mPreservedParams.rightMargin;
            params.bottomMargin = mPreservedParams.bottomMargin;
            MarginLayoutParamsCompat.setMarginStart(params,
                    MarginLayoutParamsCompat.getMarginStart(mPreservedParams));
            MarginLayoutParamsCompat.setMarginEnd(params,
                    MarginLayoutParamsCompat.getMarginEnd(mPreservedParams));
        }

        /**
         * Restores original dimensions after they were changed for percentage based values. Calling
         * this method only makes sense if you previously called
         * {@link PercentLayoutHelper.PercentLayoutInfo#fillLayoutParams}.
         */
        public void restoreLayoutParams(ViewGroup.LayoutParams params)
        {
            params.width = mPreservedParams.width;
            params.height = mPreservedParams.height;
        }
    }

    /**
     * If a layout wants to support percentage based dimensions and use this helper class, its
     * {@code LayoutParams} subclass must implement this interface.
     * <p/>
     * Your {@code LayoutParams} subclass should contain an instance of {@code PercentLayoutInfo}
     * and the implementation of this interface should be a simple accessor.
     */
    public interface PercentLayoutParams
    {
        PercentLayoutInfo getPercentLayoutInfo();
    }
}

package com.tuogol.library;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

/**
 * A rating dialog that does a bit of social engineering.
 * Typical usage would be to only direct people who rate 5 stars to the Play Store.
 * 1-4 Stars would solicit feedback for improvement.
 *
 * Implementation of when the dialog is shown or how frequently is up to you.
 * Be smart and attempt to show only at moments when you won't tick your user off
 * and after the user has had a good experience with your app.
 */
public class RatingDialog extends AppCompatDialogFragment {

    public enum Style {LIGHT, DARK}

    private static final String RATING_STATE = "RATING_STATE";
    private static final String SHOW_AS_LIGHT = "SHOW_AS_LIGHT";
    private static final String DIALOG_TITLE = "DIALOG_TITLE";
    private static final String DIALOG_FEEDBACK_HINT = "DIALOG_FEEDBACK_HINT";
    private static final String DIALOG_TOAST = "DIALOG_TOAST";
    private static final String CANCELABLE = "CANCELABLE";

    /**
     * Interface for getting rating results and click events
     *
     * ratingResults with number of stars selected and feedback
     * ** Feedback may be an empty string if no feedback was given
     *
     * ratingClicked with number of stars and reference to RatingDialog
     */
    public interface RatingDialogCallback {
        void ratingResults(int stars, @NonNull String feedback);
        void ratingClicked(int stars, @Nullable RatingDialog dialog);
        boolean solicitFeedback(int stars);
    }

    private EditText mFeedbackEditText;
    private ImageView mRating1, mRating2, mRating3, mRating4, mRating5;

    @Nullable private RatingDialogCallback mCallback;
    private int mStarClicked = 0;
    private int[] mColorState = new int[5];
    private int[] mColors = new int[2];

    //region Lifecycle

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if(context instanceof RatingDialogCallback) {
            mCallback = (RatingDialogCallback)context;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null) {
            mStarClicked = savedInstanceState.getInt(RATING_STATE, 0);
        }

        final int gray = ContextCompat.getColor(getContext(), R.color.rating_dialog_gray);
        final int primary = ContextCompat.getColor(getContext(), R.color.rating_dialog_primary);

        mColors[0] = gray;
        mColors[1] = primary;

        //Init Color State
        mColorState[0] = mColorState[1] = mColorState[2] = mColorState[3] = mColorState[4] = gray;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        View rootView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_rating, null);

        final boolean showAsLight = getArguments().getBoolean(SHOW_AS_LIGHT, true);
        final String title = getArguments().getString(DIALOG_TITLE, getString(R.string.rating_dialog_title));

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(),
                showAsLight ? R.style.RatingDialogStyle_Light : R.style.RatingDialogStyle_Dark);
        builder.setTitle(title);
        builder.setView(rootView);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        setup(rootView);

        final boolean cancelable = getArguments().getBoolean(CANCELABLE, true);

        Dialog dialog = builder.create();
        dialog.setCancelable(cancelable);
        dialog.setCanceledOnTouchOutside(cancelable);
        return dialog;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (mFeedbackEditText != null && mFeedbackEditText.getText().length() > 0 && getContext() != null) {

            if(getArguments().containsKey(DIALOG_TOAST)) {
                String toast = getArguments().getString(DIALOG_TOAST);
                if(!TextUtils.isEmpty(toast)) {
                    Toast.makeText(getActivity(), toast, Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(getActivity(), R.string.rating_dialog_feedback_thanks, Toast.LENGTH_LONG).show();
            }

            if(mCallback != null) {
                mCallback.ratingResults(mStarClicked, mFeedbackEditText.getText().toString());
            }
        }
        super.onDismiss(dialog);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if(outState != null) {
            outState.putInt(RATING_STATE, mStarClicked);
        }
        super.onSaveInstanceState(outState);
    }

    //endregion

    //region Hiding/Showing

    private static RatingDialog newInstance(boolean showAsLight) {
        Bundle args = new Bundle();
        args.putBoolean(SHOW_AS_LIGHT, showAsLight);

        RatingDialog dialog = new RatingDialog();
        dialog.setArguments(args);
        return dialog;
    }

    private static RatingDialog newInstance(boolean showAsLight, String title, String feedbackHint, String toast, boolean cancelable) {
        Bundle args = new Bundle();
        args.putBoolean(SHOW_AS_LIGHT, showAsLight);
        args.putString(DIALOG_TITLE, title);
        args.putString(DIALOG_FEEDBACK_HINT, feedbackHint);
        args.putString(DIALOG_TOAST, toast);
        args.putBoolean(CANCELABLE, cancelable);

        RatingDialog dialog = new RatingDialog();
        dialog.setArguments(args);
        return dialog;
    }

    /**
     * Show the dialog
     * @param fm Support Fragment Manager
     * @param style Preference to show as Light or Dark
     */
    public static void show(FragmentManager fm, Style style) {
        if(fm.findFragmentByTag(RatingDialog.class.getSimpleName()) != null) {
            remove(fm);
        }

        try {
            RatingDialog dialog = RatingDialog.newInstance(style == Style.LIGHT);
            dialog.show(fm, RatingDialog.class.getSimpleName());
        } catch (IllegalStateException e) {
            Log.e("RatingDialog", "Dialog Down, Dialog Down! " + e);
        }
    }

    public static void show(FragmentManager fm, Style style, String title, String feedbackHint, String toast, boolean cancelable) {
        if(fm.findFragmentByTag(RatingDialog.class.getSimpleName()) != null) {
            remove(fm);
        }

        try {
            RatingDialog dialog = RatingDialog.newInstance(style == Style.LIGHT, title, feedbackHint, toast, cancelable);
            dialog.show(fm, RatingDialog.class.getSimpleName());
        } catch (IllegalStateException e) {
            Log.e("RatingDialog", "Dialog Down, Dialog Down! " + e);
        }
    }

    public static void remove(FragmentManager fm) {
        Fragment fragment = fm.findFragmentByTag(RatingDialog.class.getSimpleName());
        if(fragment instanceof RatingDialog) {
            ((RatingDialog)fragment).dismissAllowingStateLoss();
        }
    }

    //endregion

    //region View Setup

    private void setup(View rootView) {

        //Init Views
        mRating1 = (ImageView) rootView.findViewById(R.id.rating1);
        mRating2 = (ImageView) rootView.findViewById(R.id.rating2);
        mRating3 = (ImageView) rootView.findViewById(R.id.rating3);
        mRating4 = (ImageView) rootView.findViewById(R.id.rating4);
        mRating5 = (ImageView) rootView.findViewById(R.id.rating5);
        mFeedbackEditText = (EditText) rootView.findViewById(R.id.feedbackEditText);

        final String feedbackHint = getArguments().getString(DIALOG_FEEDBACK_HINT, getString(R.string.rating_dialog_title));
        mFeedbackEditText.setHint(feedbackHint);

        //Init Rating Colors
        colorViews(mColors[0], mRating1, mRating2, mRating3, mRating4, mRating5);

        //Set Listeners
        mRating1.setOnClickListener(mRating1ClickListener);
        mRating2.setOnClickListener(mRating2ClickListener);
        mRating3.setOnClickListener(mRating3ClickListener);
        mRating4.setOnClickListener(mRating4ClickListener);
        mRating5.setOnClickListener(mRating5ClickListener);

        //Restore State
        switch (mStarClicked) {
            case 1:
                mRating1.performClick();
                break;
            case 2:
                mRating2.performClick();
                break;
            case 3:
                mRating3.performClick();
                break;
            case 4:
                mRating4.performClick();
                break;
            case 5:
                mRating5.performClick();
                break;
            default:
                break;
        }
    }

    //endregion

    //region Click Listeners

    private void solicitFeedback(int stars) {
        if(mCallback != null) {
            if(mCallback.solicitFeedback(stars)) {
                mFeedbackEditText.setVisibility(View.VISIBLE);
            } else {
                mFeedbackEditText.setVisibility(View.GONE);
            }
        } else {
            mFeedbackEditText.setVisibility(stars == 5 ? View.GONE : View.VISIBLE);
        }
    }

    private View.OnClickListener mRating1ClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //Animate
            colorDrawable(mRating1, mColorState[0], mColors[1], 1);
            colorDrawable(mRating2, mColorState[1], mColors[0], 2);
            colorDrawable(mRating3, mColorState[2], mColors[0], 3);
            colorDrawable(mRating4, mColorState[3], mColors[0], 4);
            colorDrawable(mRating5, mColorState[4], mColors[0], 5);

            //Update State
            mColorState[0] = mColors[1];
            mColorState[1] = mColors[0];
            mColorState[2] = mColors[0];
            mColorState[3] = mColors[0];
            mColorState[4] = mColors[0];

            mStarClicked = 1;
            solicitFeedback(1);
            if(mCallback != null) mCallback.ratingClicked(1, getThis());
        }
    };

    private View.OnClickListener mRating2ClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //Animate
            colorDrawable(mRating1, mColorState[0], mColors[1], 1);
            colorDrawable(mRating2, mColorState[1], mColors[1], 2);
            colorDrawable(mRating3, mColorState[2], mColors[0], 3);
            colorDrawable(mRating4, mColorState[3], mColors[0], 4);
            colorDrawable(mRating5, mColorState[4], mColors[0], 5);

            //Update State
            mColorState[0] = mColors[1];
            mColorState[1] = mColors[1];
            mColorState[2] = mColors[0];
            mColorState[3] = mColors[0];
            mColorState[4] = mColors[0];

            mFeedbackEditText.setVisibility(View.VISIBLE);

            mStarClicked = 2;
            solicitFeedback(2);
            if(mCallback != null) mCallback.ratingClicked(2, getThis());
        }
    };

    private View.OnClickListener mRating3ClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //Animate
            colorDrawable(mRating1, mColorState[0], mColors[1], 1);
            colorDrawable(mRating2, mColorState[1], mColors[1], 2);
            colorDrawable(mRating3, mColorState[2], mColors[1], 3);
            colorDrawable(mRating4, mColorState[3], mColors[0], 4);
            colorDrawable(mRating5, mColorState[4], mColors[0], 5);

            //Update State
            mColorState[0] = mColors[1];
            mColorState[1] = mColors[1];
            mColorState[2] = mColors[1];
            mColorState[3] = mColors[0];
            mColorState[4] = mColors[0];

            mFeedbackEditText.setVisibility(View.VISIBLE);

            mStarClicked = 3;
            solicitFeedback(3);
            if(mCallback != null) mCallback.ratingClicked(3, getThis());
        }
    };

    private View.OnClickListener mRating4ClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //Animate
            colorDrawable(mRating1, mColorState[0], mColors[1], 1);
            colorDrawable(mRating2, mColorState[1], mColors[1], 2);
            colorDrawable(mRating3, mColorState[2], mColors[1], 3);
            colorDrawable(mRating4, mColorState[3], mColors[1], 4);
            colorDrawable(mRating5, mColorState[4], mColors[0], 5);

            //Update State
            mColorState[0] = mColors[1];
            mColorState[1] = mColors[1];
            mColorState[2] = mColors[1];
            mColorState[3] = mColors[1];
            mColorState[4] = mColors[0];

            mFeedbackEditText.setVisibility(View.VISIBLE);

            mStarClicked = 4;
            solicitFeedback(4);
            if(mCallback != null) mCallback.ratingClicked(4, getThis());
        }
    };

    private View.OnClickListener mRating5ClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //Animate
            colorDrawable(mRating1, mColorState[0], mColors[1], 1);
            colorDrawable(mRating2, mColorState[1], mColors[1], 2);
            colorDrawable(mRating3, mColorState[2], mColors[1], 3);
            colorDrawable(mRating4, mColorState[3], mColors[1], 4);
            colorDrawable(mRating5, mColorState[4], mColors[1], 5);

            //Update State
            mColorState[0] = mColors[1];
            mColorState[1] = mColors[1];
            mColorState[2] = mColors[1];
            mColorState[3] = mColors[1];
            mColorState[4] = mColors[1];

            fadeOut(mFeedbackEditText);

            mStarClicked = 5;
            solicitFeedback(5);
            if(mCallback != null) mCallback.ratingClicked(5, getThis());
        }
    };

    //endregion

    //region View Coloring & Drawing

    private void colorViews(int color, ImageView...views) {
        for(ImageView imageView : views) {
            setDrawable(imageView, color);
        }
    }

    private Drawable colorIt(int color, Drawable drawable) {
        drawable.mutate().setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP));
        return drawable;
    }

    private void colorDrawable(final ImageView view, int colorFrom, int colorTo, int multiplier) {
        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
        int value = multiplier * 120;
        colorAnimation.setDuration(320 + value);
        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                final Drawable drawable = colorIt((int) animator.getAnimatedValue(), view.getDrawable());
                view.setImageDrawable(drawable);
            }

        });
        colorAnimation.start();
    }

    private void setDrawable(final ImageView view, int colorTo) {
        view.setImageDrawable(colorIt(colorTo, view.getDrawable()));
    }

    private void fadeOut(final View view) {
        //Prevent fade if already invisible or gone
        if(view.getVisibility() != View.VISIBLE) return;
        
        Animation fade = new AlphaAnimation(1, 0);
        fade.setDuration(220);
        fade.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                view.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        view.startAnimation(fade);
    }

    //endregion

    private RatingDialog getThis() {
        return this;
    }
}

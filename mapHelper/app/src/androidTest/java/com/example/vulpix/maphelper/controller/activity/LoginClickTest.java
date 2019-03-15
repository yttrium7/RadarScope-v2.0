package com.example.vulpix.maphelper.controller.activity;

import android.widget.EditText;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.Rule;


import android.support.test.runner.AndroidJUnit4;
import android.support.test.rule.ActivityTestRule;
import android.support.test.filters.LargeTest;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;

import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.RootMatchers.isDialog;
import static android.support.test.espresso.matcher.ViewMatchers.isRoot;
import static org.junit.Assert.*;
import com.example.vulpix.maphelper.R;


@RunWith(AndroidJUnit4.class)

public class LoginClickTest {

    @Rule
    public ActivityTestRule<LoginActivity> mActivityRule = new ActivityTestRule<>(LoginActivity.class);


    @Test
    public void testNoti_EmptyEmail(){
        onView(withId(R.id.loginEmail)).perform(typeText(""), closeSoftKeyboard());
        onView(withId(R.id.loginPsw)).perform(typeText("123456"),closeSoftKeyboard());
        onView(withId(R.id.logAcc)).check(matches(isDisplayed()));
    }
    @Test
    public void testNoti_EmptyPsw(){
        onView(withId(R.id.loginEmail)).perform(typeText("123@example.com"), closeSoftKeyboard());
        onView(withId(R.id.loginPsw)).perform(typeText(""),closeSoftKeyboard());
        onView(withId(R.id.logAcc)).check(matches(isDisplayed()));
    }

    @Test
    public void testNoti(){
        onView(withId(R.id.loginEmail)).perform(typeText("1232@example.com"), closeSoftKeyboard());
        onView(withId(R.id.loginPsw)).perform(typeText("123456"),closeSoftKeyboard());
        onView(withId(R.id.logAcc)).check(matches(isDisplayed()));
    }

}
package com.example.vulpix.maphelper.controller.activity;

import com.example.vulpix.maphelper.controller.activity.WelcomeActivity;


import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.Rule;
import org.junit.Before;


import android.support.test.runner.AndroidJUnit4;
import android.support.test.rule.ActivityTestRule;
import android.support.test.filters.LargeTest;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;


@RunWith(AndroidJUnit4.class)
@LargeTest
public class WelcomeActivityTest {

    @Rule
    public ActivityTestRule<WelcomeActivity> myActivityTestRule = new ActivityTestRule(WelcomeActivity.class);

    private WelcomeActivity welcomeActivity = null;

    @Before
    public void setUp() throws Exception {
        welcomeActivity = myActivityTestRule.getActivity();
    }

    @Test
    public void WelcomeNote() {

        onView(withText("Choose Your Way")).check(matches(isDisplayed()));
    }
}

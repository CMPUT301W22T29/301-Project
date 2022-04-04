/*
 * Copyright (c) 2022. CMPUT301W22T29
 * Subject to MIT License
 * See full terms at https://github.com/CMPUT301W22T29/QuiRky/blob/main/LICENSE
 */

package com.example.quirky;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Instrumentation;
import android.widget.EditText;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import com.robotium.solo.Solo;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class MainActivityTest {

    private Solo solo;

    @Rule
    public ActivityTestRule rule = new ActivityTestRule(MainActivity.class,true,true);

    @Before
    public void setUp() throws Exception{
        solo = new Solo(InstrumentationRegistry.getInstrumentation(),rule.getActivity());
    }

    @Test
    public void start() throws Exception{
        Activity activity = rule.getActivity();
    }
    //Two situations, One is you never login in before, So it prompts you a username to input
    @Test
    public void checkFirstTimeLogin(){
        solo.assertCurrentActivity("Wrong Activity",MainActivity.class);
        solo.clickOnButton("Login");
        solo.assertCurrentActivity("Wrong Activity", StartingPageActivity.class);
    }
    //The other situation, you have an account in the history of your phone before, so you dont need to create an account anymore
    @Test
    public void checkLogin(){
        solo.assertCurrentActivity("Wrong Activity",MainActivity.class);
        solo.clickOnButton("Login");
        solo.enterText((EditText) solo.getView(R.id.login_frag_input_field), "jiawei3");
        solo.clickOnButton("Confirm");
        solo.assertCurrentActivity("Wrong Activity", StartingPageActivity.class);
    }
    //Needs to be an owner in order to see Owner Setting Button
    @Test
    public void checkSettings(){
        solo.assertCurrentActivity("Wrong Activity",MainActivity.class);
        solo.clickOnButton("Settings");
        solo.assertCurrentActivity("Wrong Activity", OwnerMenu.class);
    }
    /**
     * Close activity after each test
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception{
        solo.finishOpenedActivities();
    }

}


package com.example.vulpix.maphelper.controller.activity;

/*this is the supplement class for the unit test*/

public class TestPresenter {
    private Viewer view;
    private String information;



    public TestPresenter(Viewer view) {
        this.view = view;

    }

    public String onLoginClicked() {
        String email = view.getEmail();
        if (email.isEmpty()) {
            information = "Empty Email";
            return information;
        }
        String password = view.getPsw();
        if (password.isEmpty()) {
            information = "Empty Password";
            return information;
        } else {
            information = "logging in";

        }
        return information;
    }

    public String onSignUpClicked() {
        String name = view.getName();
        if (name.isEmpty()) {
            information = "Empty Name";
            return information;
        }
        String email = view.getEmail();
        if (email.isEmpty()) {
            information = "Empty Email";
            return information;
        }
        String password = view.getPsw();
        if (password.isEmpty()) {
            information = "Empty Password";
            return information;
        } else {
            information = "signing up";

        }
        return information;
    }
}

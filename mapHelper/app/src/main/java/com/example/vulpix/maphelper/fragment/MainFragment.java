package com.example.vulpix.maphelper.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.vulpix.maphelper.R;
import com.example.vulpix.maphelper.controller.activity.AskForHelp;
import com.example.vulpix.maphelper.controller.activity.ChooseSafeMessenger;
import com.example.vulpix.maphelper.controller.activity.MainActivity;
import com.example.vulpix.maphelper.controller.activity.SelfNavActivity;

import java.util.Objects;

/**
 * One of 4 main fragments - Main fragment
 * showing the 2 main function button ASK FOR HELP & HELP YOURSELF
 * and the dialog associated with HELP YOURSELF function to make 2 different choice :
 * start directly / choose a receiver to send safe message
 */
public class MainFragment extends Fragment {

    protected View mView;
    protected Context mContext;
    private com.gc.materialdesign.views.ButtonRectangle helpYourself;
    private com.gc.materialdesign.views.ButtonRectangle askForHelp;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContext = getActivity();
        mView = inflater.inflate(R.layout.fragment_main, container, false);

        //initial frame
        helpYourself = mView.findViewById(R.id.help_yourself);
        askForHelp = mView.findViewById(R.id.ask_for_help);

        // listen the helpYourSelf and askForHelp event
        initListener();

        return mView;
    }

    private void initListener(){

        // handle event for "HelpYourSelf"
        helpYourself.setOnClickListener(v -> {
            showHelpYourselfDialog();
        });

        //handle event for "AskForHelp"
        askForHelp.setOnClickListener(v -> {
            Intent startIntent = new Intent(getContext(), AskForHelp.class);
            startActivity(startIntent);
        });

    }
    private void showHelpYourselfDialog(){

        View dialog_view = LayoutInflater.from(mContext).inflate(R.layout.dialog_custom, null);
        Button cancel = dialog_view.findViewById(R.id.dialog_cancel);
        Button confirm = dialog_view.findViewById(R.id.dialog_confirm);

        AlertDialog.Builder builder =
                new AlertDialog.Builder(mContext);
        builder.setIcon(R.drawable.safe_message);
        builder.setView(dialog_view);

        builder.show();
        cancel.setOnClickListener(view -> {
            Intent intent = new Intent(mContext, SelfNavActivity.class);
                    startActivity(intent);
        });
        confirm.setOnClickListener(view -> {
            Intent startIntent = new Intent(getContext(), ChooseSafeMessenger.class);
                    startActivity(startIntent);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        ((MainActivity) Objects.requireNonNull(getActivity()))
                .setActionBarTitle("RadarScope");
    }

}


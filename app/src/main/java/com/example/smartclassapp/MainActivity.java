package com.example.smartclassapp;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button student,faculty;
        student = (Button) findViewById(R.id.Student);
        faculty = (Button) findViewById(R.id.faculty);
        student.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startpop();
            }
        });

        faculty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                facultypageChange();
            }
        });
    }

    private void facultypageChange() {
        final AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
        View mview = getLayoutInflater().inflate(R.layout.username_popup_faculty,null);

        final EditText subject = (EditText)mview.findViewById(R.id.txt_suject);
        final EditText classname = (EditText)mview.findViewById(R.id.txt_class);
        Button btn_submit = (Button) mview.findViewById(R.id.submit);

        alert.setView(mview);
        final AlertDialog alertDialog = alert.create();
        alertDialog.setCanceledOnTouchOutside(false);

        btn_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if( TextUtils.isEmpty(subject.getText())){

                    subject.setError( "First name is required!" );
                } else if (TextUtils.isEmpty(classname.getText())) {

                    classname.setError( "First name is required!" );
                } else{
                    Intent inext = new Intent(MainActivity.this,FacultyHomeActivity.class);
                    inext.putExtra("subject",subject.getText().toString());
                    inext.putExtra("classname",classname.getText().toString());
                    startActivity(inext);
                    alertDialog.dismiss();
                }

            }
        });
        alertDialog.show();
    }

    private void startpop() {
        final AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
        View mview = getLayoutInflater().inflate(R.layout.username_popup,null);

        final EditText username = (EditText)mview.findViewById(R.id.txt_input);
        final EditText classname = (EditText)mview.findViewById(R.id.txt_class);
        Button btn_submit = (Button) mview.findViewById(R.id.submit);

        alert.setView(mview);
        final AlertDialog alertDialog = alert.create();
        alertDialog.setCanceledOnTouchOutside(false);

        btn_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if( TextUtils.isEmpty(username.getText())){

                    username.setError( "First name is required!" );
                } else if (TextUtils.isEmpty(classname.getText())) {

                    classname.setError( "First name is required!" );
                } else{
                    Intent inext = new Intent(MainActivity.this,StudentHomeActivity.class);
                    inext.putExtra("name",username.getText().toString());
                    inext.putExtra("classname",classname.getText().toString());
                    startActivity(inext);
                    alertDialog.dismiss();
                }

            }
        });
        alertDialog.show();
    }

}
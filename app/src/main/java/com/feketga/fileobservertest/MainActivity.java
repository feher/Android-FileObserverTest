package com.feketga.fileobservertest;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private TextView mOutputText;
    private TextView mEventDump;
    private ScrollView mScrollCommandDump;
    private ScrollView mScrollEventDump;

    private EditText mFilePath;
    private int mPendingAction = -1;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mOutputText = (TextView) findViewById(R.id.output_text);
        mEventDump = (TextView) findViewById(R.id.event_dump);

        mScrollCommandDump = (ScrollView) findViewById(R.id.scroll_command_dump);
        mScrollEventDump = (ScrollView) findViewById(R.id.scroll_event_dump);

        mFilePath = (EditText) findViewById(R.id.file_path);
        mHandler = new Handler();
    }

    @Override
    protected void onResume() {
        super.onResume();

        LocalBroadcastManager.getInstance(this).registerReceiver(
                mMessageReceiver, new IntentFilter(MyService.ACTION_EVENT));

        requestEventDump();
    }

    @Override
    protected void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (MyService.ACTION_EVENT.equals(action)) {
                final String text = intent.getStringExtra(MyService.EXTRA_EVENT_DUMP);
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        addToEventDump(text);
                    }
                });
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        mPendingAction = id;
//        ensurePermissionAndHandleAction();
        handleAction();

        return true;
    }

//    private void ensurePermissionAndHandleAction() {
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
//                != PackageManager.PERMISSION_GRANTED) {
//
//            if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
//                showMessageOKCancel("You need to allow access to Storage",
//                        new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                ActivityCompat.requestPermissions(
//                                        MainActivity.this,
//                                        new String[]{
//                                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                                                Manifest.permission.READ_EXTERNAL_STORAGE},
//                                        100);
//                            }
//                        });
//            } else {
//                ActivityCompat.requestPermissions(
//                        MainActivity.this,
//                        new String[]{
//                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                                Manifest.permission.READ_EXTERNAL_STORAGE},
//                        100);
//            }
//        } else {
//            handleAction();
//        }
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//
//        handleAction();
//    }
//
//    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
//        new AlertDialog.Builder(this)
//                .setMessage(message)
//                .setPositiveButton("OK", okListener)
//                .setNegativeButton("Cancel", null)
//                .create()
//                .show();
//    }

    private void handleAction() {
        switch (mPendingAction) {
            case R.id.action_clear_output:
                clearOutput();
                break;
            case R.id.action_create_file:
                createFile();
                break;
            case R.id.action_write_file:
                writeFile();
                break;
            case R.id.action_read_file:
                readFile();
                break;
            case R.id.action_delete_file:
                deleteFile();
                break;
            case R.id.action_make_directory:
                makeDirectory();
                break;
            case R.id.action_start_observing:
                startObserving();
                break;
            case R.id.action_stop_observing:
                stopObserving();
                break;
            default:
                assert false;
                break;
        }
    }

    private void clearOutput() {
        mOutputText.setText("");

        Intent intent = new Intent(this, MyService.class);
        intent.setAction(MyService.ACTION_COMMAND_CLEAR);
        startService(intent);
    }

    private void requestEventDump() {
        Intent intent = new Intent(this, MyService.class);
        intent.setAction(MyService.ACTION_COMMAND_DUMP);
        startService(intent);
    }

    private void createFile() {
        File file = new File(mFilePath.getText().toString());
        try {
            file.createNewFile();
            appendToCommandDump("CREATE: OK: File created: " + file.getAbsolutePath() + "\n");
        } catch (IOException e) {
            appendToCommandDump("CREATE: ERROR: IOException: " + e.getMessage() + "\n");
        }
    }

    private void writeFile() {
        File file = new File(mFilePath.getText().toString());
        try {
            FileOutputStream stream = new FileOutputStream(file.getAbsolutePath());
            String data = "Hello " + System.currentTimeMillis();
            stream.write(data.getBytes());
            stream.close();
            appendToCommandDump("WRITE: Text: " + data + "\n");
        } catch (FileNotFoundException e) {
            appendToCommandDump("WRITE: ERROR: FileNotFoundException: " + e.getMessage() + "\n");
        } catch (IOException e) {
            appendToCommandDump("WRITE: ERROR: IOException: " + e.getMessage() + "\n");
        }
    }

    private void readFile() {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(mFilePath.getText().toString()));
            StringBuilder sb = new StringBuilder();
            appendToCommandDump("READ: Text: " + br.readLine() + "\n");
        } catch (FileNotFoundException e) {
            appendToCommandDump("READ: ERROR: FileNotFoundException: " + e.getMessage() + "\n");
        } catch (IOException e) {
            appendToCommandDump("READ: ERROR: IOException: " + e.getMessage() + "\n");
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    appendToCommandDump("READ: ERROR: IOException: " + e.getMessage() + "\n");
                }
            }
        }
    }

    private void deleteFile() {
        File file = new File(mFilePath.getText().toString());
        if (file.delete()) {
            appendToCommandDump("DELETE: OK: Deleted: " + file.getAbsolutePath() + "\n");
        } else {
            appendToCommandDump("DELETE: ERROR: Cannot delete: " + file.getAbsolutePath() + "\n");
        }
    }

    private void makeDirectory() {
        File file = new File(mFilePath.getText().toString());
        if (file.mkdirs()) {
            appendToCommandDump("MKDIR: OK: Made: " + file.getAbsolutePath() + "\n");
        } else {
            appendToCommandDump("MKDIR: ERROR: Cannot make: " + file.getAbsolutePath() + "\n");
        }
    }

    private void appendToCommandDump(String text) {
        mOutputText.append(text);
        mScrollCommandDump.fullScroll(ScrollView.FOCUS_DOWN);
    }

    private void addToEventDump(String text) {
        mEventDump.setText(text);
        mScrollEventDump.fullScroll(ScrollView.FOCUS_DOWN);
    }

    private void startObserving() {
        Intent intent = new Intent(this, MyService.class);
        intent.setAction(MyService.ACTION_COMMAND_START);
        intent.putExtra(MyService.EXTRA_FILE_PATH, mFilePath.getText().toString());
        startService(intent);
    }

    private void stopObserving() {
        Intent intent = new Intent(this, MyService.class);
        intent.setAction(MyService.ACTION_COMMAND_STOP);
        startService(intent);
    }

}

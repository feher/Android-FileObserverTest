package com.feketga.fileobservertest;

import android.app.Service;
import android.content.Intent;
import android.os.FileObserver;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

public class MyService extends Service {

    public static final String ACTION_COMMAND_START = "com.feketga.fileobservertest.intent.ACTION_COMMAND_START";
    public static final String ACTION_COMMAND_STOP = "com.feketga.fileobservertest.intent.ACTION_COMMAND_STOP";
    public static final String EXTRA_FILE_PATH = "com.feketga.fileobservertest.intent.EXTRA_FILE_PATH";

    public static final String ACTION_COMMAND_CLEAR = "com.feketga.fileobservertest.intent.ACTION_COMMAND_CLEAR";
    public static final String ACTION_COMMAND_DUMP = "com.feketga.fileobservertest.intent.ACTION_COMMAND_DUMP";

    public static final String ACTION_EVENT = "com.feketga.fileobservertest.intent.ACTION_EVENT";
    public static final String EXTRA_EVENT_DUMP = "com.feketga.fileobservertest.intent.EXTRA_EVENT_DUMP";

    private String mObservedPath;
    private FileObserver mFileObserver;
    private StringBuilder mEventDump = new StringBuilder();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (ACTION_COMMAND_START.equals(intent.getAction())) {
                stopObserving();
                mObservedPath = intent.getStringExtra(EXTRA_FILE_PATH);
                startObserving();
            } else if (ACTION_COMMAND_STOP.equals(intent.getAction())) {
                stopObserving();
            } else if (ACTION_COMMAND_CLEAR.equals(intent.getAction())) {
                mEventDump = new StringBuilder();
                addToDumpAndSend("CLEARED\n");
            } else if (ACTION_COMMAND_DUMP.equals(intent.getAction())) {
                sendDump();
            }
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }


    private void startObserving() {
        int eventMask = FileObserver.ALL_EVENTS;
        mFileObserver = new FileObserver(mObservedPath, eventMask) {
            @Override
            public void onEvent(int event, String path) {
                // Filter out the undocumented IN_IGNORED (32768) event:
                // https://code.google.com/p/android/issues/detail?id=29546&q=FileObserver&colspec=ID%20Type%20Status%20Owner%20Summary%20Stars
                final int FILE_IGNORED = 0x8000;
                if ((event & FILE_IGNORED) != 0) {
                    return;
                }

                // Workaround for a bug in FileObserver:
                // The high-order bits of event may be set for some event types.
                // So, make sure those bits are cleared before looking at the event type.
                event &= FileObserver.ALL_EVENTS;

                switch (event) {
                    case FileObserver.ACCESS:
                        addToDumpAndSend("EVENT: ACCESS: File: " + path + "\n");
                        break;
                    case FileObserver.MODIFY:
                        addToDumpAndSend("EVENT: MODIFY: File: " + path + "\n");
                        break;
                    case FileObserver.CREATE:
                        addToDumpAndSend("EVENT: CREATE: File: " + path + "\n");
                        break;
                    case FileObserver.CLOSE_WRITE:
                        addToDumpAndSend("EVENT: CLOSE_WRITE: File: " + path + "\n");
                        break;
                    case FileObserver.CLOSE_NOWRITE:
                        addToDumpAndSend("EVENT: CLOSE_NOWRITE: File: " + path + "\n");
                        break;
                    case FileObserver.OPEN:
                        addToDumpAndSend("EVENT: OPEN: File: " + path + "\n");
                        break;
                    case FileObserver.MOVED_TO:
                        addToDumpAndSend("EVENT: MOVED_TO: File: " + path + "\n");
                        break;
                    case FileObserver.ATTRIB:
                        addToDumpAndSend("EVENT: ATTRIB: File: " + path + "\n");
                        break;
                    case FileObserver.MOVED_FROM:
                        addToDumpAndSend("EVENT: MOVED_FROM: File: " + path + "\n");
                        break;
                    case FileObserver.DELETE:
                        addToDumpAndSend("EVENT: DELETE: File: " + path + "\n");
                        break;
                    case FileObserver.DELETE_SELF:
                        addToDumpAndSend("EVENT: DELETE_SELF: File: " + path + "\n");
                        break;
                    case FileObserver.MOVE_SELF:
                        addToDumpAndSend("EVENT: MOVE_SELF: File: " + path + "\n");
                        break;
                    default:
                        addToDumpAndSend("EVENT: UNKNOWN (" + event + "): File: " + path + "\n");
                        break;
                }
            }
        };

        mFileObserver.startWatching();
        addToDumpAndSend("START: Observing: " + mObservedPath + "\n");
    }

    private void stopObserving() {
        if (mFileObserver != null) {
            mFileObserver.stopWatching();
            mFileObserver = null;
            addToDumpAndSend("STOP: Observing: " + mObservedPath + "\n");
        }
    }

    private void addToDumpAndSend(final String text) {
        mEventDump.append(text);
        sendDump();
    }

    private void sendDump() {
        Intent intent = new Intent(ACTION_EVENT);
        intent.putExtra(EXTRA_EVENT_DUMP, mEventDump.toString());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

}

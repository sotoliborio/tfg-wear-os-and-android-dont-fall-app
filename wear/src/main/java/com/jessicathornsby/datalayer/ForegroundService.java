package com.jessicathornsby.datalayer;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.List;
import java.util.concurrent.ExecutionException;


public class ForegroundService extends Service {

    public static int FOREGROUND_SERVICE_ID = 101;

    public static String START_ACTION = "com.here.android.example.guidance.fs.action.start";
    public static String STOP_ACTION = "com.here.android.example.guidance.fs.action.stop";

    private TextView textView;
    int receivedMessageNumber = 1;
    int sentMessageNumber = 1;

    private static String CHANNEL = "default";

    @Override
    public void onCreate() {
        super.onCreate();
        initChannels(this.getApplicationContext());

        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.activity_main, null);

        textView =  layout.findViewById(R.id.text);


//Register the local broadcast receiver//

        IntentFilter newFilter = new IntentFilter(Intent.ACTION_SEND);
        Receiver messageReceiver = new Receiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, newFilter);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent.getAction().equals(START_ACTION)) {

            String onClickMessage = "I just sent the handheld a message " + sentMessageNumber++;
            textView.setText(onClickMessage);
            String datapath = "/my_path";
            new SendMessage(datapath, onClickMessage).start();

        } else if (intent.getAction().equals(STOP_ACTION)) {

        }


        return START_NOT_STICKY;
    }

    public void initChannels(Context context) {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(CHANNEL, "Foreground channel",
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Channel for foreground service");
        notificationManager.createNotificationChannel(channel);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Used only in case of bound services.
        return null;
    }

    public class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String onMessageReceived = "I just received a  message from the handheld " + receivedMessageNumber++;
            textView.setText(onMessageReceived);
        }
    }

    class SendMessage extends Thread {
        String path;
        String message;

//Constructor///

        SendMessage(String p, String m) {
            path = p;
            message = m;
        }

//Send the message via the thread. This will send the message to all the currently-connected devices//

        public void run() {

//Get all the nodes//

            Task<List<Node>> nodeListTask =
                    Wearable.getNodeClient(getApplicationContext()).getConnectedNodes();
            try {

//Block on a task and get the result synchronously//

                List<Node> nodes = Tasks.await(nodeListTask);

//Send the message to each device//

                for (Node node : nodes) {
                    Task<Integer> sendMessageTask =
                            Wearable.getMessageClient(getApplicationContext()).sendMessage(node.getId(), path, message.getBytes());

                    try {



                        Integer result = Tasks.await(sendMessageTask);


//Handle the errors//

                    } catch (ExecutionException exception) {

//TO DO//

                    } catch (InterruptedException exception) {

//TO DO//

                    }

                }

            } catch (ExecutionException exception) {

//TO DO//

            } catch (InterruptedException exception) {

//TO DO//

            }
        }
    }
}
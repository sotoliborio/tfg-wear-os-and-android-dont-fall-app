package com.jessicathornsby.datalayer;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.List;
import java.util.concurrent.ExecutionException;


public class HomeFragment extends Fragment {

    private boolean m_foregroundServiceStarted;
    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    Button talkbutton;
    Button llamar;
    TextView textview;
    TextView conexion;
    protected Handler myHandler;
    int receivedMessageNumber = 1;
    int sentMessageNumber = 1;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_home, container, false);
        Switch onOffSwitch = view.findViewById(R.id.switch1);

        talkbutton = view.findViewById(R.id.talkButton);
        llamar = view.findViewById(R.id.llamar);
        textview = view.findViewById(R.id.textView2);
        conexion = view.findViewById(R.id.textView);

        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
        }


        talkbutton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String message = "Sending message.... ";
                textview.setText(message);
                new NewThread("/my_path", message).start();
            }
        });

        llamar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String phone = "655851411";
                String uri = "tel:" + phone.trim();
                Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setData(Uri.parse(uri));
                startActivity(intent);

            }
        });


        onOffSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    startForegroundService();
                } else {
                    stopForegroundService();
                }

            }
        });

        if (!mBluetoothAdapter.isEnabled()) {

            AlertDialog.Builder dialogo1 = new AlertDialog.Builder(getActivity());
            dialogo1.setMessage("¿ Para iniciar la monitorización debe conectar el Bluetooth, desea conectarlo ?");
            dialogo1.setCancelable(false);
            dialogo1.setPositiveButton("Confirmar", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogo1, int id) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, 1);
                }
            });
            dialogo1.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogo1, int id) {
                }
            });
            dialogo1.show();

        }


        myHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                Bundle stuff = msg.getData();
                messageText(stuff.getString("messageText"));
                return true;
            }
        });


        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        Receiver messageReceiver = new Receiver();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(messageReceiver, messageFilter);

        return view;
    }


    private void startForegroundService() {
        if (!m_foregroundServiceStarted) {
            m_foregroundServiceStarted = true;
            Intent startIntent = new Intent(getActivity(), ForegroundService.class);
            startIntent.setAction(ForegroundService.START_ACTION);
            getActivity().getApplicationContext().startService(startIntent);
        }
    }

    private void stopForegroundService() {
        if (m_foregroundServiceStarted) {
            m_foregroundServiceStarted = false;
            Intent stopIntent = new Intent(getActivity(), ForegroundService.class);
            stopIntent.setAction(ForegroundService.STOP_ACTION);
            getActivity().getApplicationContext().startService(stopIntent);
        }
    }


    public void messageText(String newinfo) {
        if (newinfo.compareTo("") != 0) {
            textview.append("\n" + newinfo);
        }
    }


    public class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = "I just received a message from the wearable " + receivedMessageNumber++;;
            textview.setText(message);
            conexion.setText("Conectado");
        }
    }



    public void sendmessage(String messageText) {
        Bundle bundle = new Bundle();
        bundle.putString("messageText", messageText);
        Message msg = myHandler.obtainMessage();
        msg.setData(bundle);
        myHandler.sendMessage(msg);

    }


    class NewThread extends Thread {
        String path;
        String message;

        NewThread(String p, String m) {
            path = p;
            message = m;
        }


        public void run() {

            Task<List<Node>> wearableList =
                    Wearable.getNodeClient(getActivity().getApplicationContext()).getConnectedNodes();
            try {

                List<Node> nodes = Tasks.await(wearableList);
                for (Node node : nodes) {
                    Task<Integer> sendMessageTask =
                            Wearable.getMessageClient(getContext()).sendMessage(node.getId(), path, message.getBytes());

                    try {

                        Integer result = Tasks.await(sendMessageTask);
                        sendmessage("I just sent the wearable a message " + sentMessageNumber++);

                    } catch (ExecutionException exception) {

                        //TO DO: Handle the exception//


                    } catch (InterruptedException exception) {

                    }

                }

            } catch (ExecutionException exception) {

                //TO DO: Handle the exception//

            } catch (InterruptedException exception) {

                //TO DO: Handle the exception//
            }

        }
    }


}

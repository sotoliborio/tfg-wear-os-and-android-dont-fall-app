package com.jessicathornsby.datalayer;

import java.util.ArrayList;
import java.util.List;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.Menu;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("NewApi")
public class SensorListener extends Activity implements SensorEventListener  {
    private TextView txtX;
    private TextView txtY;
    private TextView txtZ;
    private TextView txtDetail;
    private ImageView stateImg;

    private boolean speech_state = false;
    private int speech_count = 0;
    private int move_count = 0;
    private boolean move_start_count = false;
    private boolean sound_active = true;
    private int fall_counter = 0;
    private boolean log_active = false;

    private static final int THRESHOLD_MIN = 2;
    private static final int THRESHOLD_MAX = 15;
    private static final int THRESHOLD_MOVE = 10;
    private static final int THRESHOLD_DEVICE_POSITION_TIME = 2500;
    private boolean THRESHOLD_DEVICE_POSITION_OK = false;
    private static final int THRESHOLD_SPEECH_COUNTER = 5;
    private boolean THRESHOLD_MIN_OK;
    private boolean THRESHOLD_MAX_OK;
    private int THRESHOLD_COUNTER;

    // variables for detect device position
    private float[] matrixR = new float[9];
    private float[] matrixI = new float[9];
    private float[] matrixValues = new float[3];
    private float[] remapMatrix = new float[9];
    private float[] valuesAccelerometer = new float[3];
    private float[] valuesMagneticField = new float[3];
    private double azimuth = 0;
    private double pitch = 0;
    private double roll = 0;
    private boolean DEVICE_POSITION_TIME_RUNNING = false;

    // variable to play events sounds
    //public MediaPlayer mp_possible_fall,mp_fall;


    public static final int MULTIPLE_PERMISSIONS = 10; // code you want.

    String[] permissions = new String[] {
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.RECORD_AUDIO
    };

    /*
     * time smoothing constant for low-pass filter
     * 0 <= alpha <= 1 ; a smaller value basically means more smoothing
     * See: http://en.wikipedia.org/wiki/Low-pass_filter#Discrete-time_realization
     */
    static final float ALPHA = 0.15f;

    private long lastUpdate=0;
    private long lastUpdatePosition=0;
    /**
     * Sender email
     */
    public static String EMAIL_STRING = "teu-email@gmail.com";
    public static String PASSWORD_STRING = "tua-senha";


    // Sensor variables
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagneticField;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sensor_main);

        if (checkPermissions()){
            // permissions granted.
        } else {
            // show dialog informing them that we lack certain permissions
        }

        //mp_possible_fall=MediaPlayer.create(getBaseContext(), R.raw.mp0_possible_fall_);
        //mp_fall=MediaPlayer.create(getBaseContext(), R.raw.mp0_fall_);


        // we start variables
        txtX = (TextView) findViewById(R.id.txtX);
        txtY = (TextView) findViewById(R.id.txtY);
        txtZ = (TextView) findViewById(R.id.txtZ);
        txtDetail = (TextView) findViewById(R.id.txtDetail);
        if(!log_active){
            stateImg = (ImageView) findViewById(R.id.stateImg);
            txtDetail.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
            txtDetail.setText(R.string.sensor_monitoring);
        }

        THRESHOLD_MIN_OK=false;
        THRESHOLD_MAX_OK=false;
        THRESHOLD_COUNTER=0;
        speech_count=0;
        speech_state=false;
        move_count=0;
        move_start_count=false;

        // we register Sensors
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mMagneticField, SensorManager.SENSOR_DELAY_GAME);
    }

    private boolean checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p:permissions) {
            result = ContextCompat.checkSelfPermission(this,p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MULTIPLE_PERMISSIONS:{
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    // permissions granted.
                } else {
                    // no permissions granted.
                }
                return;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    protected void onResume() {
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mMagneticField, SensorManager.SENSOR_DELAY_GAME);
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        mSensorManager.unregisterListener(this);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mSensorManager.unregisterListener(this);
        super.onDestroy();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            //valuesAccelerometer = lowPass( event.values.clone(), valuesAccelerometer );
            valuesAccelerometer = event.values.clone();
        }

        if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            valuesMagneticField = lowPass( event.values.clone(), valuesMagneticField );
        }

        boolean success = SensorManager.getRotationMatrix(
                matrixR,
                matrixI,
                valuesAccelerometer,
                valuesMagneticField);

        if(success){
            SensorManager.remapCoordinateSystem(matrixR, SensorManager.AXIS_X, SensorManager.AXIS_Z, remapMatrix);
            SensorManager.getOrientation(matrixR, matrixValues);

            azimuth = Math.toDegrees(matrixValues[0]);
            pitch = Math.toDegrees(matrixValues[1]);
            roll = Math.toDegrees(matrixValues[2]);

            Float x = valuesAccelerometer[0];
            Float y = valuesAccelerometer[1];
            Float z = valuesAccelerometer[2];

            txtX.setText("Posición X: " + x.intValue());
            txtY.setText("Posición Y: " + y.intValue());
            txtZ.setText("Posición Z: " + z.intValue());

            long curTime;
            curTime = System.currentTimeMillis();

            // updates every 20ms. SENSOR_DELAY_GAME 48.86 Hz = 20000 (20ms)
            if ((curTime - lastUpdate) > 20) {

                lastUpdate = curTime;

                // function to get acceleration
                double ACCELERATION = Math.round(Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2)));

                // if we have an acceleration detected (didn't count force of gravity)
                if(ACCELERATION>=13 || ACCELERATION<=6){
                    if(log_active){
                        txtDetail.setText((int)ACCELERATION+" ("+((Double) azimuth).intValue()+","+((Double) pitch).intValue()+","+((Double) roll).intValue()+")"+"\n"+txtDetail.getText(), TextView.BufferType.SPANNABLE);
                    }
                    if(move_start_count==true){
                        move_count++;
                    }
                }

                if (ACCELERATION<=THRESHOLD_MIN) {
                    THRESHOLD_MIN_OK=true;
                }

                if(THRESHOLD_MIN_OK) {
                    THRESHOLD_COUNTER++;
                    if(ACCELERATION>=THRESHOLD_MAX) {
                        THRESHOLD_MAX_OK=true;
                    }
                }

                if(THRESHOLD_MIN_OK && THRESHOLD_MAX_OK){

                    if ((curTime - lastUpdatePosition) > THRESHOLD_DEVICE_POSITION_TIME) {
                        lastUpdatePosition  = curTime;
                        if(DEVICE_POSITION_TIME_RUNNING){
                            if(log_active){
                                txtDetail.setText("------ Stop 2.5s--------"+"\n"+txtDetail.getText(), TextView.BufferType.SPANNABLE);
                            }
                            DEVICE_POSITION_TIME_RUNNING = false;
                            THRESHOLD_DEVICE_POSITION_OK = true;
                        }else{
                            if(log_active){
                                txtDetail.setText("------ Start 2.5s--------"+"\n"+txtDetail.getText(), TextView.BufferType.SPANNABLE);
                            }
                            DEVICE_POSITION_TIME_RUNNING = true;
                        }
                    }
                }


                // if the THRESHOLD_MIN and THRESHOLD_MAX pass, and we are not in speech_state
                // and we have detect that device is in a horizontal position (position after a fall)
                if (THRESHOLD_MIN_OK &&
                        THRESHOLD_MAX_OK &&
                        THRESHOLD_DEVICE_POSITION_OK &&
                        speech_state==false &&
                        move_start_count==false &&
                        (pitch>=-40 && pitch <=40) ) {

                    // when LOG is active, we don't show any image
                    if(!log_active){
                        stateImg.setImageResource(R.drawable.falling_icon);
                        txtDetail.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
                        txtDetail.setText(R.string.sensor_fall_possible);
                    }

                    if(log_active){
                        fall_counter++;
                        txtDetail.setText("------ FALL DETECTED ("+fall_counter+") --------"+"\n"+txtDetail.getText(), TextView.BufferType.SPANNABLE);
                    }

                    startDetectionAlarm();
                    THRESHOLD_COUNTER=0;
                    THRESHOLD_MIN_OK=false;
                    THRESHOLD_MAX_OK=false;
                    THRESHOLD_DEVICE_POSITION_OK=false;
                }

                if(speech_state &&  move_count>THRESHOLD_MOVE){
                    stopDetectionAlarm();
                }

                // if after 1500ms (20x75) we didn't detected the impact, we reset counters
                if (THRESHOLD_COUNTER>75 && !DEVICE_POSITION_TIME_RUNNING) {
                    THRESHOLD_COUNTER=0;
                    THRESHOLD_MIN_OK=false;
                    THRESHOLD_MAX_OK=false;
                    THRESHOLD_DEVICE_POSITION_OK=false;
                }

            }
        }
    }

    private void startDetectionAlarm(){
        speech_state=true;
        move_start_count=true;
        THRESHOLD_DEVICE_POSITION_OK = false;


        // we play the alert sound
        Toast.makeText(this, R.string.sensor_fall_possible, Toast.LENGTH_SHORT).show();
        /*if(sound_active){
            mp_possible_fall.start();
            while(mp_possible_fall.isPlaying());
        }*/
        // throw voice recognition dialog
        SpeechRecognitionHelper.run(this);

    }

    private void stopDetectionAlarm(){
        Toast.makeText(this, R.string.sensor_moviment_detected, Toast.LENGTH_SHORT).show();
        stopMoveCounter();
        stopSpeechCounter();
        // when LOG is active, we don't show any image
        if(!log_active){
            stateImg.setImageResource(R.drawable.stand_icon);
            txtDetail.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
            txtDetail.setText(R.string.sensor_moviment_detected);
        }
        THRESHOLD_DEVICE_POSITION_OK = false;
    }


    private void ThrowAlertSystem(){
        Toast.makeText(this, R.string.sensor_fall_detected, Toast.LENGTH_SHORT).show();
        // we play alert sound for a detected fall
        /*if(sound_active){
            mp_fall.start();
            while(mp_fall.isPlaying());
        }*/

        //sendMail(confData.getEmail(), getResources().getString(R.string.email_alert_subject), getResources().getString(R.string.email_alert_body)+" ("+confData.getName()+") ("+confData.getAddress()+")" );
        makeAlertCall("655851411");

        if(log_active){
            txtDetail.setText("(Alerta enviado) "+"\n"+txtDetail.getText(), TextView.BufferType.SPANNABLE);
        }
    }


    /**
     * Handle the results from the voice recognition activity.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        // if the voice recognition system returns a success recognition string
        if (requestCode == 1234 && resultCode == RESULT_OK && speech_state){
            int i=0;
            String item;

            // Populate the wordsList with the String values the recognition engine thought it heard
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            for (i = 0; i < matches.size(); i++){
                item = matches.get(i);
                if(log_active){
                    txtDetail.setText(item+"\n"+txtDetail.getText(), TextView.BufferType.SPANNABLE);
                }
            }
            // throw alert system if they said an ALERT WORD
            if(matches.contains("me ajuda") ||
                    matches.contains("help") ||
                    matches.contains("socorro") ||
                    matches.contains("ayuda") ||
                    matches.contains("ajuda") ||
                    matches.contains("ayuda me") ||
                    matches.contains("help me") ){
                showFallImg();
                stopSpeechCounter();
                stopMoveCounter();
                ThrowAlertSystem();
                backToMonitoringImg();
            }else if(matches.contains("o que �") ||
                    matches.contains("okei") ||
                    matches.contains("ok") ||
                    matches.contains("oque") ||
                    matches.contains("estou bem") ||
                    matches.contains("estoy bien") ||
                    matches.contains("t� muito bem") ||
                    matches.contains("eu estou bem") ||
                    matches.contains("bem")    ){
                stopSpeechCounter();
                stopMoveCounter();
                backToMonitoringImg();
            }else{
                // or we increment speech counter
                speech_count++;
                if(speech_count>=THRESHOLD_SPEECH_COUNTER){
                    showFallImg();
                    stopSpeechCounter();
                    stopMoveCounter();
                    ThrowAlertSystem();
                    backToMonitoringImg();
                }else{
                    // throw the recognition dialog again if we can't detect an ALERT WORD
                    if(speech_state){
                        SpeechRecognitionHelper.run(this);
                    }else{
                        stopSpeechCounter();
                        stopMoveCounter();
                        backToMonitoringImg();
                    }
                }
            }
        }else{
            speech_count++;
            if(speech_count>=THRESHOLD_SPEECH_COUNTER ){
                showFallImg();
                stopSpeechCounter();
                stopMoveCounter();
                ThrowAlertSystem();
                backToMonitoringImg();
            }else{
                if(speech_state){
                    SpeechRecognitionHelper.run(this);
                }else{
                    stopSpeechCounter();
                    stopMoveCounter();
                    backToMonitoringImg();
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Set FALL img
     */
    private void showFallImg(){
        // when LOG is active, we don't show any image
        if(!log_active){
            stateImg.setImageResource(R.drawable.fall_icon);
            txtDetail.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
            txtDetail.setText(R.string.sensor_fall_detected);
        }
    }

    /**
     * Restart monitoring img
     */
    private void backToMonitoringImg(){
        // when LOG is active, we don't show any image
        if(!log_active){
            stateImg.setImageResource(R.drawable.stand_icon);
            txtDetail.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
            txtDetail.setText(R.string.sensor_monitoring);
        }
    }

    private void stopSpeechCounter(){
        speech_state = false;
        speech_count=0;
    }

    private void stopMoveCounter(){
        move_start_count=false;
        move_count=0;
    }


    /**
     * Pass a phone number, and call a CALL_INTENT
     * @param phone
     */
    @SuppressLint("MissingPermission")
    private void makeAlertCall(String phone){
        String uri = "tel:" + phone.trim() ;
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse(uri));
        startActivity(intent);
    }


    /*
     * SEND ALERT EMAIL
     * @param email
     * @param subject
     * @param messageBody
     */
    /*private void sendMail(String email, String subject, String messageBody) {
        GMailSender sender = new GMailSender(EMAIL_STRING, PASSWORD_STRING);
        try {
            sender.sendMail(subject,
                    messageBody,
                    EMAIL_STRING,
                    email);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Function to smooth values from a SENSOR
     * @see http://en.wikipedia.org/wiki/Low-pass_filter#Algorithmic_implementation
     * @see http://developer.android.com/reference/android/hardware/SensorEvent.html#values
     */
    protected float[] lowPass( float[] input, float[] output ) {
        if ( output == null ) return input;

        for ( int i=0; i<input.length; i++ ) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }


}
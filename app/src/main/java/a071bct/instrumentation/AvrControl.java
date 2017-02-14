package a071bct.instrumentation;

import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.fingerprint.FingerprintManager;
import android.icu.text.LocaleDisplayNames;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.AsyncTask;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.InputMismatchException;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class AvrControl extends AppCompatActivity {





   // private static final String TAG = AvrControl.class.getSimpleName();
    LinearLayout linearlayout;
    Handler bluetoothIn;
    private static final String TAG = AvrControl.class.getSimpleName();

    private static final String DIALOG_FRAGMENT_TAG = "myFragment";
    private static final String SECRET_MESSAGE = "Very secret message";
    private static final String KEY_NAME_NOT_INVALIDATED = "key_not_invalidated";
    static final String DEFAULT_KEY_NAME = "default_key";

    private KeyStore mKeyStore;
    private KeyGenerator mKeyGenerator;
    private SharedPreferences mSharedPreferences;
    //boolean doorstate=false;


    Button btnDis,opdoor,cldoor,ldoor,uldoor;
    TextView status,mytext;
    String address = null;
    String datatoopen="i",datatoclose="j";
    public String checkwhichpressed;
    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    InputStream inpsteream;
    final int handlerState = 0;        				 //used to identify handler message
    private ConnectedThread mConnectedThread;
    private StringBuilder recDataString = new StringBuilder();
    //private Context context;
    //spp uuid

    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       // setContentView(R.layout.activity_avr_control);

        //receive the address of the bluetooth device
        Intent newint = getIntent();
        address = newint.getStringExtra(MainPage.EXTRA_ADDRESS);

//view of the ledControl layout
        setContentView(R.layout.activity_avr_control);
       // PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit().putString("MYADDRESS", address).commit();



        opdoor= (Button)findViewById(R.id.opendoor);
        cldoor = (Button)findViewById(R.id.closedoor);
        btnDis = (Button)findViewById(R.id.disconnect);
        ldoor=(Button)findViewById(R.id.lockdoor);
        uldoor=(Button)findViewById(R.id.unlockdoor);
        status=(TextView)findViewById(R.id.textStatus);
        mytext=(TextView)findViewById(R.id.textreceiver);
        linearlayout=(LinearLayout)findViewById(R.id.activity_avr_control);

        //for finger print
        try {
            mKeyStore = KeyStore.getInstance("AndroidKeyStore");
        } catch (KeyStoreException e) {
            throw new RuntimeException("Failed to get an instance of KeyStore", e);
        }
        try {
            mKeyGenerator = KeyGenerator
                    .getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException("Failed to get an instance of KeyGenerator", e);
        }
        Cipher defaultCipher;
        Cipher cipherNotInvalidated;
        try {
            defaultCipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                    + KeyProperties.BLOCK_MODE_CBC + "/"
                    + KeyProperties.ENCRYPTION_PADDING_PKCS7);
            cipherNotInvalidated = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                    + KeyProperties.BLOCK_MODE_CBC + "/"
                    + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException("Failed to get an instance of Cipher", e);
        }
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        KeyguardManager keyguardManager = getSystemService(KeyguardManager.class);
        FingerprintManager fingerprintManager = getSystemService(FingerprintManager.class);

       // Button purchaseButton = (Button) findViewById(R.id.purchase_button);
       // Button purchaseButtonNotInvalidated = (Button) findViewById(R.id.purchase_button_not_invalidated);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            purchaseButtonNotInvalidated.setEnabled(true);
//            purchaseButtonNotInvalidated.setOnClickListener(
//                    new PurchaseButtonClickListener(cipherNotInvalidated,
//                            KEY_NAME_NOT_INVALIDATED));
        } else {
            // Hide the purchase button which uses a non-invalidated key
            // if the app doesn't work on Android N preview
          //  purchaseButtonNotInvalidated.setVisibility(View.GONE);
//            findViewById(R.id.purchase_button_not_invalidated_description)
//                    .setVisibility(View.GONE);
        }

        if (!keyguardManager.isKeyguardSecure()) {
            // Show a message that the user hasn't set up a fingerprint or lock screen.
            Toast.makeText(this,
                    "Secure lock screen hasn't set up.\n"
                            + "Go to 'Settings -> Security -> Fingerprint' to set up a fingerprint",
                    Toast.LENGTH_LONG).show();
           // purchaseButton.setEnabled(false);
            opdoor.setEnabled(false);
            cldoor.setEnabled(false);
           // purchaseButtonNotInvalidated.setEnabled(false);
            return;
        }

        // Now the protection level of USE_FINGERPRINT permission is normal instead of dangerous.
        // See http://developer.android.com/reference/android/Manifest.permission.html#USE_FINGERPRINT
        // The line below prevents the false positive inspection from Android Studio
        // noinspection ResourceType
        if (!fingerprintManager.hasEnrolledFingerprints()) {
           // purchaseButton.setEnabled(false);
            opdoor.setEnabled(false);
            cldoor.setEnabled(false);
            // This happens when no fingerprints are registered.
            Toast.makeText(this,
                    "Go to 'Settings -> Security -> Fingerprint' and register at least one fingerprint",
                    Toast.LENGTH_LONG).show();
            return;
        }
        createKey(DEFAULT_KEY_NAME, true);
        createKey(KEY_NAME_NOT_INVALIDATED, false);
        //purchaseButton.setEnabled(true);
        opdoor.setEnabled(true);
        cldoor.setEnabled(true);
        opdoor.setOnClickListener(new PurchaseButtonClickListener(defaultCipher,DEFAULT_KEY_NAME,datatoopen));
        cldoor.setOnClickListener(new PurchaseButtonClickListener(defaultCipher,DEFAULT_KEY_NAME,datatoclose));
        //purchaseButton.setOnClickListener(new PurchaseButtonClickListener(defaultCipher, DEFAULT_KEY_NAME));





//call the widgtes



        bluetoothIn = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == handlerState) {										//if message is what we want
                    String readMessage = (String) msg.obj;                                                                // msg.arg1 = bytes from connect thread
                    recDataString.append(readMessage);      								//keep appending to string until ~

                    int length=recDataString.length();
                    if(length>0) {
                        String dataInPrint = recDataString.substring(0, length);    // extract string
                        mytext.setText("Data Received: "+dataInPrint);
                        if (readMessage.charAt(0)=='m') {
                            status.setText("Noone at the door");
                            //createNotification("Someone at the door",1);
                            //clear all string data
                            recDataString.delete(0,length);
                            //readMessage.replace('m','n');
                            readMessage="n";
                        } else {
                            status.setText("Someone at the door");
                            //createNotification("Noone",2);
                        }
                        recDataString.delete(0, recDataString.length());
                       // readMessage="n";
                        dataInPrint=" ";
                    }
//                    int endOfLineIndex = recDataString.indexOf("~");                    // determine the end-of-line
//                    if (endOfLineIndex > 0) {                                           // make sure there data before ~
//                        String dataInPrint = recDataString.substring(0, endOfLineIndex);    // extract string
//                        txtString.setText("Data Received = " + dataInPrint);
//                        int dataLength = dataInPrint.length();							//get length of data received
//                        txtStringLength.setText("String Length = " + String.valueOf(dataLength));
//
//                        if (recDataString.charAt(0) == '#')								//if it starts with # we know it is what we are looking for
//                        {
//                            String sensor0 = recDataString.substring(1, 5);             //get sensor value from string between indices 1-5
//                            String sensor1 = recDataString.substring(6, 10);            //same again...
//                            String sensor2 = recDataString.substring(11, 15);
//                            String sensor3 = recDataString.substring(16, 20);
//
//                            sensorView0.setText(" Sensor 0 Voltage = " + sensor0 + "V");	//update the textviews with sensor values
//                            sensorView1.setText(" Sensor 1 Voltage = " + sensor1 + "V");
//                            sensorView2.setText(" Sensor 2 Voltage = " + sensor2 + "V");
//                            sensorView3.setText(" Sensor 3 Voltage = " + sensor3 + "V");
//                        }
//                        recDataString.delete(0, recDataString.length()); 					//clear all string data
//                        // strIncom =" ";
//                        dataInPrint = " ";
//                    }
                }
            }
        };

        new ConnectBT().execute();



        btnDis.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Disconnect(); //close connection
            }
        });
//        opdoor.setOnClickListener(new View.OnClickListener()
//        {
//            @Override
//            public void onClick(View v)
//            {
//                openthedoor();
//              //  turnOnLed();      //method to turn on
//            }
//        });
//        cldoor.setOnClickListener(new View.OnClickListener()
//        {
//            @Override
//            public void onClick(View v)
//            {
//                closethedoor();
//                //  turnOnLed();      //method to turn on
//            }
//        });

        ldoor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                lockthedoor();
            }
        });
        uldoor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                unlockthedoor();
            }
        });



    }

    /**
     * Initialize the {@link Cipher} instance with the created key in the
     * {@link #createKey(String, boolean)} method.
     *
     * @param keyName the key name to init the cipher
     * @return {@code true} if initialization is successful, {@code false} if the lock screen has
     * been disabled or reset after the key was generated, or if a fingerprint got enrolled after
     * the key was generated.
     */
    private boolean initCipher(Cipher cipher, String keyName) {
        try {
            mKeyStore.load(null);
            SecretKey key = (SecretKey) mKeyStore.getKey(keyName, null);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return true;
        } catch (KeyPermanentlyInvalidatedException e) {
            return false;
        } catch (KeyStoreException | CertificateException | UnrecoverableKeyException | IOException
                | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to init Cipher", e);
        }
    }

    /**
     * Proceed the purchase operation
     *
     * @param withFingerprint {@code true} if the purchase was made by using a fingerprint
     * @param cryptoObject the Crypto object
     */
    public void onPurchased(boolean withFingerprint,
                            @Nullable FingerprintManager.CryptoObject cryptoObject) {
        if (withFingerprint) {
            // If the user has authenticated with fingerprint, verify that using cryptography and
            // then show the confirmation message.
            assert cryptoObject != null;
            tryEncrypt(cryptoObject.getCipher());
        } else {
            // Authentication happened with backup password. Just show the confirmation message.
            showConfirmation(null);
        }
    }

    // Show confirmation, if fingerprint was used show crypto information.
    private void showConfirmation(byte[] encrypted) {

        //to do opening and closing of the door
        if(checkwhichpressed==datatoopen){
            openthedoor();
            TextView t=(TextView)findViewById(R.id.confirmation_message);
            t.setVisibility(View.VISIBLE);
            t.setText("Data send: "+datatoopen);
            opdoor.setEnabled(false);
            cldoor.setEnabled(true);
            //Toast.makeText(this,datatoopen, Toast.LENGTH_SHORT).show();

        }
        else if(checkwhichpressed==datatoclose){
            closethedoor();
            TextView t=(TextView)findViewById(R.id.confirmation_message);
            t.setVisibility(View.VISIBLE);
            t.setText("Data send: "+datatoclose);
            opdoor.setEnabled(true);
            cldoor.setEnabled(false);
            //Toast.makeText(this,datatoclose, Toast.LENGTH_SHORT).show();
        }
//        if (encrypted != null) {
//            TextView v = (TextView) findViewById(R.id.encrypted_message);
//            v.setVisibility(View.VISIBLE);
//            v.setText(Base64.encodeToString(encrypted, 0 /* flags */));
//        }
    }

    /**
     * Tries to encrypt some data with the generated key in {@link #createKey} which is
     * only works if the user has just authenticated via fingerprint.
     */
    private void tryEncrypt(Cipher cipher) {
        try {
            byte[] encrypted = cipher.doFinal(SECRET_MESSAGE.getBytes());
            showConfirmation(encrypted);
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            Toast.makeText(this, "Failed to encrypt the data with the generated key. "
                    + "Retry the purchase", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Failed to encrypt the data with the generated key." + e.getMessage());
        }
    }

    /**
     * Creates a symmetric key in the Android Key Store which can only be used after the user has
     * authenticated with fingerprint.
     *
     * @param keyName the name of the key to be created
     * @param invalidatedByBiometricEnrollment if {@code false} is passed, the created key will not
     *                                         be invalidated even if a new fingerprint is enrolled.
     *                                         The default value is {@code true}, so passing
     *                                         {@code true} doesn't change the behavior
     *                                         (the key will be invalidated if a new fingerprint is
     *                                         enrolled.). Note that this parameter is only valid if
     *                                         the app works on Android N developer preview.
     *
     */
    public void createKey(String keyName, boolean invalidatedByBiometricEnrollment) {
        // The enrolling flow for fingerprint. This is where you ask the user to set up fingerprint
        // for your flow. Use of keys is necessary if you need to know if the set of
        // enrolled fingerprints has changed.
        try {
            mKeyStore.load(null);
            // Set the alias of the entry in Android KeyStore where the key will appear
            // and the constrains (purposes) in the constructor of the Builder

            KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(keyName,
                    KeyProperties.PURPOSE_ENCRYPT |
                            KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    // Require the user to authenticate with a fingerprint to authorize every use
                    // of the key
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7);

            // This is a workaround to avoid crashes on devices whose API level is < 24
            // because KeyGenParameterSpec.Builder#setInvalidatedByBiometricEnrollment is only
            // visible on API level +24.
            // Ideally there should be a compat library for KeyGenParameterSpec.Builder but
            // which isn't available yet.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                builder.setInvalidatedByBiometricEnrollment(invalidatedByBiometricEnrollment);
            }
            mKeyGenerator.init(builder.build());
            mKeyGenerator.generateKey();
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException
                | CertificateException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private class PurchaseButtonClickListener implements View.OnClickListener {

        Cipher mCipher;
        String mKeyName;
        String value;

        PurchaseButtonClickListener(Cipher cipher, String keyName,String val) {
            mCipher = cipher;
            mKeyName = keyName;
            value =val;
        }

        @Override
        public void onClick(View view) {
            findViewById(R.id.confirmation_message).setVisibility(View.GONE);
            findViewById(R.id.encrypted_message).setVisibility(View.GONE);
            checkwhichpressed=value;


            // Set up the crypto object for later. The object will be authenticated by use
            // of the fingerprint.
            if (initCipher(mCipher, mKeyName)) {

                // Show the fingerprint dialog. The user has the option to use the fingerprint with
                // crypto, or you can fall back to using a server-side verified password.
                FingerprintAuthenticationDialogFragment fragment
                        = new FingerprintAuthenticationDialogFragment();
                fragment.setCryptoObject(new FingerprintManager.CryptoObject(mCipher));
                boolean useFingerprintPreference = mSharedPreferences
                        .getBoolean(getString(R.string.use_fingerprint_to_authenticate_key),
                                true);
                if (useFingerprintPreference) {
                    fragment.setStage(
                            FingerprintAuthenticationDialogFragment.Stage.FINGERPRINT);
                } else {
                    fragment.setStage(
                            FingerprintAuthenticationDialogFragment.Stage.PASSWORD);
                }
                fragment.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);
            } else {
                // This happens if the lock screen has been disabled or or a fingerprint got
                // enrolled. Thus show the dialog to authenticate with their password first
                // and ask the user if they want to authenticate with fingerprints in the
                // future
                FingerprintAuthenticationDialogFragment fragment
                        = new FingerprintAuthenticationDialogFragment();
                fragment.setCryptoObject(new FingerprintManager.CryptoObject(mCipher));
                fragment.setStage(
                        FingerprintAuthenticationDialogFragment.Stage.NEW_FINGERPRINT_ENROLLED);
                fragment.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);
            }
        }
    }



    private void lockthedoor() {
        if (btSocket!=null)
        {
            try
            {
                btSocket.getOutputStream().write("a".toString().getBytes());
//                        .toString().getBytes());
            }
            catch (IOException e)
            {
                msg("Error");
            }
        }
        else{
            Snackbar.make(linearlayout,"Not available",Snackbar.LENGTH_LONG).show();
        }
    }

    private void unlockthedoor(){
        if (btSocket!=null)
        {
            try
            {
                btSocket.getOutputStream().write("b".toString().getBytes());
            }
            catch (IOException e)
            {
                msg("Error");
            }
        }
        else{
            Snackbar.make(linearlayout,"Not available",Snackbar.LENGTH_LONG).show();
        }

    }
    private void closethedoor() {
        if (btSocket!=null)
        {
            try
            {
                btSocket.getOutputStream().write("j".toString().getBytes());
            }
            catch (IOException e)
            {
                msg("Error");
            }
        }
        else{
            Snackbar.make(linearlayout,"Not available",Snackbar.LENGTH_LONG).show();
        }
    }

    private void openthedoor() {
        if (btSocket!=null)
        {
            try
            {
                btSocket.getOutputStream().write("i".toString().getBytes());
            }
            catch (IOException e)
            {
                msg("Error");
            }
        }

    }

    private void msg(String s)
    {
        //Snackbar.make(linearlayout,s,Snackbar.LENGTH_LONG).show();
        Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();
    }
    private void Disconnect()
    {
        if (btSocket!=null) //If the btSocket is busy
        {
            try
            {
                btSocket.close(); //close connection
            }
            catch (IOException e)
            { msg("Error");}
        }

//        Toast.makeText(this, "", Toast.LENGTH_SHORT).show();



        Snackbar.make(linearlayout,"Successfully Disconnected",Snackbar.LENGTH_LONG).show();

        finish(); //return to the first layout
    }

    private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute()
        {
            progress = ProgressDialog.show(AvrControl.this, "Connecting...", "Please wait!!! LOL");  //show a progress dialog
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {
            try
            {
                if (btSocket == null || !isBtConnected)
                {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);//connects to the device's address and checks if it's available
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();//start connection






                }
            }
            catch (IOException e)
            {
                ConnectSuccess = false;//if the try failed, you can check the exception here
            }



            return null;
        }
        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            if (!ConnectSuccess)
            {
                msg("Ha Ha Ha Connection Failed. Is it a SPP Bluetooth? Try again.");
                finish();
            }
            else
            {
                msg("Connected LOL.");
                mConnectedThread = new ConnectedThread(btSocket);
                mConnectedThread.start();

                //I send a character when resuming.beginning transmission to check device is connected
                //If it is not an exception will be thrown in the write method and finish() will be called
                //mConnectedThread.write("x");
                isBtConnected = true;
            }
            progress.dismiss();
        }
    }



    @Override
    public void onPause()
    {
        super.onPause();
        try
        {
            //Don't leave Bluetooth sockets open when leaving activity
            btSocket.close();
        } catch (IOException e2) {
            //insert code to deal with this
        }
    }

    //create new class for connect thread
    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        //creation of the connect thread
        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                //Create I/O streams for connection
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }


        public void run() {
            byte[] buffer = new byte[3];
            int bytes;

            // Keep looping to listen for received messages
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);            //read bytes from input buffer
                    String readMessage = new String(buffer, 0, bytes);
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();




                    // Send the obtained bytes to the UI Activity via handler
                   // bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    //break;
                }
            }
        }

        //write method
        public void write(String input) {
            byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
            } catch (IOException e) {
                //if you cannot write, close the application
                Toast.makeText(getBaseContext(), "Connection Failure", Toast.LENGTH_LONG).show();
                finish();

            }
        }


    }
    public void createNotification(String toshow,int id_number){

        //for notification
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setAutoCancel(true)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("Write Nepal")
                        .setContentText(toshow);


// Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, MainPage.class);

// The stack builder object will contain an artificial back stack for the
// started Activity.
// This ensures that navigating backward from the Activity leads out of
// your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
// Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainPage.class);
// Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
// mId allows you to update the notification later on.
        mBuilder.getNotification().flags|= Notification.FLAG_AUTO_CANCEL;
        mNotificationManager.notify(id_number, mBuilder.build());
        //NOTIFY_ME_ID+=1;


        Log.e("sasd","notification created");
    }
}





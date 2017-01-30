package a071bct.instrumentation;

import android.app.KeyguardManager;
import android.content.SharedPreferences;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.security.keystore.KeyProperties;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.AsyncTask;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;

public class AvrControl extends AppCompatActivity {

    private static final String TAG = AvrControl.class.getSimpleName();

    private static final String DIALOG_FRAGMENT_TAG = "myFragment";
    private static final String SECRET_MESSAGE = "Very secret message";
    private static final String KEY_NAME_NOT_INVALIDATED = "key_not_invalidated";
    static final String DEFAULT_KEY_NAME = "default_key";

    private KeyStore mKeyStore;
    private KeyGenerator mKeyGenerator;
    private SharedPreferences mSharedPreferences;


    Button btnDis,opdoor,cldoor;
    String address = null;
    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
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
//call the widgtes
        opdoor= (Button)findViewById(R.id.opendoor);
        cldoor = (Button)findViewById(R.id.closedoor);
         btnDis = (Button)findViewById(R.id.disconnect);
        new ConnectBT().execute();


//        try {
//            mKeyStore = KeyStore.getInstance("AndroidKeyStore");
//        } catch (KeyStoreException e) {
//            throw new RuntimeException("Failed to get an instance of KeyStore", e);
//        }
//        try {
//            mKeyGenerator = KeyGenerator
//                    .getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
//        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
//            throw new RuntimeException("Failed to get an instance of KeyGenerator", e);
//        }
//        Cipher defaultCipher;
//        Cipher cipherNotInvalidated;
//        try {
//            defaultCipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
//                    + KeyProperties.BLOCK_MODE_CBC + "/"
//                    + KeyProperties.ENCRYPTION_PADDING_PKCS7);
//            cipherNotInvalidated = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
//                    + KeyProperties.BLOCK_MODE_CBC + "/"
//                    + KeyProperties.ENCRYPTION_PADDING_PKCS7);
//        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
//            throw new RuntimeException("Failed to get an instance of Cipher", e);
//        }
//        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
//
//        KeyguardManager keyguardManager = getSystemService(KeyguardManager.class);
//        FingerprintManager fingerprintManager = getSystemService(FingerprintManager.class);
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            opdoor.setEnabled(true);
//            cldoor.setEnabled(true);
//        } else {
//            // Hide the purchase button which uses a non-invalidated key
//            // if the app doesn't work on Android N preview
//            opdoor.setEnabled(false);
//            cldoor.setEnabled(false);
//
//        }

//        brightness = (SeekBar)findViewById(R.id.seekBar);
        btnDis.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Disconnect(); //close connection
            }
        });
        opdoor.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
              //  turnOnLed();      //method to turn on
            }
        });

        //for checking if there are an lockguard
//        if (!keyguardManager.isKeyguardSecure()) {
//            // Show a message that the user hasn't set up a fingerprint or lock screen.
//            Toast.makeText(this,
//                    "Secure lock screen hasn't set up.\n"
//                            + "Go to 'Settings -> Security -> Fingerprint' to set up a fingerprint",
//                    Toast.LENGTH_LONG).show();
//            cldoor.setEnabled(false);
//            opdoor.setEnabled(false);
//            return;
//        }
        // Now the protection level of USE_FINGERPRINT permission is normal instead of dangerous.
        // See http://developer.android.com/reference/android/Manifest.permission.html#USE_FINGERPRINT
        // The line below prevents the false positive inspection from Android Studio
        // noinspection ResourceType
//        if (!fingerprintManager.hasEnrolledFingerprints()) {
//            opdoor.setEnabled(false);
//            cldoor.setEnabled(false);
//            // This happens when no fingerprints are registered.
//            Toast.makeText(this,
//                    "Go to 'Settings -> Security -> Fingerprint' and register at least one fingerprint",
//                    Toast.LENGTH_LONG).show();
//            return;
//        }
////        createKey(DEFAULT_KEY_NAME, true);
////        createKey(KEY_NAME_NOT_INVALIDATED, false);
//        opdoor.setEnabled(true);
////        opdoor.setOnClickListener(
////                new View.OnClickListener(defaultCipher, DEFAULT_KEY_NAME));


    }

    private void msg(String s)
    {
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
        finish(); //return to the first layout
    }

    private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute()
        {
            progress = ProgressDialog.show(AvrControl.this, "Connecting...", "Please wait!!!");  //show a progress dialog
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
                msg("Connection Failed. Is it a SPP Bluetooth? Try again.");
                finish();
            }
            else
            {
                msg("Connected.");
                isBtConnected = true;
            }
            progress.dismiss();
        }
    }

}

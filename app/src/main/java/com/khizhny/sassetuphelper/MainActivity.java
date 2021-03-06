package com.khizhny.sassetuphelper;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.security.KeyPairGeneratorSpec;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Calendar;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.security.auth.x500.X500Principal;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int PERMISSION_REQUEST_CODE = 1;
    private KeyStore keyStore;
    private TextView tvTip;
    private EditText etPin;
    private EditText etSimNumber;
    private Spinner spParameter;
    private Spinner spEnableDisable;
    private TextView tvEnableDisable;
    private Spinner spZoneType;
    private TextView tvZoneType;
    private TextView tvZoneTypeDescription;
    private EditText etNumbers;
    private TextView tvNumbers;
    private Spinner spSensorType;
    private TextView tvSensorType;
    private Spinner spChoices;
    private TextView tvChoices;
    private EditText etText;
    private TextView tvText;
    private FloatingActionButton fab;
    ArrayAdapter<String> spChoicesArrayAdapter;

    private String parameterCode;
    private String pin = "1234";
    private String simPhoneNumber;

    @Override
    protected void onStop() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        settings.edit()
                .putString("simPhoneNumber", etSimNumber.getText().toString())
                .putString("pin",encryptString("pin",etPin.getText().toString()))
                .apply();
        super.onStop();
    }

    public void makeKeysIfNeeded(String alias) {
        try {
            // Create new key if needed
            if (!keyStore.containsAlias(alias)) {
                Calendar start = Calendar.getInstance();
                Calendar end = Calendar.getInstance();
                end.add(Calendar.YEAR, 1);
                KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(this)
                        .setAlias(alias)
                        .setSubject(new X500Principal("CN=Khizhny, O=Android Authority"))
                        .setSerialNumber(BigInteger.ONE)
                        .setStartDate(start.getTime())
                        .setEndDate(end.getTime())
                        .build();
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore");
                generator.initialize(spec);

                KeyPair keyPair= generator.generateKeyPair();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Exception " + e.getMessage() + " occured", Toast.LENGTH_LONG).show();
        }
    }

    public String decryptString(String alias,String cipherText) {
        try {
            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry)keyStore.getEntry(alias, null);
            RSAPrivateKey privateKey = (RSAPrivateKey) privateKeyEntry.getPrivateKey();

            Cipher output = Cipher.getInstance("RSA/ECB/PKCS1Padding", "AndroidOpenSSL");
            output.init(Cipher.DECRYPT_MODE, privateKey);

            CipherInputStream cipherInputStream = new CipherInputStream(
                    new ByteArrayInputStream(Base64.decode(cipherText, Base64.DEFAULT)), output);
            ArrayList<Byte> values = new ArrayList<>();
            int nextByte;

            while ((nextByte = cipherInputStream.read()) != -1) {
                values.add((byte)nextByte);
            }

            byte[] bytes = new byte[values.size()];
            for(int i = 0; i < bytes.length; i++) {
                bytes[i] = values.get(i).byteValue();
            }

           return new String(bytes, 0, bytes.length, "UTF-8");

        } catch (Exception e) {
            Toast.makeText(this, "Exception " + e.getMessage() + " occured", Toast.LENGTH_LONG).show();
        }
        return null;
    }

    public String encryptString(String alias, String initialText) {
        try {
            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry)keyStore.getEntry(alias, null);
            RSAPublicKey publicKey = (RSAPublicKey) privateKeyEntry.getCertificate().getPublicKey();

            // Encrypt the text
            if(initialText.isEmpty()) {
                Toast.makeText(this, "Enter text in the 'Initial Text' widget", Toast.LENGTH_LONG).show();
                return null;
            }

            Cipher input = Cipher.getInstance("RSA/ECB/PKCS1Padding", "AndroidOpenSSL");
            input.init(Cipher.ENCRYPT_MODE, publicKey);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, input);
            cipherOutputStream.write(initialText.getBytes("UTF-8"));
            cipherOutputStream.close();

            byte [] vals = outputStream.toByteArray();
            return Base64.encodeToString(vals, Base64.DEFAULT);

        } catch (Exception e) {
            Toast.makeText(this, "Exception " + e.getMessage() + " occured", Toast.LENGTH_LONG).show();
        }
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            makeKeysIfNeeded("pin");
        } catch (Exception e) {
            e.printStackTrace();
        }

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Restoring preferences
        simPhoneNumber = PreferenceManager.getDefaultSharedPreferences(this).getString("simPhoneNumber", "80671234567");
        String encriptedPin = PreferenceManager.getDefaultSharedPreferences(this).getString("pin", "1234");
        if (encriptedPin.equals("1234")){
            pin = "1234";
        }else{
            pin = decryptString("pin", encriptedPin);
        }

        // finding Views
        fab = (FloatingActionButton) findViewById(R.id.fab);
        tvTip=(TextView)  findViewById(R.id.tvTIP);
        etPin=(EditText) findViewById(R.id.etPIN);
        etSimNumber=(EditText) findViewById(R.id.etSimNumber);
        spParameter=(Spinner)  findViewById(R.id.spParameter);
        spEnableDisable=(Spinner)  findViewById(R.id.spEnableDisable);
        tvEnableDisable=(TextView)  findViewById(R.id.tvEnableDisable);
        spZoneType=(Spinner)  findViewById(R.id.spZoneType);
        tvZoneType=(TextView)  findViewById(R.id.tvZoneType);
        tvZoneTypeDescription=(TextView)  findViewById(R.id.tvZoneTypeDescription);
        etNumbers=(EditText) findViewById(R.id.etNumbers);
        tvNumbers=(TextView)  findViewById(R.id.tvNumbers);
        spSensorType=(Spinner)  findViewById(R.id.spSensorType);
        tvSensorType=(TextView)  findViewById(R.id.tvSensorType);
        spChoices=(Spinner)  findViewById(R.id.spChoices);
        tvChoices=(TextView)  findViewById(R.id.tvChoices);
        spChoicesArrayAdapter= new ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item);
        spChoices.setAdapter(spChoicesArrayAdapter);
        tvText = (TextView)   findViewById(R.id.tvText);
        etText = (EditText)   findViewById(R.id.etText);

        etPin.setText(pin);
        etSimNumber.setText(simPhoneNumber);
        hideViews(0);

        spZoneType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                tvZoneTypeDescription.setText(getResources().getStringArray(R.array.zone_type_description)[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                int zoneCode;
                String parameterValue="";
                switch (spParameter.getSelectedItemPosition()){
                    case 0: //SMS sending
                        parameterCode="12";
                        parameterValue=""+spEnableDisable.getSelectedItemPosition();
                        break;
                    case 1: //Set alert call number
                        parameterCode="3"+(1+spChoices.getSelectedItemPosition());
                        parameterValue=""+etNumbers.getText();
                        break;
                    case 2: //Set alert SMS number
                        parameterCode="3"+(7+spChoices.getSelectedItemPosition());
                        parameterValue=""+etNumbers.getText();
                        break;
                    case 3: //Remove alert call number
                        parameterCode="3"+(1+spChoices.getSelectedItemPosition());
                        parameterValue="";
                        break;
                    case 4: //Remove alert SMS number
                        parameterCode="3"+(7+spChoices.getSelectedItemPosition());
                        parameterValue="";
                        break;
                    case 5: //Change PIN
                        parameterCode="50";
                        String newPin = etNumbers.getText().toString();
                        if (newPin.length()!=4) {
                            Snackbar.make(view, "Enter new PIN code", Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();
                            return;
                        }
                        parameterValue=newPin;
                        break;
                    case 6: //Alarm set-on delay (sec)
                        parameterCode="51";
                        String onDelay = etNumbers.getText().toString();
                        if (onDelay.length()!=2) {
                            Snackbar.make(view, "Enter 2 digits", Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();
                            return;
                        }
                        parameterValue=onDelay;
                        break;
                    case 7: //Alarm set-off delay (sec)
                        parameterCode="52";
                        String offDelay = etNumbers.getText().toString();
                        if (offDelay.length()!=2) {
                            Snackbar.make(view, "Enter 2 digits", Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();
                            return;
                        }
                        parameterValue=offDelay;
                        break;
                    case 8: //Alarm set voice
                        parameterCode="11"; // TODO 11 or 55
                        parameterValue=""+spEnableDisable.getSelectedItemPosition();
                        break;
                    case 9: //Alart sound duration (min)
                        parameterCode="53";
                        String alarmLength = etNumbers.getText().toString();
                        if (alarmLength.length()!=2) {
                            Snackbar.make(view, "Enter 2 digits [00-20]", Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();
                            return;
                        }
                        parameterValue=alarmLength;
                        break;
                    case 10: //Alart volume
                        parameterCode="62";
                        String alarmVolume = etNumbers.getText().toString();
                        if (alarmVolume.length()!=2) {
                            Snackbar.make(view, "Enter 2 digits [00-99]", Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();
                            return;
                        }
                        parameterValue=alarmVolume;
                        break;
                    case 11: //Relay mode
                        parameterCode="54";
                        parameterValue=""+spEnableDisable.getSelectedItemPosition();
                        break;
                    case 12: //Set date/time
                        parameterCode="56";
                        String dateTime = etNumbers.getText().toString();
                        if (dateTime.length()!=12) {
                            Snackbar.make(view, "Enter 12 digits [YYMMDDhhmmss]", Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();
                            return;
                        }
                        parameterValue=dateTime;
                    case 13: //Set scheduler rule#1
                        parameterCode="57";
                        if (etNumbers.getText().toString().length()!=15) {
                            Snackbar.make(view, "Enter 15 digits [hhmm][hhmm][1234567]", Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();
                            return;
                        }
                        parameterValue=etNumbers.getText().toString();
                        break;
                    case 14: //Set scheduler rule#2
                        parameterCode="58";
                        if (etNumbers.getText().toString().length()!=15) {
                            Snackbar.make(view, "Enter 15 digits [hhmm][hhmm][1234567]", Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();
                            return;
                        }
                        parameterValue=etNumbers.getText().toString();
                        break;
                    case 15: //Power off alarm
                        parameterCode="64";
                        parameterValue+=spEnableDisable.getSelectedItemPosition();
                        break;
                    case 16: //Factory reset
                        parameterCode="13";
                        parameterValue="0"; // TODO '0" or "0000"?
                        break;
                    case 17: //Set wireless zone type
                        parameterCode="60";
                        parameterValue += "0"+(1+spChoices.getSelectedItemPosition());  // Zone number [01-06]
                        parameterValue += 1+spZoneType.getSelectedItemPosition();
                        parameterValue += spEnableDisable.getSelectedItemPosition();
                        break;
                    case 18: //Set wired zone type
                        parameterCode="61";
                        zoneCode=spChoices.getSelectedItemPosition()+7;
                        if (zoneCode<10) parameterValue += "0";
                        parameterValue += zoneCode;  // Zone number [07-10]
                        parameterValue += 1+spZoneType.getSelectedItemPosition();
                        parameterValue += spEnableDisable.getSelectedItemPosition();
                        parameterValue += spSensorType.getSelectedItemPosition();
                        break;
                    case 19://Set messages
                        zoneCode=spChoices.getSelectedItemPosition()+1;
                        if (zoneCode==10) {
                            parameterCode="90";
                        }else {
                            parameterCode="8"+zoneCode;
                        }
                        if (etText.getText().toString().length()>12) {
                            Snackbar.make(view, "Only 12 digits maximum", Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();
                            return;
                        }
                        parameterValue = etText.getText().toString();
                        break;
                    case 20: //Request settings
                        parameterCode="20";
                        break;
                    case 21: //Request phones
                        parameterCode="21";
                        break;
                    case 22: //Request messages
                        parameterCode="80";
                        break;
                    case 23: //Request date/time/scheduler
                        parameterCode="22";
                        break;
                    case 24: //Request zone settings
                        parameterCode="23";
                        break;
                    default:
                        parameterCode="";
                }
                sendCommand(etSimNumber.getText().toString(),etPin.getText().toString(), parameterCode,parameterValue);
            }
        });

        spParameter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                hideViews(position);


            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_hints) {
               showHintsDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    private void hideViews(int parameterIndex){
        // hiding useless Views
        spEnableDisable.setVisibility(View.GONE);
        tvEnableDisable.setVisibility(View.GONE);
        spZoneType.setVisibility(View.GONE);
        tvZoneType.setVisibility(View.GONE);
        tvZoneTypeDescription.setVisibility(View.GONE);
        tvNumbers.setVisibility(View.GONE);
        etNumbers.setVisibility(View.GONE);
        spSensorType.setVisibility(View.GONE);
        tvSensorType.setVisibility(View.GONE);
        spChoices.setVisibility(View.GONE);
        tvChoices.setVisibility(View.GONE);
        tvText.setVisibility(View.GONE);
        etText.setVisibility(View.GONE);

        switch (parameterIndex){
            case 0: //SMS sending
                spEnableDisable.setVisibility(View.VISIBLE);
                tvEnableDisable.setText("SMS sending:");
                tvEnableDisable.setVisibility(View.VISIBLE);
                tvTip.setText("If enabled system will send messages to 3 prdefined numbers.");
                break;
            case 1: //Set alert call number
                etNumbers.setVisibility(View.VISIBLE);
                tvNumbers.setVisibility(View.VISIBLE);
                tvNumbers.setText("Phone number to call:");
                etNumbers.setText("");

                tvChoices.setVisibility(View.VISIBLE);
                spChoices.setVisibility(View.VISIBLE);
                tvChoices.setText("Phone slot:");
                spChoices.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.phone_slots)));

                tvTip.setText("Pick up slot and enter phone number in format 067 XXX XX XX without spaces");
                break;
            case 2: //Set alert SMS number
                etNumbers.setVisibility(View.VISIBLE);
                tvNumbers.setVisibility(View.VISIBLE);
                tvNumbers.setText("Phone number to text:");
                etNumbers.setText("");

                tvChoices.setVisibility(View.VISIBLE);
                spChoices.setVisibility(View.VISIBLE);
                tvChoices.setText("SMS slot:");
                spChoices.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.sms_slots)));

                tvTip.setText("Pick up slot and enter phone number in format 067 XXX XX XX without spaces.");
                break;
            case 3: //Remove alert call number
                tvChoices.setVisibility(View.VISIBLE);
                spChoices.setVisibility(View.VISIBLE);
                tvChoices.setText("Phone slot:");
                spChoices.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.phone_slots)));

                tvTip.setText("Pick up slot you want to clear");
                break;
            case 4: //Remove alert SMS number
                tvChoices.setVisibility(View.VISIBLE);
                spChoices.setVisibility(View.VISIBLE);
                tvChoices.setText("SMS slot:");
                spChoices.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.sms_slots)));

                tvTip.setText("Pick up slot you want to clear");
                break;
            case 5: //Change PIN
                etNumbers.setVisibility(View.VISIBLE);
                tvNumbers.setVisibility(View.VISIBLE);
                tvNumbers.setText("New PIN code:");
                etNumbers.setText("1234");

                tvTip.setText("Enter new pin (4 digits)");
                break;
            case 6: //Alarm set-on delay (sec)
                etNumbers.setVisibility(View.VISIBLE);
                tvNumbers.setVisibility(View.VISIBLE);
                tvNumbers.setText("Delay (sec):");
                etNumbers.setText("00");

                tvTip.setText("Set this delay if you need some time to leave the zone after alarm is set on. Enter 2 digits [00-99].");
                break;
            case 7: //Alarm set-off delay (sec)
                etNumbers.setVisibility(View.VISIBLE);
                tvNumbers.setVisibility(View.VISIBLE);
                tvNumbers.setText("Delay (sec):");
                etNumbers.setText("00");

                tvTip.setText("Set this delay if you need some time to enter the zone before alarm is set off. Enter 2 digits [00-99].");
                break;
            case 8: //Alarm set voice</item>
                spEnableDisable.setVisibility(View.VISIBLE);
                tvEnableDisable.setText("Voice support:");
                tvEnableDisable.setVisibility(View.VISIBLE);
                tvTip.setText("If enabled system will speak human voice when alarm is set on/off.");
                break;
            case 9: //Alart sound duration (min)
                etNumbers.setVisibility(View.VISIBLE);
                etNumbers.setText("01");
                tvNumbers.setVisibility(View.VISIBLE);
                tvNumbers.setText("Duration (min):");
                tvTip.setText("Set the alarm duration. Enter 2 digits [00-20].");
                break;
            case 10: //Alart volume
                etNumbers.setVisibility(View.VISIBLE);
                etNumbers.setText("99");
                tvNumbers.setVisibility(View.VISIBLE);
                tvNumbers.setText("Alarm volume level:");
                tvTip.setText("Set the alarm volume. Enter 2 digits [00-99].");
                break;
            case 11: //Relay mode
                spEnableDisable.setVisibility(View.VISIBLE);
                tvEnableDisable.setText("Relay switch:");
                tvEnableDisable.setVisibility(View.VISIBLE);
                tvTip.setText("If enabled system will switch relay state to oposite for the time of alarming. If disabled then relay switching is controlled over sms(3#-close 4#-open) or DTMF(9#close,0#-open).");
                break;
            case 12: //Set date/time
                etNumbers.setVisibility(View.VISIBLE);
                etNumbers.setText("170128204511");
                tvNumbers.setVisibility(View.VISIBLE);
                tvNumbers.setText("Date/Time:");
                tvTip.setText("Set the alarm date and time. Enter 12 digits [YYMMDDhhmmss].");
                break;
            case 13: //Set scheduler rule#1
                etNumbers.setVisibility(View.VISIBLE);
                etNumbers.setText("220007001234567");
                tvNumbers.setVisibility(View.VISIBLE);
                tvNumbers.setText("Scheduler #1:");

                tvTip.setText("Define scheduler rule #1. Enter 15 digits [AAAA][BBBB][1234567] where AAAA-time to set-on the alarm, BBBB-time to set-off, [1234567]-weekdays starting from monday.");
                break;
            case 14: //Set scheduler rule#2
                etNumbers.setVisibility(View.VISIBLE);
                etNumbers.setText("220007001234567");
                tvNumbers.setVisibility(View.VISIBLE);
                tvNumbers.setText("Scheduler #2:");
                tvTip.setText("Define scheduler rule #2. Enter 15 digits [AAAA][BBBB][1234567] where AAAA-time to set-on the alarm, BBBB-time to set-off, [1234567]-weekdays starting from monday.");
                break;
            case 15: //Power off alarm
                spEnableDisable.setVisibility(View.VISIBLE);
                tvEnableDisable.setText("Power off notify:");
                tvEnableDisable.setVisibility(View.VISIBLE);
                tvTip.setText("If enabled the system will notify you in case of mains power is down or up.");
                break;
            case 16: //Factory reset
                tvTip.setText("Resets all settings in system to defaults. Sensors and keys settings will be unchanged. Default PIN is 1234.");
                break;
            case 17: //Set wireless zone type
                tvChoices.setVisibility(View.VISIBLE);
                spChoices.setVisibility(View.VISIBLE);
                tvChoices.setText("Wireless zone number:");
                spChoices.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.wireless_zone_num)));

                spEnableDisable.setVisibility(View.VISIBLE);
                tvEnableDisable.setText("Alarming sound:");
                tvEnableDisable.setVisibility(View.VISIBLE);

                spZoneType.setVisibility(View.VISIBLE);
                tvZoneType.setVisibility(View.VISIBLE);
                tvZoneTypeDescription.setVisibility(View.VISIBLE);

                tvTip.setText("Select the behavior of the system for each zone (wireless sensor).");
                break;
            case 18: //Set wired zone type
                tvChoices.setVisibility(View.VISIBLE);
                spChoices.setVisibility(View.VISIBLE);
                tvChoices.setText("Wired zone number:");
                spChoices.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.wired_zone_num)));

                spEnableDisable.setVisibility(View.VISIBLE);
                tvEnableDisable.setText("Alarming sound:");
                tvEnableDisable.setVisibility(View.VISIBLE);

                spZoneType.setVisibility(View.VISIBLE);
                tvZoneType.setVisibility(View.VISIBLE);
                tvZoneTypeDescription.setVisibility(View.VISIBLE);

                spSensorType.setVisibility(View.VISIBLE);
                tvSensorType.setVisibility(View.VISIBLE);

                tvTip.setText("Select the behavior of the system for each zone (wired sensor).");
                break;
            case 19://Set messages
                tvText.setVisibility(View.VISIBLE);
                etText.setVisibility(View.VISIBLE);
                tvChoices.setVisibility(View.VISIBLE);
                spChoices.setVisibility(View.VISIBLE);
                tvChoices.setText("Zone number:");
                spChoices.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.zone_num)));
                break;
            case 20: //Request settings
                tvTip.setText("System will respond with message containing its setting(delays,volume,pass,etc.");
                break;
            case 21: //Request phones
                tvTip.setText("System will respond with message containing its phone slots settings");
                break;
            case 22: //Request messages
                tvTip.setText("System will respond with message containing message templates for all zones.");
                break;
            case 23: //Request date/time/scheduler
                tvTip.setText("System will respond with message containing date, time and both scheduler rules.");
                break;
            case 24: //Request zone settings
                tvTip.setText("System will respond with message containing current zone settings.");
                break;
        }
    }
    private void showHintsDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getResources().getString(R.string.hint_text));
            builder.create().show();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btnUnLock:
                // Opening sms sending activity
                sendCommand(etSimNumber.getText().toString(),etPin.getText().toString(),"0","");
                break;
            case R.id.btnLock:
                // Opening sms sending activity
               sendCommand(etSimNumber.getText().toString(),etPin.getText().toString(),"1","");
                break;
            case R.id.btnPartLock:
                // Opening sms sending activity
                sendCommand(etSimNumber.getText().toString(),etPin.getText().toString(),"2","");
                break;
            case R.id.btnFlashOn:
                // Opening sms sending activity
                sendCommand(etSimNumber.getText().toString(),etPin.getText().toString(),"3","");
                break;
            case R.id.btnFlashOff:
                // Opening sms sending activity
                sendCommand(etSimNumber.getText().toString(),etPin.getText().toString(),"4","");
                break;
            case R.id.btnPower:
                // Opening sms sending activity
                sendCommand(etSimNumber.getText().toString(),etPin.getText().toString(),"6","");
                break;
        }

    }

    private void sendCommand(String simPhoneNumber, String pin, String parameterCode, String parameterValue ){
        // Check code
        if (parameterCode.equals("")) {
            Snackbar.make(fab, "Select parameter", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            return;
        }
        // Check pin

        if (pin.length()!=4) {
            Snackbar.make(fab, "Enter PIN code", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            return;
        }

        // Phone check
        if (simPhoneNumber.length()<5) {
            Snackbar.make(fab, "Enter phone number", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            return;
        }

        // Mockup
        Snackbar.make(fab, "Sending "+pin+parameterCode+parameterValue+"#", Snackbar.LENGTH_LONG).setAction("Action", null).show();


        // Opening sms sending activity
        /*Uri uri = Uri.parse("smsto:"+simPhoneNumber);
        Intent it = new Intent(Intent.ACTION_SENDTO, uri);
        it.putExtra("sms_body", pin+parameterCode+parameterValue+"#");
        startActivity(it);/**/

        // Check permissions
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {

            if (checkSelfPermission(Manifest.permission.SEND_SMS)!= PackageManager.PERMISSION_GRANTED) {
                Log.d("permission", "permission denied to SEND_SMS - requesting it");
                String[] permissions = {Manifest.permission.SEND_SMS};
                requestPermissions(permissions, PERMISSION_REQUEST_CODE);
            }
        }
        sendSMS(simPhoneNumber,pin+parameterCode+parameterValue+"#");
    }

    /*
     * BroadcastReceiver mBrSend; BroadcastReceiver mBrReceive;
     */
    private void sendSMS(String phoneNumber, String message) {
        ArrayList<PendingIntent> sentPendingIntents = new ArrayList<PendingIntent>();
        ArrayList<PendingIntent> deliveredPendingIntents = new ArrayList<PendingIntent>();
        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0,
                new Intent(this, SmsSentReceiver.class), 0);
        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,
                new Intent(this, SmsDeliveredReceiver.class), 0);
        try {
            SmsManager sms = SmsManager.getDefault();
            ArrayList<String> mSMSMessage = sms.divideMessage(message);
            for (int i = 0; i < mSMSMessage.size(); i++) {
                sentPendingIntents.add(i, sentPI);
                deliveredPendingIntents.add(i, deliveredPI);
            }
            sms.sendMultipartTextMessage(phoneNumber, null, mSMSMessage,
                    sentPendingIntents, deliveredPendingIntents);

        } catch (Exception e) {
            Snackbar.make(fab, "SMS sending failed...", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            e.printStackTrace();
        }

    }

}

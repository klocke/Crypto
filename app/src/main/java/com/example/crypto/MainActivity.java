package com.example.crypto;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.File;
import java.net.URLConnection;
import java.security.Key;

public class MainActivity extends AppCompatActivity {

    private TextView _tevFileName;
    private Spinner _sprAlgos;
    private Button _btnEncrypt;
    private Button _btnDecrypt;

    private String _path = "";
    private Algo _selectedAlgo;
    private Key _key;

    private boolean _isEncrypted;
    private long _backPressed;

    public static final String SPINNER_HINT = "auswählen...";
    public static final String[] SPINNER_ITEMS = {
            SPINNER_HINT,
            AlgoGroup.SYMMETRIC.getSpinnerName(),
            Algo.AES.getName(),
            Algo.DES.getName(),
            Algo.BLOWFISH.getName(),
            AlgoGroup.ASYMMETRIC.getSpinnerName(),
            Algo.RSA.getName()
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupViews();
    }

    private void setupViews() {
        _sprAlgos = (Spinner) findViewById(R.id.spr_algo);
        setupSpinner(_sprAlgos);

        View _filepicker = findViewById(R.id.filepicker);
        setupFilePicker(_filepicker);

        _btnEncrypt = (Button) findViewById(R.id.btn_encrypt);
        _btnDecrypt = (Button) findViewById(R.id.btn_decrypt);

        _btnEncrypt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                encrypt();
            }
        });

        _btnDecrypt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                decrypt();
            }
        });
    }

    private void setupSpinner(Spinner spinner) {
        // Spinner-Icon Farbe anpassen
        Drawable spinnerDrawable = spinner.getBackground();
        int colorPrimaryDark = ContextCompat.getColor(this, R.color.colorPrimaryDark);
        PorterDuffColorFilter filter = new PorterDuffColorFilter(colorPrimaryDark, PorterDuff.Mode.SRC_ATOP);
        spinnerDrawable.setColorFilter(filter);

        SpinnerArrayAdapter adapter = new SpinnerArrayAdapter(this, android.R.layout.simple_spinner_item, SPINNER_ITEMS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        String[] spinnerNames = AlgoGroup.getSpinnerNames();
        adapter.setDisabledItems(spinnerNames);

        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TextView tevSpinnerSelection = (TextView) view;

                if (tevSpinnerSelection != null) {
                    String algoName = tevSpinnerSelection.getText().toString();
                    _selectedAlgo = Algo.getAlgo(algoName);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupFilePicker(final View filepicker) {
        ImageView imvClear = (ImageView) filepicker.findViewById(R.id.imv_clear);
        ImageView imvPreview = (ImageView) filepicker.findViewById(R.id.imv_preview);
        _tevFileName = (TextView) filepicker.findViewById(R.id.file_name);

        filepicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");  // Alle MIME-Typen (Dateitypen) zulassen.

                startActivityForResult(intent, Constants.ACTION_OPEN_DOCUMENT_REQUEST_CODE);
            }
        });

        // Durch eigenen OnClickListener reagiert die View bei Klicks unabhängig der Parent View.
        imvClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (_isEncrypted) {
                    CryptoUtils.storeKey(MainActivity.this, _path, _key);
                    setEncrypted(false);
                }

                setPath("");
                _sprAlgos.setSelection(0);
            }
        });

        imvPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (_path == "") {
                    Message.show(MainActivity.this, getString(R.string.msg_choose_file));
                    return;
                }

                String mimeType = URLConnection.guessContentTypeFromName(_path);

                Log.i("demo", "mimeType? " + mimeType);
                Intent intent = new Intent(Intent.ACTION_VIEW);

                intent.setDataAndType(Uri.fromFile(new File(_path)), mimeType);
                intent.setFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);

                try {
                    MainActivity.this.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Message.show(MainActivity.this, getString(R.string.msg_no_app_found));
                }
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(Constants.ARG_PATH, _path);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        setPath(savedInstanceState.getString(Constants.ARG_PATH));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Constants.ACTION_OPEN_DOCUMENT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                setPath(SAFUtils.getPath(this, uri));

                if (CryptoUtils.isStored(this, _path)) {
                    setEncrypted(true);

                    _key = CryptoUtils.loadKey(this, _path);

                    // Spinner auf richtigen Algo setzen
                    SpinnerArrayAdapter adapter = (SpinnerArrayAdapter) _sprAlgos.getAdapter();
                    int sprPos = adapter.getPosition(_key.getAlgorithm());
                    _sprAlgos.setSelection(sprPos);

                    new AlertDialog.Builder(this)
                            .setTitle(getString(R.string.dialog_title_file_encrypted))
                            .setMessage(getString(R.string.dialog_msg_want_to_decrypt))
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    decrypt();
                                }
                            })
                            .setNegativeButton(android.R.string.no, null)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                } else {
                    setEncrypted(false);
                    _sprAlgos.setSelection(0);
                }
            }
        }
    }

    @Override
    protected void onStop() {
        if (_isEncrypted) {
            CryptoUtils.storeKey(this, _path, _key);
        }

        super.onStop();
    }

    @Override
    public void onBackPressed() {
        if (_backPressed + Constants.BACK_PRESS_MILLIS_TIME_INTERVAL > System.currentTimeMillis()) {
            Message.cancel();
            super.onBackPressed();
            return;
        } else {
            Message.show(this, getString(R.string.msg_press_back_to_cancel));
        }

        _backPressed = System.currentTimeMillis();
    }

    private void encrypt() {
        if (_path == "" || _selectedAlgo == null) {
            Message.show(MainActivity.this, getString(R.string.msg_choose_file_and_algo));
            return;
        }

        try {
            _key = CryptoUtils.encrypt(_path, _selectedAlgo);

            if (_key != null) {
                setEncrypted(true);
                Message.show(MainActivity.this, getString(R.string.msg_encryption_success));
            }
        } catch (Exception e) {
            if (e instanceof RSAException) {
                Log.i("demo", "RSAException Message? " + e.getMessage());
                Message.show(MainActivity.this, getString(R.string.msg_encryption_failure_rsa));
            } else {
                Message.show(MainActivity.this, getString(R.string.msg_encryption_failure));
            }

            e.printStackTrace();
        }
    }

    private void decrypt() {
        if (_path == "" || _selectedAlgo == null) {
            Message.show(MainActivity.this, getString(R.string.msg_choose_file_and_algo));
            return;
        }

        try {
            CryptoUtils.decrypt(_key, _path, _selectedAlgo);

            if (CryptoUtils.isStored(MainActivity.this, _path)) {
                CryptoUtils.deleteKey(MainActivity.this, _path);
            }

            _key = null;
            setEncrypted(false);
            Message.show(MainActivity.this, getString(R.string.msg_decryption_success));
        } catch (Exception e) {
            e.printStackTrace();
            Message.show(MainActivity.this, getString(R.string.msg_encryption_success));
        }
    }

    private void setPath(String path) {
        Log.i("demo", "path? " + path);
        _path = path;

        if (path != null) {
            String fileName = new File(path).getName();
            _tevFileName.setText(fileName);
        }
    }

    private void setEncrypted(boolean isEncrypted) {
        _isEncrypted = isEncrypted;

        _sprAlgos.setEnabled(!_isEncrypted);
        _btnEncrypt.setEnabled(!_isEncrypted);
        _btnDecrypt.setEnabled(_isEncrypted);
    }

}

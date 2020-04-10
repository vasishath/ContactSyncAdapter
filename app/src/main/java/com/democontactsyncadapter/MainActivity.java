package com.democontactsyncadapter;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.democontactsyncadapter.syncAdapter.ContactsManager;
import com.democontactsyncadapter.syncAdapter.SyncUtils;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ArrayList<MyContact> myContacts = new ArrayList<>();

    TextView tv;
    ProgressBar pb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv = findViewById(R.id.tv);
        pb = findViewById(R.id.pb);

        SyncUtils.CreateSyncAccount(this);

        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkPermissions();
            }
        });
    }

    void checkPermissions() {
        if (Permission.selfPermissionGranted(this, Manifest.permission.READ_CONTACTS)
                && Permission.selfPermissionGranted(this, Manifest.permission.WRITE_CONTACTS)
                && Permission.selfPermissionGranted(this, Manifest.permission.READ_PHONE_STATE)) {
            new SyncContacts().execute();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS,
                        Manifest.permission.READ_PHONE_STATE}, 0);
            } else {
            }
        }
    }

    class SyncContacts extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pb.setVisibility(View.VISIBLE);
            tv.setVisibility(View.GONE);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            int hasPhone;
            String[] PROJECTION = new String[] {
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.RawContacts.ACCOUNT_TYPE,
                    ContactsContract.CommonDataKinds.Photo.CONTACT_ID };

            Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
            String filter = ""+ ContactsContract.Contacts.HAS_PHONE_NUMBER + " > 0 and " + ContactsContract.CommonDataKinds.Phone.TYPE +"=" + ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE + " and " + ContactsContract.RawContacts.ACCOUNT_TYPE + " = ?";
            String order = ContactsContract.Contacts.DISPLAY_NAME + " ASC";// LIMIT " + limit + " offset " + lastId + "";
            String[] selectionArgs = new String[]{"com.google"};
            Cursor c = getContentResolver().query(uri, PROJECTION, filter, selectionArgs, order);
            if ((c != null) && c.moveToFirst()) {
                while (c.moveToNext()) {

                    if (true) {
                        //Log.e("", "contact " + c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)));
                            //String mimetype = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.MIMETYPE));

                                String number = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                                int id = c.getInt(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));
                                //Log.e("", "contact " + mimetype);
                                MyContact myContact = new MyContact(c.getString(
                                        c.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)), number, id);
                                myContacts.add(myContact);



                    }
                }
                c.close();
            }
            for (int i = 0; i < myContacts.size(); i++) {
                ContactsManager.addContact(MainActivity.this, myContacts.get(i));
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Toast.makeText(MainActivity.this, "Contacts Synced Successfully", Toast.LENGTH_SHORT).show();
            pb.setVisibility(View.GONE);
            tv.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 0:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    new SyncContacts().execute();
                } else {
                }
                break;
        }
    }
}

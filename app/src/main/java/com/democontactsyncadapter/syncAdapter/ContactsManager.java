package com.democontactsyncadapter.syncAdapter;

import android.accounts.Account;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

import com.democontactsyncadapter.MyContact;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by richa on 3/7/18.
 */

public class ContactsManager {
    private static String MIMETYPE = "vnd.android.cursor.item/com.account";

    public static void addContact(Context context, MyContact contact) {
        ContentResolver resolver = context.getContentResolver();
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        Account account = AccountDetails.getAccount();
        boolean mHasAccount = isAlreadyRegistered(resolver, contact.getId());

        if (mHasAccount) {
            return;
        }
        ops.add(ContentProviderOperation
                .newInsert(addCallerIsSyncAdapterParameter(ContactsContract.RawContacts.CONTENT_URI, true))
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, account.name)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, account.type)
                .withValue(ContactsContract.RawContacts.AGGREGATION_MODE, ContactsContract.RawContacts.AGGREGATION_MODE_DEFAULT)
                .build());

        ops.add(ContentProviderOperation
                .newInsert(addCallerIsSyncAdapterParameter(ContactsContract.Data.CONTENT_URI, true))
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contact.getName())
                .build());

        ops.add(ContentProviderOperation
                .newInsert(addCallerIsSyncAdapterParameter(ContactsContract.Data.CONTENT_URI, true))
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, MIMETYPE)
                .withValue(ContactsContract.Data.DATA1, 12345)
                .withValue(ContactsContract.Data.DATA2, "DemoContactsSyncAdapter")
                .withValue(ContactsContract.Data.DATA3, "Call " + contact.getNumber())
                .build());





        try {
            resolver.applyBatch(ContactsContract.AUTHORITY, ops);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Cursor cursor = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI, new String[] {ContactsContract.Data.RAW_CONTACT_ID, ContactsContract.Data.DISPLAY_NAME, ContactsContract.Data.MIMETYPE,
                        ContactsContract.Data.CONTACT_ID },
                ContactsContract.CommonDataKinds.Phone.NUMBER + "= ?",
                new String[] {contact.getNumber()}, null);

        if (cursor.moveToFirst()) {
            joinIntoExistingContact(context, contact.getId(), cursor.getLong(0));
        }
    }

    private static Uri addCallerIsSyncAdapterParameter(Uri uri, boolean isSyncOperation) {
        if (isSyncOperation) {
            return uri.buildUpon().appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true").build();
        }
        return uri;
    }

    public static boolean isAlreadyRegistered(ContentResolver resolver, int id) {

        boolean isRegistered = false;
        List<String> str = new ArrayList<>();

        //query raw contact id's from the contact id
        Cursor c = resolver.query(ContactsContract.RawContacts.CONTENT_URI, new String[]{ContactsContract.RawContacts._ID},
                ContactsContract.RawContacts.CONTACT_ID + "=?",
                new String[]{String.valueOf(id)}, null);

        //fetch all raw contact id's and save them in a list of string
        if (c != null && c.moveToFirst()) {
            do {
                str.add(c.getString(c.getColumnIndexOrThrow(ContactsContract.RawContacts._ID)));
            } while (c.moveToNext());
            c.close();
        }

        //query account types and check the account type for each raw contact id
        for (int i = 0; i < str.size(); i++) {
            Cursor c1 = resolver.query(ContactsContract.RawContacts.CONTENT_URI, new String[]{ContactsContract.RawContacts.ACCOUNT_TYPE},
                    ContactsContract.RawContacts._ID + "=?",
                    new String[]{str.get(i)}, null);

            if (c1 != null) {
                c1.moveToFirst();
                String accType = c1.getString(c1.getColumnIndexOrThrow(ContactsContract.RawContacts.ACCOUNT_TYPE));
                if (accType != null && accType.equals("com.account")) {
                    isRegistered = true;
                    break;
                }
                c1.close();
            }
        }

        return isRegistered;
    }

    private static void joinIntoExistingContact(Context context, long existingContactId, long newRawContactId) {

        // get all existing raw-contact-ids that belong to the contact-id
        List<Long> existingRawIds = new ArrayList<>();
        Cursor cur = context.getContentResolver().query(ContactsContract.RawContacts.CONTENT_URI, new String[] { ContactsContract.RawContacts._ID }, ContactsContract.RawContacts.CONTACT_ID + "=" + existingContactId, null, null);
        while (cur.moveToNext()) {
            existingRawIds.add(cur.getLong(0));
        }
        cur.close();
        Log.i("Join", "Found " + existingRawIds.size() + " raw-contact-ids");

        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

        // go over all existing raw ids, and join with our new one
        for (Long existingRawId : existingRawIds) {
            ContentProviderOperation.Builder builder = ContentProviderOperation.newUpdate(ContactsContract.AggregationExceptions.CONTENT_URI);
            builder.withValue(ContactsContract.AggregationExceptions.TYPE, ContactsContract.AggregationExceptions.TYPE_KEEP_TOGETHER);
            builder.withValue(ContactsContract.AggregationExceptions.RAW_CONTACT_ID1, newRawContactId);
            builder.withValue(ContactsContract.AggregationExceptions.RAW_CONTACT_ID2, existingRawId);
            ops.add(builder.build());
        }

        try {
            context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
}

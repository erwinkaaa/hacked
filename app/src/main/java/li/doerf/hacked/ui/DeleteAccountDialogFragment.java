package li.doerf.hacked.ui;

import android.app.Dialog;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import java.util.Collection;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import li.doerf.hacked.HackedApplication;
import li.doerf.hacked.R;
import li.doerf.hacked.db.HackedSQLiteHelper;
import li.doerf.hacked.db.tables.Account;
import li.doerf.hacked.db.tables.Breach;

/**
 * Created by moo on 06/09/16.
 */
public class DeleteAccountDialogFragment extends DialogFragment {
    private final String LOGTAG = getClass().getSimpleName();
    private Account myAccount;
    private SQLiteDatabase myDb;

    public void setAccount(Account account) {
        myAccount = account;
        myDb = HackedSQLiteHelper.getInstance(getContext()).getWritableDatabase();
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(getString(R.string.dialog_account_delete_msg, myAccount.getName()))
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Collection<Breach> breaches = Breach.findAllByAccount(myDb, myAccount);
                        try {
                            myDb.beginTransaction();
                            for (Breach b : breaches) {
                                b.delete(myDb);
                            }
                            myAccount.delete(myDb);
                            myDb.setTransactionSuccessful();
                        } finally {
                            myDb.endTransaction();
                            myAccount.notifyObservers();
                            ((HackedApplication) getActivity().getApplication()).trackEvent("DeleteAccount");
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        DeleteAccountDialogFragment.this.getDialog().cancel();
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }
}

package li.doerf.hacked.ui.fragments;

import android.animation.AnimatorInflater;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import li.doerf.hacked.R;
import li.doerf.hacked.db.DatasetChangeListener;
import li.doerf.hacked.db.HackedSQLiteHelper;
import li.doerf.hacked.db.tables.Account;
import li.doerf.hacked.services.HaveIBeenPwnedCheckService;
import li.doerf.hacked.ui.adapters.AccountsAdapter;
import li.doerf.hacked.utils.ConnectivityHelper;
import li.doerf.hacked.utils.IServiceRunningListener;
import li.doerf.hacked.utils.SynchronizationHelper;

/**
 * Created by moo on 05/10/16.
 */
public class AccountListFragment extends Fragment implements DatasetChangeListener, IServiceRunningListener {
    private final String LOGTAG = getClass().getSimpleName();
    private SQLiteDatabase myReadbableDb;
    private AccountsAdapter myAccountsAdapter;
    private Cursor myCursor;

    public static AccountListFragment create() {
        return new AccountListFragment();
    }

    public AccountListFragment() {
        super();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myReadbableDb = HackedSQLiteHelper.getInstance(getContext()).getReadableDatabase();
        myAccountsAdapter = new AccountsAdapter(getContext(), null, getFragmentManager());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View fragmentRoot =  inflater.inflate(R.layout.fragment_account_list, container, false);

        RecyclerView accountsList = (RecyclerView) fragmentRoot.findViewById(R.id.accounts_list);
        accountsList.setHasFixedSize(true);
        LinearLayoutManager lm = new LinearLayoutManager(getContext());
        accountsList.setLayoutManager(lm);
        accountsList.setAdapter(myAccountsAdapter);

        showInitialSetupAccount(fragmentRoot);
        showInitialSetupCheck(fragmentRoot);
        showInitialHelp(fragmentRoot);

        return fragmentRoot;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshList();
    }

    private void showInitialSetupAccount(final View aRootView) {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean initialSetupAccountDone = settings.getBoolean(getString(R.string.pref_initial_setup_account_done), false);
        if ( ! initialSetupAccountDone ) {
            final CardView initialAccount = (CardView) aRootView.findViewById(R.id.initial_account);
            initialAccount.setVisibility(View.VISIBLE);
            Button addB = (Button) aRootView.findViewById(R.id.button_add_initial_account);
            addB.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EditText accountET = (EditText) aRootView.findViewById(R.id.account);
                    String accountName = accountET.getText().toString().trim();

                    if ( accountName.equals("") ) {
                        Toast.makeText(getContext(), getString(R.string.toast_please_enter_account), Toast.LENGTH_LONG).show();
                        return;
                    }

                    Account account = Account.create( accountName);
                    SQLiteDatabase db = HackedSQLiteHelper.getInstance(getContext()).getWritableDatabase();

                    if ( account.exists(db) ) {
                        Log.w(LOGTAG, "account already exists");
                        Toast.makeText(getContext(), getString(R.string.toast_account_exists), Toast.LENGTH_LONG).show();
                        return;
                    }

                    db.beginTransaction();
                    account.insert(db);
                    db.setTransactionSuccessful();
                    db.endTransaction();
                    account.notifyObservers();

                    initialAccount.setVisibility(View.GONE);
                    Toast.makeText(getContext(), getString(R.string.toast_account_added), Toast.LENGTH_LONG).show();
                    checkForBreaches(initialAccount, account);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean(getString(R.string.pref_initial_setup_account_done), true);
                    editor.apply();
                    InputMethodManager mgr = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    mgr.hideSoftInputFromWindow(accountET.getWindowToken(), 0);
                }
            });
        }
    }

    private void showInitialSetupCheck(View aRootView) {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean initialSetupCheckDone = settings.getBoolean(getString(R.string.pref_initial_setup_check_done), false);
        if ( ! initialSetupCheckDone ) {
            final CardView initialSetupCheck = (CardView) aRootView.findViewById(R.id.initial_setup_check);
            initialSetupCheck.setVisibility(View.VISIBLE);

            Button noB = (Button) aRootView.findViewById(R.id.initial_setup_check_no);
            noB.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    initialSetupCheck.setVisibility(View.GONE);
                    Toast.makeText(getContext(), getString(R.string.toast_check_not_enabled), Toast.LENGTH_LONG).show();

                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean(getString(R.string.pref_initial_setup_check_done), true);
                    editor.apply();
                }
            });

            Button yesB = (Button) aRootView.findViewById(R.id.initial_setup_check_yes);
            yesB.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    initialSetupCheck.setVisibility(View.GONE);

                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean(getString(R.string.pref_key_sync_enable), true);
                    editor.putBoolean(getString(R.string.pref_initial_setup_check_done), true);
                    editor.apply();

                    SynchronizationHelper.scheduleSync(getContext());
                    Toast.makeText(getContext(), getString(R.string.toast_check_enabled), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void showInitialHelp(View aRootView) {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean initialHelpDismissed = settings.getBoolean(getString(R.string.pref_initial_help_dismissed), false);
        if ( ! initialHelpDismissed ) {
            final CardView initialHelp = (CardView) aRootView.findViewById(R.id.initial_help);
            initialHelp.setVisibility(View.VISIBLE);
            Button dismissB = (Button) aRootView.findViewById(R.id.button_dismiss_help);
            dismissB.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    initialHelp.setVisibility(View.GONE);
                    Toast.makeText(getContext(), getString(R.string.toast_dont_show_initial_help_again), Toast.LENGTH_SHORT).show();

                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean(getString(R.string.pref_initial_help_dismissed), true);
                    editor.apply();
                }
            });
        }
    }

    public void refreshList() {
        myCursor = Account.listAll(myReadbableDb);
        myAccountsAdapter.swapCursor(myCursor);

        // TODO
//        if ( myCursor.getCount() > 0 ) {
//            myFloatingActionCheckButton.setVisibility(View.VISIBLE);
//        } else {
//            myFloatingActionCheckButton.setVisibility(View.GONE);
//        }
    }

    @Override
    public void datasetChanged() {
        refreshList();
    }

    private void checkForBreaches(View view, Account account) {
        if ( ! ConnectivityHelper.isConnected( getContext()) ) {
            Log.i(LOGTAG, "no network");
            Toast.makeText(getContext(), getString(R.string.toast_error_no_network), Toast.LENGTH_SHORT).show();
            return;
        }

        // TODO
//        // only do this when checking more than one account (possible timing issue)
//        if ( account == null && mySyncActive) {
//            Log.i(LOGTAG, "check already in progress");
//            Toast.makeText(getContext(), getString(R.string.toast_check_in_progress), Toast.LENGTH_SHORT).show();
//            return;
//        }

        Intent i = new Intent(getContext(), HaveIBeenPwnedCheckService.class);

        if ( account != null ) {
            i.putExtra(HaveIBeenPwnedCheckService.EXTRA_IDS, new long[] {account.getId()});
        }

        getContext().startService(i);

        if ( account == null ) { // only show this message when checking for more then one account
            int expectedDuration = (int) Math.ceil(myAccountsAdapter.getItemCount() * 2.5);
            Snackbar.make(view, getString(R.string.snackbar_checking_account, expectedDuration), Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }
    }

    @Override
    public void notifyListener(final Event anEvent) {
        new Handler(Looper.getMainLooper()).post(
                new Runnable() {
                    @Override
                    public void run() {
                        // TODO
//                        switch (anEvent) {
//                            case STARTED:
//                                mySyncActive = true;
//
//                                if (myFabAnimation == null) {
//                                    Log.d(LOGTAG, "animation starting");
//                                    myFabAnimation = (ObjectAnimator) AnimatorInflater.loadAnimator(getContext(),
//                                            R.animator.rotate_right_repeated);
//                                    myFabAnimation.setTarget(myFloatingActionCheckButton);
//                                    myFabAnimation.start();
//                                } else {
//                                    Log.d(LOGTAG, "animation already active");
//                                }
//                                break;
//
//                            case STOPPED:
//                                mySyncActive = false;
//
//                                if (myFabAnimation != null) {
//                                    Log.d(LOGTAG, "animation stopping");
//                                    myFabAnimation.removeAllListeners();
//                                    myFabAnimation.end();
//                                    myFabAnimation.cancel();
//                                    myFabAnimation = null;
//                                    myFloatingActionCheckButton.clearAnimation();
//                                    myFloatingActionCheckButton.setRotation(0);
//                                }
//                                break;
//                        }
                    }
                }
        );
    }

}

package com.atb.nfcapp;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

/**
 * Service Android hébergeant la logique d'autorisation bancaire (acquéreur).
 * Le client (WaitingActivity) s'y lie via bindService() plutôt que d'instancier
 * directement MockAcquirerService -- ce découplage permet de faire évoluer ce
 * service vers un vrai acquéreur distant plus tard, sans changer la façon dont
 * l'Activity communique avec lui.
 */
public class AcquirerConnectionService extends Service {

    private static final String TAG_LOG = "ATBPay_Acquirer";

    private final IBinder binder = new LocalBinder();
    private AcquirerService acquirerService;

    /** Binder local : donne au client un accès direct à l'interface AcquirerService. */
    public class LocalBinder extends Binder {
        AcquirerService getService() {
            return acquirerService;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG_LOG, "Service acquéreur créé");
        // <-- SEULE ligne à changer pour brancher un vrai acquéreur plus tard
        acquirerService = new MockAcquirerService();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG_LOG, "Client lié au service");
        return binder;
    }
}
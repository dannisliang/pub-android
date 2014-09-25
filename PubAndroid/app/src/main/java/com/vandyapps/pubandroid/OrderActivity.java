package com.vandyapps.pubandroid;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

import static com.vandyapps.pubandroid.OrderResponse.Order;

public class OrderActivity extends Activity {

    private static final String TAG = OrderActivity.class.getName();

    private AtomicBoolean mBound = new AtomicBoolean(false);
    @InjectView(R.id.number_list)
    TextView mTextView;
    private List<Order> mOrders;
    private QueryService mService;
    private Messenger mMessenger = new Messenger(new PubHandler(this));
    private long mLastUpdated = -1;

    private static class PubHandler extends Handler {
        /* We have to use a weak reference here to avoid memory leaks.
         This happens when a message is left on the Looper queue that still has a reference
         to our activity.
        */
        WeakReference<OrderActivity> mReference;

        PubHandler(OrderActivity a) {
            mReference = new WeakReference<OrderActivity>(a);
        }

        @Override
        public void handleMessage(Message msg) {
            OrderActivity a = mReference.get();
            if (a == null)
                return;
            switch (msg.what) {
                case Constants.NEW_DATA:
                    a.updateList((List<Order>) msg.obj);
                    break;
                case Constants.STOP_REQUEST:
                    a.finish();
                    break;
                default:
                    Log.e(TAG, "Got a message with unknown type: " + msg.what);
            }
        }
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {

        /*
        * This is called when the service we bound to is started.
        */
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            QueryService.QueryBinder b = (QueryService.QueryBinder) binder;
            mService = b.getService();
            mBound.set(true);
            mService.setMessenger(mMessenger);
            mService.startQueries();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound.set(false);
            mService = null;
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);
        ButterKnife.inject(this);

        // We have to set our custom font here because apparently
        // there's no way to do it in xml
        Typeface myTypeface = Typeface.createFromAsset(getAssets(), "dotmatrix.tff");
        mTextView.setTypeface(myTypeface);

        Intent i = new Intent(this, QueryService.class);
        bindService(i, mServiceConnection, Context.BIND_AUTO_CREATE);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
    }

    @OnClick(R.id.filter_button)
    public void notifyClick(View v) {
        if (!mBound.get())
            return;
        final Dialog dlg = new Dialog(this);
        dlg.setContentView(R.layout.dialog_order_prompt);
        dlg.setTitle(R.string.order_prompt_text);
        final EditText et = (EditText) dlg.findViewById(R.id.prompt_et);
        final Button ok = (Button) dlg.findViewById(R.id.prompt_ok_button);
        final Button cancel = (Button) dlg
                .findViewById(R.id.prompt_cancel_button);
        ok.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                String input = et.getText().toString();
                try {
                    int orderNum = Integer.parseInt(input);
                    mService.setNotify(orderNum);
                } catch (NumberFormatException e) {
                    Toast.makeText(OrderActivity.this, "Please enter a number",
                            Toast.LENGTH_LONG).show();
                }
                dlg.dismiss();
            }

        });
        cancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dlg.dismiss();
            }
        });
        dlg.show();
    }

    private void updateList(List<Order> orders) {
        // Prevent refreshing when there's no new data
        //if (orders.isEmpty() || orders.get(0).getTimeCreated() <= mLastUpdated)
         //   return;
        //Log.d(TAG, "Updating list with " + orders.toString());
        //mLastUpdated = orders.get(0).getTimeCreated();

        mOrders = orders;
        mTextView.setText("");
        for (Order order : orders) {
            mTextView.append(String.valueOf(order.getOrderNumber()) + "   ");
        }
    }

}
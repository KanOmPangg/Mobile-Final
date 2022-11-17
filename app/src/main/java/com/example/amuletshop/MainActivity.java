package com.example.amuletshop;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Notification;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.example.amuletshop.adapter.MyAmuletAdapter;
import com.example.amuletshop.eventbus.MyUpdateCartEvent;
import com.example.amuletshop.listener.IAmuletLoadListener;
import com.example.amuletshop.listener.ICartLoadListener;
import com.example.amuletshop.model.AmuletModel;
import com.example.amuletshop.model.CartModel;
import com.example.amuletshop.utils.SpaceItemDecoration;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nex3z.notificationbadge.NotificationBadge;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.DefaultGasProvider;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity implements IAmuletLoadListener, ICartLoadListener {
    @BindView(R.id.recycler_amulet)
    RecyclerView recyclerAmulet;
    @BindView(R.id.mainLayout)
    RelativeLayout mainLayout;
    @BindView(R.id.badge)
    NotificationBadge badge;
    @BindView(R.id.btnCart)
    FrameLayout btnCart;


    IAmuletLoadListener amuletLoadListener;
    ICartLoadListener cartLoadListener;

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        if(EventBus.getDefault().hasSubscriberForEvent(MyUpdateCartEvent.class))
            EventBus.getDefault().removeStickyEvent(MyUpdateCartEvent.class);
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Subscribe(threadMode = ThreadMode.MAIN,sticky = true)
    public void onUpdateCart(MyUpdateCartEvent event)
    {
        countCartItem();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
        loadAmuletFromFirebase();
        countCartItem();
    }


    private void loadAmuletFromFirebase() {
        List<AmuletModel> amuletModels = new ArrayList<>();
        FirebaseDatabase.getInstance()
                .getReference("Amulet")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if(snapshot.exists())
                        {
                            for(DataSnapshot amuletSnapshot:snapshot.getChildren())
                             {
                                AmuletModel amuletModel = amuletSnapshot.getValue(AmuletModel.class);
                                amuletModel.setKey(amuletSnapshot.getKey());
                                amuletModels.add(amuletModel);
                            }
                            amuletLoadListener.onAmuletLoadSuccess(amuletModels);
                        }
                        else
                            amuletLoadListener.onAmuletLoadFail("Can't find Amulet");
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
amuletLoadListener.onAmuletLoadFail(error.getMessage());
                    }
                });
    }

    private void init(){
        ButterKnife.bind(this);

        amuletLoadListener = this;
        cartLoadListener = this;

        GridLayoutManager gridLayoutManager = new GridLayoutManager(this,2);
        recyclerAmulet.setLayoutManager(gridLayoutManager);
        recyclerAmulet.addItemDecoration(new SpaceItemDecoration());

        btnCart.setOnClickListener(v -> startActivity(new Intent(this, CartActivity.class)));
    }

    @Override
    public void onAmuletLoadSuccess(List<AmuletModel> amuletModelList) {
        MyAmuletAdapter adapter = new MyAmuletAdapter(this,amuletModelList,cartLoadListener);
        recyclerAmulet.setAdapter(adapter);
    }

    @Override
    public void onAmuletLoadFail(String message) {
        Snackbar.make(mainLayout,message,Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onCartLoadSuccess(List<CartModel> cartModelList) {
        int cartSum = 0;
        for(CartModel cartModel: cartModelList)
            cartSum += cartModel.getQuantity();
        badge.setNumber(cartSum);
    }

    @Override
    public void onCartLoadFail(String message) {
        Snackbar.make(mainLayout,message,Snackbar.LENGTH_LONG).show();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        countCartItem();
    }

    private void countCartItem() {
        List<CartModel> cartModels = new ArrayList<>();
        FirebaseDatabase
                .getInstance().getReference("Cart")
                .child("UNIQUE_USER_ID")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for(DataSnapshot cartSnapshot:snapshot.getChildren())
                        {
                            CartModel cartModel = cartSnapshot.getValue(CartModel.class);
                            cartModel.setKey(cartSnapshot.getKey());
                            cartModels.add(cartModel);
                        }
                        cartLoadListener.onCartLoadSuccess(cartModels);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        cartLoadListener.onCartLoadFail(error.getMessage());
                    }
                });
    }
}
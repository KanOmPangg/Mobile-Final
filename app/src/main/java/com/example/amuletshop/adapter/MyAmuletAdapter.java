package com.example.amuletshop.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.amuletshop.R;
import com.example.amuletshop.eventbus.MyUpdateCartEvent;
import com.example.amuletshop.listener.ICartLoadListener;
import com.example.amuletshop.listener.IRecyclerViewClickListener;
import com.example.amuletshop.model.AmuletModel;
import com.example.amuletshop.model.CartModel;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.greenrobot.eventbus.EventBus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class MyAmuletAdapter extends RecyclerView.Adapter<MyAmuletAdapter.MyAmuletViewHolder> {

    private Context context;
    private List<AmuletModel> amuletModelList;
    private ICartLoadListener iCartLoadListener;

    public MyAmuletAdapter(Context context, List<AmuletModel> amuletModelList, ICartLoadListener iCartLoadListener) {
        this.context = context;
        this.amuletModelList = amuletModelList;
        this.iCartLoadListener = iCartLoadListener;
    }

    @NonNull
    @Override
    public MyAmuletViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyAmuletViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.layout_amulet_item,parent,false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyAmuletViewHolder holder, int position) {
        Glide.with(context)
                .load(amuletModelList.get(position).getImage())
                .into(holder.imageView);
        holder.txtPrice.setText(new StringBuilder("฿").append(amuletModelList.get(position).getPrice()));
        holder.txtName.setText(new StringBuilder().append(amuletModelList.get(position).getName()));

        holder.setListener((view, adapterPosition) -> {
            addToCart(amuletModelList.get(position));
        });
    }

    private void addToCart(AmuletModel amuletModel) {
        DatabaseReference userCart = FirebaseDatabase
                .getInstance()
                .getReference("Cart")
                .child("UNIQUE_USER_ID");

        userCart.child(amuletModel.getKey())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.exists())
                        {
                            CartModel cartModel = snapshot.getValue(CartModel.class);
                            cartModel.setQuantity(cartModel.getQuantity()+1);
                            Map<String,Object> updateDate = new HashMap<>();
                            updateDate.put("quantity",cartModel.getQuantity());
                            updateDate.put("totalPrice",cartModel.getQuantity()*Float.parseFloat(cartModel.getPrice()));

                            userCart.child(amuletModel.getKey())
                                    .updateChildren(updateDate)
                                    .addOnSuccessListener(unused -> {
                                        iCartLoadListener.onCartLoadFail("Add To Cart Success");
                                    })
                                    .addOnFailureListener(e -> iCartLoadListener.onCartLoadFail(e.getMessage()));
                        }
                        else
                        {
                            CartModel cartModel = new CartModel();
                            cartModel.setName(amuletModel.getName());
                            cartModel.setImage(amuletModel.getImage());
                            cartModel.setKey(amuletModel.getKey());
                            cartModel.setPrice(amuletModel.getPrice());
                            cartModel.setQuantity(1);
                            cartModel.setTotalPrice(Float.parseFloat(amuletModel.getPrice()));

                            userCart.child(amuletModel.getKey())
                                    .setValue(cartModel)
                                    .addOnSuccessListener(unused -> {
                                        iCartLoadListener.onCartLoadFail("Add To Cart Success");
                                    })
                                    .addOnFailureListener(e -> iCartLoadListener.onCartLoadFail(e.getMessage()));
                        }

                        EventBus.getDefault().postSticky(new MyUpdateCartEvent());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        iCartLoadListener.onCartLoadFail(error.getMessage());
                    }
                });
    }

    @Override
    public int getItemCount() {
        return amuletModelList.size();
    }

    public class MyAmuletViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        @BindView(R.id.imageView)
        ImageView imageView;
        @BindView(R.id.txtName)
        TextView txtName;
        @BindView(R.id.txtPrice)
        TextView txtPrice;

        IRecyclerViewClickListener listener;

        public void setListener(IRecyclerViewClickListener listener) {
            this.listener = listener;
        }

        private Unbinder unbinder;
        public MyAmuletViewHolder(@NonNull View itemView) {
            super(itemView);
            unbinder = ButterKnife.bind(this,itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            listener.onRecyelerClick(v,getAdapterPosition());
        }
    }
}

package com.example.amuletshop.listener;

import com.example.amuletshop.model.AmuletModel;
import com.example.amuletshop.model.CartModel;

import java.util.List;

public interface ICartLoadListener {
    void onCartLoadSuccess(List<CartModel> cartModelList);
    void onCartLoadFail(String message);
}

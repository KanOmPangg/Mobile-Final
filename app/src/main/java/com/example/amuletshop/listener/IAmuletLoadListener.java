package com.example.amuletshop.listener;

import com.example.amuletshop.model.AmuletModel;

import java.util.List;

public interface IAmuletLoadListener {
    void onAmuletLoadSuccess(List<AmuletModel> amuletModelList);
    void onAmuletLoadFail(String message);
}

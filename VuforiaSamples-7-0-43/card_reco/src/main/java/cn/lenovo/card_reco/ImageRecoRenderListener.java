package cn.lenovo.card_reco;

/**
 * Created by linsen3 on 2018/6/5.
 */

public interface ImageRecoRenderListener {

    void imageRender(String recoName);

    void onError();

    void initARDone();
}

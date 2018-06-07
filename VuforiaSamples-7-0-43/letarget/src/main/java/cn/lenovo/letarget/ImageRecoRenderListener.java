package cn.lenovo.letarget;

/**
 * Created by linsen3 on 2018/6/5.
 */

public interface ImageRecoRenderListener {

    void imageRender(String recoName);

    void onError();

    void initARDone();
}

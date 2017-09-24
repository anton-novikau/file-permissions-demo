package by.gdgminsk.filepermissionsdemo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;


public class ImageHandlerKeeper extends Fragment {
    public static final String FRAGMENT_TAG = "ImageHandlerKeeper";

    private ImageHandler mImageHandler;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public void setImageHandler(ImageHandler loader) {
        mImageHandler = loader;
    }

    @Nullable
    public ImageHandler getImageHandler() {
        return mImageHandler;
    }
}

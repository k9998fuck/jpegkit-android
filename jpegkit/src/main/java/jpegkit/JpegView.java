package jpegkit;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class JpegView extends SurfaceView implements SurfaceHolder.Callback {

    private PixelAllocation pixelAllocation;

    public JpegView(Context context) {
        super(context);
        init();
    }

    public JpegView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        if (pixelAllocation != null) {
            renderJpeg(surfaceHolder.getSurface(),
                    pixelAllocation.getAllocHandle(),
                    pixelAllocation.getAllocSize(),
                    pixelAllocation.getWidth(),
                    pixelAllocation.getHeight());
        }
        this.surfaceHolder = surfaceHolder;
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        if (pixelAllocation != null) {
            renderJpeg(surfaceHolder.getSurface(),
                    pixelAllocation.getAllocHandle(),
                    pixelAllocation.getAllocSize(),
                    pixelAllocation.getWidth(),
                    pixelAllocation.getHeight());
        }
        this.surfaceHolder = surfaceHolder;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        this.surfaceHolder = null;
    }

    public void setJpeg(byte[] jpeg) throws JpegKitException {
        try {
            if (pixelAllocation != null) {
                pixelAllocation.release();
                pixelAllocation = null;
            }
        } catch (JpegKitException e) {
            pixelAllocation = null;
        }

        pixelAllocation = new PixelAllocation(jpeg, PixelAllocation.PIXEL_FORMAT_RGBA);
    }

    private native void renderJpeg(Surface surface, long allocHandle, long jpegSize, int width, int height);

    static {
        System.loadLibrary("jpegkit");
    }

    SurfaceHolder surfaceHolder = null;
    public void refresh() throws JpegKitException {
        if (pixelAllocation != null) {
            pixelAllocation.refresh();
        }
        if (surfaceHolder != null) {
            renderJpeg(surfaceHolder.getSurface(),
                    pixelAllocation.getAllocHandle(),
                    pixelAllocation.getAllocSize(),
                    pixelAllocation.getWidth(),
                    pixelAllocation.getHeight());
        }
    }

}

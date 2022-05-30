package jpegkit;

import android.support.annotation.NonNull;

import static libjpeg.TurboJpeg.*;

public class PixelAllocation extends JpegHandler {

    private long jpegAllocHandle;
    private int jpegAllocSize;

    private long allocHandle;
    private int allocSize;

    private byte[] jpeg;
    @PixelFormat
    private int pixelFormat;

    private int width;
    private int height;

    private PixelAllocation() {
        throw new RuntimeException("No empty constructor allowed.");
    }

    public PixelAllocation(@NonNull byte[] jpeg) throws JpegKitException {
        this(jpeg, PIXEL_FORMAT_RGB);
    }

    public PixelAllocation(@NonNull byte[] jpeg, @PixelFormat int dstPixelFormat) throws JpegKitException {
        this.jpeg = jpeg;
        this.pixelFormat = dstPixelFormat;

        // In constructor command success is required for continued use of this PixelAllocation,
        // so we need to catch any CommandException to invalidate the object's state fields
        // before re-throwing.
        try {
            jpegAllocHandle = tjAlloc(jpeg.length);
            checkCommandError();
            tjwSrcToAlloc(jpegAllocHandle, jpeg);
            checkCommandError();

            jpegAllocSize = jpeg.length;

            setJpeg(jpegAllocHandle, jpegAllocSize);
        } catch (CommandException e) {
            if (allocHandle > 0) {
                // Try to free the allocation just in case the current state
                // makes that necessary and possible.
                tjFree(allocHandle);
            }

            allocHandle = 0;
            throw e;
        }
    }

    @Override
    protected void checkStateError() throws StateException {
        if (allocHandle == -1) {
            throw new StateException("Invalid reference to JNI allocation for jpeg data.");
        }
    }

    private void setJpeg(long jpegAllocHandle, long jpegAllocSize) throws JpegKitException {
        checkStateError();

        long decompressHandle = tjInitDecompress();
        checkCommandError();

        int[] outputs = new int[3];
        tjDecompressHeader2(decompressHandle, jpegAllocHandle, jpegAllocSize, outputs);
        checkCommandError();

        width = TJSCALED(outputs[0], new int[]{1, 1});
        height = TJSCALED(outputs[1], new int[]{1, 1});

        int pitch = computePitch(outputs[0], new int[]{1, 1}, pixelFormat, false);
        int newAllocSize = pitch * TJSCALED(outputs[1], new int[]{1, 1});
        if (allocSize != newAllocSize) {
            allocSize = newAllocSize;
            if (allocHandle > 0) {
                tjFree(allocHandle);
            }
            allocHandle = tjAlloc(allocSize);
        }
        checkCommandError();

        tjDecompress2(decompressHandle, jpegAllocHandle, jpegAllocSize, allocHandle, width, pitch, height, pixelFormat, 0);
        checkCommandError();

        tjDestroy(decompressHandle);
        checkCommandError();
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getAllocSize() {
        return allocSize;
    }

    public long getAllocHandle() {
        return allocHandle;
    }

    public void release() throws JpegKitException {
        checkStateError();

        tjFree(jpegAllocHandle);
        jpegAllocHandle = -1;
        checkCommandError();
        tjFree(allocHandle);
        allocHandle = -1;
        checkCommandError();
    }

    public void refresh() throws JpegKitException {
        checkStateError();

        try {
            tjwSrcToAlloc(jpegAllocHandle, jpeg);
            checkCommandError();

            setJpeg(jpegAllocHandle, jpegAllocSize);
        } catch (CommandException e) {
            if (allocHandle > 0) {
                tjFree(allocHandle);
            }

            allocHandle = 0;
            throw e;
        }
    }

}

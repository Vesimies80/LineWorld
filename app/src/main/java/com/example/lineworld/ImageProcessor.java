package com.example.lineworld;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.media.Image;
import android.media.Image.Plane;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.nio.ByteBuffer;

import static org.opencv.imgproc.Imgproc.COLOR_YUV2GRAY_NV21;

public class ImageProcessor {
    private static final String TAG = "ImageProcessor";

    public static Mat imageToMat(Image image) {
        ByteBuffer buffer;
        int rowStride;
        int pixelStride;
        int width = image.getWidth();
        int height = image.getHeight();
        int offset = 0;

        Plane[] planes = image.getPlanes();
        byte[] data = new byte[image.getWidth() * image.getHeight() * 3 / 2];// ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8];
        byte[] rowData = new byte[planes[0].getRowStride()];

        for (int i = 0; i < planes.length; i++) {
            buffer = planes[i].getBuffer();
            rowStride = planes[i].getRowStride();
            pixelStride = planes[i].getPixelStride();
            int w = (i == 0) ? width : width / 2;
            int h = (i == 0) ? height : height / 2;
            for (int row = 0; row < h; row++) {
                int bytesPerPixel = ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8;
                if (pixelStride == bytesPerPixel) {
                    int length = w * bytesPerPixel;
                    buffer.get(data, offset, length);

                    // Advance buffer the remainder of the row stride, unless on the last row.
                    // Otherwise, this will throw an IllegalArgumentException because the buffer
                    // doesn't include the last padding.
                    if (h - row != 1) {
                        buffer.position(buffer.position() + rowStride - length);
                    }
                    offset += length;
                } else {

                    // On the last row only read the width of the image minus the pixel stride
                    // plus one. Otherwise, this will throw a BufferUnderflowException because the
                    // buffer doesn't include the last padding.
                    if (h - row == 1) {
                        buffer.get(rowData, 0, width - pixelStride + 1);
                    } else {
                        buffer.get(rowData, 0, rowStride);
                    }

                    for (int col = 0; col < w; col++) {
                        data[offset++] = rowData[col * pixelStride];
                    }
                }
            }
        }

        // Finally, create the Mat.
        Mat mat = new Mat(height + height / 2, width, CvType.CV_8UC1);
        mat.put(0, 0, data);

        return mat;
    }

    public static Bitmap detectLane(Image src) {

        Mat mLines;
        if (src.getFormat() != ImageFormat.YUV_420_888) {
            throw new IllegalArgumentException("src must have format YUV_420_888.");
        }

        Mat gray = new Mat();
        mLines = imageToMat(src);
        Imgproc.cvtColor(mLines, gray, COLOR_YUV2GRAY_NV21);

        // Blur image
        Imgproc.blur(gray, gray, new Size(3, 3));

        // Apply canny edge algorithm
        Imgproc.Canny(gray, gray, 70, 170);
        Imgproc.cvtColor(gray, gray, Imgproc.COLOR_GRAY2RGBA);

        // Extract edge colors
        Imgproc.cvtColor(mLines, mLines, Imgproc.COLOR_YUV2RGBA_I420);
        mLines.copyTo(gray, gray);

        Bitmap bmp = null;
        try {
            bmp = Bitmap.createBitmap(gray.cols(), gray.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(gray, bmp);
        } catch (CvException e) {
            Log.d("Exception", e.getMessage());
        }

        return bmp;

    }
}

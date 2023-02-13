package com.example.lineworld;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.media.Image;
import android.media.Image.Plane;
import android.provider.ContactsContract;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.CLAHE;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import static org.opencv.imgproc.Imgproc.COLOR_BGR2Lab;
import static org.opencv.imgproc.Imgproc.COLOR_BGR2YUV;
import static org.opencv.imgproc.Imgproc.COLOR_Lab2BGR;
import static org.opencv.imgproc.Imgproc.COLOR_YUV2BGR;
import static org.opencv.imgproc.Imgproc.COLOR_YUV2BGR_NV21;
import static org.opencv.imgproc.Imgproc.COLOR_YUV2GRAY_NV21;
import static org.opencv.imgproc.Imgproc.MORPH_DILATE;
import static org.opencv.imgproc.Imgproc.MORPH_RECT;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static org.opencv.imgproc.Imgproc.getStructuringElement;

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
    public static void gammaCorrection(Mat src, Mat dst, float gamma)
    {
        float invGamma = 1 / gamma;

        Mat table = new Mat(1, 256, CvType.CV_8U);
        for (int i = 0; i < 256; ++i) {
            table.put(0, i, (int) (Math.pow(i / 255.0f, invGamma) * 255));
        }

        Core.LUT(src, table, dst);
    }
    public static Bitmap detectLane(Image src) {

        Mat mLines;
        if (src.getFormat() != ImageFormat.YUV_420_888) {
            throw new IllegalArgumentException("src must have format YUV_420_888.");
        }

        Mat gray = new Mat();

        //DOESNT WORK
        //needed for absolute difference between masks

        //Mat sub = new Mat();
        //Mat dilate = new Mat();
        //Mat thresh = new Mat();
        //Mat diff = new Mat();


        //double length = 5;
        //Size siz = new Size(length, length);

        mLines = imageToMat(src);

        // Try contrast enhancement

        //Mat enhance = new Mat();
        //Imgproc.cvtColor(mLines, enhance, COLOR_YUV2BGR);
        //Imgproc.cvtColor(enhance, enhance, COLOR_BGR2Lab);

        // Clahe needs to be applied to LAB coloured image (lightness, red green, blue yellow)

        //Size grid_size = new Size(8,8);
        //CLAHE cla = Imgproc.createCLAHE(2,grid_size);

        //ArrayList<Mat> a_list = new ArrayList<Mat>();
        //Core.split(enhance, a_list);

        //Mat l_channel = a_list.get(0);
        //cla.apply(l_channel,l_channel);

        //Core.merge(a_list, enhance);

        //Imgproc.cvtColor(enhance,enhance, COLOR_Lab2BGR);
        //Imgproc.cvtColor(enhance, mLines, COLOR_BGR2YUV);


        //Basic gamma correction
        //gammaCorrection(mLines,mLines,2.2f);

        Imgproc.cvtColor(mLines, gray, COLOR_YUV2GRAY_NV21);

        //cla.apply(gray,gray);


        // Basic contrast enhancment
        //double contrast = 1.2;
        //gray.mul(gray,contrast);

        // The absolute difference between a mask and the dilated mask

        //Imgproc.threshold(gray,thresh,180,255,THRESH_BINARY);
        //sub = Imgproc.getStructuringElement(MORPH_RECT,siz);
        //Imgproc.morphologyEx(thresh,dilate,MORPH_DILATE,sub);
        //Core.absdiff(dilate, thresh, diff);

        //Scalar s = new Scalar(255);
        //Mat edges = new Mat(diff.size(),diff.type(), s);

        //Core.subtract(edges, diff, edges);
        //gray = edges;

        // Blur image
        Imgproc.blur(gray, gray, new Size(3, 3));

        // Apply canny edge algorithm

        // Sobel before canny attempt
        Imgproc.Sobel(gray, gray, -1,1,1,5);

        // original values 70 and 170
        // wide parameters like 20 and 200
        // wide is very unstable with basic contrast enhancement
        //Imgproc.Canny(gray, gray, 70, 170);
        Imgproc.cvtColor(gray, gray, Imgproc.COLOR_GRAY2RGBA);

        // Extract edge colors
        //Imgproc.cvtColor(mLines, mLines, Imgproc.COLOR_YUV2RGBA_I420);
        //mLines.copyTo(gray, gray);

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

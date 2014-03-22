/**
 * StackBlur v1.0 for Android
 *
 * @Author: Enrique López Mañas <eenriquelopez@gmail.com>
 * http://www.neo-tech.es
 *
 * Author of the original algorithm: Mario Klingemann <mario.quasimondo.com>
 *
 * This is a compromise between Gaussian Blur and Box blur
 * It creates much better looking blurs than Box Blur, but is
 * 7x faster than my Gaussian Blur implementation.
 *
 * I called it Stack Blur because this describes best how this
 * filter works internally: it creates a kind of moving stack
 * of colors whilst scanning through the image. Thereby it
 * just has to add one new block of color to the right side
 * of the stack and remove the leftmost color. The remaining
 * colors on the topmost layer of the stack are either added on
 * or reduced by one, depending on if they are on the right or
 * on the left side of the stack.
 *
 * @copyright: Enrique López Mañas
 * @license: Apache License 2.0
 */


package com.enrique.stackblur;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v8.renderscript.RSRuntimeException;
import android.util.Log;

import java.io.FileOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StackBlurManager {
	static final int EXECUTOR_THREADS = Runtime.getRuntime().availableProcessors();
	static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(EXECUTOR_THREADS);

	private static volatile boolean hasRS = true;

	/**
	 * Original image
	 */
	private final Bitmap _image;

	/**
	 * Most recent result of blurring
	 */
	private Bitmap _result;

	/**
	 * Method of blurring
	 */
	private final BlurProcess _javaBlurProcess;
	private final BlurProcess _nativeBlurProcess;
	private volatile BlurProcess _rsBlurProcess;

	/**
	 * Constructor method (basic initialization and construction of the pixel array)
	 * @param image The image that will be analyed
	 */
	public StackBlurManager(Bitmap image) {
		_image = image;
		_javaBlurProcess = new JavaBlurProcess(image);
		_nativeBlurProcess = new NativeBlurProcess(image);
		if(!hasRS)
			_rsBlurProcess = _nativeBlurProcess;
		_result = image.copy(Bitmap.Config.ARGB_8888, true);
	}

	/**
	 * Process the image on the given radius. Radius must be at least 1
	 * @param radius
	 */
	public Bitmap process(int radius) {
		_result = _javaBlurProcess.blur(radius);
		return _result;
	}

	public void process(int radius, Bitmap output) {
		_javaBlurProcess.blur(radius, output);
	}

	/**
	 * Returns the blurred image as a bitmap
	 * @return blurred image
	 */
	public Bitmap returnBlurredImage() {
		return _result;
	}

	/**
	 * Save the image into the file system
	 * @param path The path where to save the image
	 */
	public void saveIntoFile(String path) {
		try {
			FileOutputStream out = new FileOutputStream(path);
			_result.compress(Bitmap.CompressFormat.PNG, 90, out);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns the original image as a bitmap
	 * @return the original bitmap image
	 */
	public Bitmap getImage() {
		return this._image;
	}

	/**
	 * Process the image using a native library
	 */
	public Bitmap processNatively(int radius) {
		_result = _nativeBlurProcess.blur(radius);
		return _result;
	}

	public void processNatively(int radius, Bitmap output) {
		_nativeBlurProcess.blur(radius, output);
	}

	/**
	 * Process the image using renderscript if possible
	 * Fall back to native if renderscript is not available
	 * @param context renderscript requires an android context
	 * @param radius
	 */
	public Bitmap processRenderScript(Context context, float radius) {
		BlurProcess blur = _rsBlurProcess;
		// The renderscript support library doesn't have .so files for ARMv6.
		// Remember if there is an error creating the renderscript context,
		// and fall back to NativeBlurProcess
		if(blur == null) {
			synchronized (this) {
				blur = _rsBlurProcess;
				if(blur == null) {
					try {
						blur = new RSBlurProcess(context, _image);
					} catch (RSRuntimeException e) {
						if(BuildConfig.DEBUG) {
							Log.i("StackBlurManager", "Falling back to Native Blur", e);
						}
						blur = _nativeBlurProcess;
						hasRS = false;
					}
				}
			}
		}
		_result = blur.blur(radius);
		return _result;
	}

	/**
	 * Process the image using renderscript if possible
	 * Fall back to native if renderscript is not available
	 * @param context renderscript requires an android context
	 * @param radius
	 */
	public void processRenderScript(Context context, float radius, Bitmap output) {
		BlurProcess blur = _rsBlurProcess;
		// The renderscript support library doesn't have .so files for ARMv6.
		// Remember if there is an error creating the renderscript context,
		// and fall back to NativeBlurProcess
		if(blur == null) {
			synchronized (this) {
				blur = _rsBlurProcess;
				if(blur == null) {
					try {
						blur = new RSBlurProcess(context, _image);
						_rsBlurProcess = blur;
					} catch (RSRuntimeException e) {
						if(BuildConfig.DEBUG) {
							Log.i("StackBlurManager", "Falling back to Native Blur", e);
						}
						blur = _nativeBlurProcess;
						_rsBlurProcess = blur;
						hasRS = false;
					}
				}
			}
		}
		blur.blur(radius, output);
	}
}

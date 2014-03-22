package com.enrique.stackblur;

import android.graphics.Bitmap;

abstract class BlurProcess {
	protected Bitmap original;

	public Bitmap getOriginal() {
		return original;
	}

	public void setOriginal(Bitmap original) {
		this.original = original;
	}

	protected static void verifyBitmaps(Bitmap b1, Bitmap b2) {
		if(b1 == null || b2 == null) {
			throw new IllegalArgumentException("original and output both must not be null");
		}
		if(b1.getWidth() != b2.getWidth() || b1.getHeight() != b2.getHeight() ||
				b1.getConfig() != Bitmap.Config.ARGB_8888 || b2.getConfig() != Bitmap.Config.ARGB_8888
				) {
			throw new IllegalArgumentException("Both original and output bitmaps must be " +
					"the same dimensions, and be configured as ARGB_8888");
		}
	}

	/**
	 * Process the given image, blurring by the supplied radius.
	 * If radius is 0, this will return original
	 * If this method will be called often, consider using
	 * {@link #blur(float, android.graphics.Bitmap)}
	 * to prevent excess GC calls
	 *
	 * @see #blur(float, android.graphics.Bitmap)
	 * @param radius the radius in pixels to blur the image
	 * @return the blurred version of the image.
	 */
    public Bitmap blur(float radius) {
	    Bitmap copy = Bitmap.createBitmap(original.getWidth(), original.getHeight(), Bitmap.Config.ARGB_8888);
	    blur(radius, copy);
	    return copy;
    }

	/**
	 * Process the given image, blurring by the supplied radius.
	 * If radius is 0, <code>output</code> will contain the same data as <code>original</code>
	 * <code>output</code> must not be null, and must have the same Bitmap.Config as <code>original</code>
	 * <br/>
	 * This avoids excess GC calls, because a new bitmap should not be allocated on every call.
	 *
	 * @param radius radius the radius in pixels to blur the image
	 * @param output the bitmap that will contain the blurred image.
	 */
	abstract public void blur(float radius, Bitmap output);
}

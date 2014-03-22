package com.enrique.stackblur;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;

import java.io.File;
/**
 * @see JavaBlurProcess
 * Blur using renderscript.
 */
class RSBlurProcess extends BlurProcess {
	private final RenderScript _rs;
	private Allocation rowsAllocation;
	private Allocation columnsAllocation;
	private final ScriptC_blur blurScript;

	public RSBlurProcess(Context context, Bitmap original) {
		_rs = RenderScript.create(context.getApplicationContext());
		File renderscriptCacheFile = new File(context.getCacheDir(), "com.android.renderscript.cache");
		renderscriptCacheFile.mkdir();
		blurScript = new ScriptC_blur(_rs, context.getResources(), R.raw.blur);
		setOriginal(original);
	}

	@Override public void setOriginal(Bitmap original) {
		super.setOriginal(original);
		int height = original.getHeight();
		int width = original.getWidth();
		blurScript.set_width(width);
		blurScript.set_height(height);
		int[] row_indices = new int[height];
		for (int i = 0; i < height; i++) {
			row_indices[i] = i;
		}

		rowsAllocation = Allocation.createSized(_rs, Element.U32(_rs), height);
		rowsAllocation.copyFrom(row_indices);

		row_indices = new int[width];
		for (int i = 0; i < width; i++) {
			row_indices[i] = i;
		}

		columnsAllocation = Allocation.createSized(_rs, Element.U32(_rs), width);
		columnsAllocation.copyFrom(row_indices);
	}

	@Override
	public void blur(float radius, Bitmap output) {
		verifyBitmaps(original, output);

		Allocation inAllocation = Allocation.createFromBitmap(_rs, output);
		inAllocation.copyFrom(original);
		blurScript.set_gIn(inAllocation);

		blurScript.set_radius((int) radius);

		blurScript.forEach_blur_h(rowsAllocation);
		blurScript.forEach_blur_v(columnsAllocation);
		inAllocation.copyTo(output);
	}
}

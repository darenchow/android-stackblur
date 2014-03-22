package com.example.stackblurdemo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.enrique.stackblur.StackBlurManager;

import roboguice.activity.RoboActivity;
import roboguice.inject.InjectView;

public class BenchmarkActivity extends RoboActivity {

	@InjectView(R.id.progress_java)       ProgressBar _javaProgressbar;
	@InjectView(R.id.progress_native)     ProgressBar _nativeProgressbar;
	@InjectView(R.id.progress_rs)         ProgressBar _rsProgressbar;
	@InjectView(R.id.detail_java)         TextView    _javaDetail;
	@InjectView(R.id.detail_native)       TextView    _nativeDetail;
	@InjectView(R.id.detail_rs)           TextView    _rsDetail;
	@InjectView(R.id.blur_amount)         SeekBar     _blurAmt;
	@InjectView(R.id.blur_amount_detail)  TextView    _blurAmtText;
	@InjectView(R.id.result_img)          ImageView   _resultImage;

	private BenchmarkTask _benchmarkTask;
	private StackBlurManager blurManager;
	private Bitmap inBitmap;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_benchmark);

		inBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.image_transparency);
		blurManager = new StackBlurManager(inBitmap);

		_blurAmt.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				_blurAmtText.setText(progress + " px");
				if(_benchmarkTask != null) {
					_benchmarkTask.cancel(true);
				}
				_benchmarkTask = new BenchmarkTask();
				_benchmarkTask.execute(progress);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});
	}

	private class BenchmarkTask extends AsyncTask<Integer, BlurBenchmarkResult, Bitmap> {
		private int max = Integer.MIN_VALUE;
		private Bitmap outBitmap;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			_javaProgressbar.setProgress(-1);
			_nativeProgressbar.setProgress(-1);
			_rsProgressbar.setProgress(-1);

			_javaProgressbar.setIndeterminate(true);
			_nativeProgressbar.setIndeterminate(true);
			_rsProgressbar.setIndeterminate(true);

			_javaDetail.setText("");
			_nativeDetail.setText("");
			_rsDetail.setText("");
		}

		@Override
		protected Bitmap doInBackground(Integer... params) {
			if(params.length != 1 || params[0] == null)
				throw new IllegalArgumentException("Pass only 1 Integer to BenchmarkTask");
			int blurAmount = params[0];
			final Bitmap blurredBitmap = Bitmap.createBitmap(inBitmap.getWidth(), inBitmap.getHeight(), inBitmap.getConfig());
			Paint paint = new Paint();

			outBitmap = Bitmap.createBitmap(inBitmap.getWidth(), inBitmap.getHeight(), inBitmap.getConfig());
			Canvas canvas = new Canvas(outBitmap);

			System.gc();

			long time;

			BlurBenchmarkResult result;

			// Java
			time = SystemClock.elapsedRealtime();
			blurManager.process(blurAmount, blurredBitmap);
			result = new BlurBenchmarkResult("Java", (int) (SystemClock.elapsedRealtime() - time));

			if(isCancelled())
				return outBitmap;

			canvas.save();
			canvas.clipRect(0, 0, outBitmap.getWidth() / 3, outBitmap.getHeight());
			canvas.drawBitmap(blurredBitmap, 0, 0, paint);
			canvas.restore();
			publishProgress(result);

			System.gc();

			// Native
			time = SystemClock.elapsedRealtime();
			blurManager.processNatively(blurAmount, blurredBitmap);
			result = new BlurBenchmarkResult("Native", (int) (SystemClock.elapsedRealtime() - time));

			if(isCancelled())
				return outBitmap;

			canvas.save(Canvas.CLIP_SAVE_FLAG);
			canvas.clipRect(outBitmap.getWidth() / 3, 0, 2 * outBitmap.getWidth() / 3, inBitmap.getHeight());
			canvas.drawBitmap(blurredBitmap, 0, 0, paint);
			canvas.restore();
			publishProgress(result);

			System.gc();

			// Renderscript
			time = SystemClock.elapsedRealtime();
			blurManager.processRenderScript(getApplicationContext(), blurAmount, blurredBitmap);

			if(isCancelled())
				return outBitmap;

			result = new BlurBenchmarkResult("Renderscript", (int) (SystemClock.elapsedRealtime() - time));
			canvas.save();
			canvas.clipRect(2 * outBitmap.getWidth() / 3, 0, outBitmap.getWidth(), outBitmap.getHeight());
			canvas.drawBitmap(blurredBitmap, 0, 0, paint);
			canvas.restore();
			publishProgress(result);

			Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			linePaint.setStyle(Paint.Style.STROKE);
			linePaint.setStrokeWidth(getResources().getDisplayMetrics().density);
			linePaint.setColor(Color.BLACK);
			canvas.drawLine(outBitmap.getWidth() / 3, 0, outBitmap.getWidth() / 3, outBitmap.getHeight(), linePaint);
			canvas.drawLine(2 * outBitmap.getWidth() / 3, 0, 2 * outBitmap.getWidth() / 3, outBitmap.getHeight(), linePaint);

			return outBitmap;
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			super.onPostExecute(result);
			_resultImage.setImageBitmap(result);
		}

		@Override protected void onProgressUpdate(BlurBenchmarkResult... values) {
			super.onProgressUpdate(values);
			_resultImage.setImageBitmap(outBitmap);
			for (BlurBenchmarkResult benchmark : values) {
				if(benchmark == null)
					continue;
				if(benchmark._processingMillis > max) {
					max = benchmark._processingMillis;
					if(!_javaProgressbar.isIndeterminate())
						_javaProgressbar.setMax(max);
					if(!_nativeProgressbar.isIndeterminate())
						_nativeProgressbar.setMax(max);
					if(!_rsProgressbar.isIndeterminate())
						_rsProgressbar.setMax(max);
				}
				if("Java".equals(benchmark._processingType)) {
					_javaProgressbar.setIndeterminate(false);
					_javaProgressbar.setMax(max);
					_javaProgressbar.setProgress(benchmark._processingMillis);
					_javaDetail.setText(benchmark._processingMillis + " ms");
				}
				else if("Native".equals(benchmark._processingType)) {
					_nativeProgressbar.setIndeterminate(false);
					_nativeProgressbar.setMax(max);
					_nativeProgressbar.setProgress(benchmark._processingMillis);
					_nativeDetail.setText(benchmark._processingMillis + " ms");
				}
				else if("Renderscript".equals(benchmark._processingType)) {
					_rsProgressbar.setIndeterminate(false);
					_rsProgressbar.setMax(max);
					_rsProgressbar.setProgress(benchmark._processingMillis);
					_rsDetail.setText(benchmark._processingMillis + " ms");
				}
			}
		}

	}

	private static class BlurBenchmarkResult {
		public final int _processingMillis;
		public final String _processingType;

		private BlurBenchmarkResult(String processingType, int processingMillis) {
			_processingType = processingType;
			_processingMillis = processingMillis;
		}
	}
}

package com.axlecho.memo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class NewItemActivity extends SherlockActivity {

	private Button btnAddText;
	private Button btnAddPic;
	private View popupAddView;
	private PopupWindow popupAdd;
	private EditText editAddTextView;
	private TextView noteView;

	private Button btnDel;
	private Button btnSave;

	private ToolsManager tm;
	private CanvasManager cm;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_newitem);

		// TODO 适应横竖
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		noteView = (TextView) findViewById(R.id.view_note);

		popupAddView = getLayoutInflater().inflate(R.layout.menu_add, null, true);
		popupAdd = new PopupWindow(popupAddView, LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, true);
		popupAdd.setBackgroundDrawable(new BitmapDrawable());
		popupAdd.setOutsideTouchable(true);

		btnAddText = (Button) popupAddView.findViewById(R.id.btn_addtext);
		btnAddText.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

			}

		});

		editAddTextView = (EditText) findViewById(R.id.view_addnote);
		editAddTextView.addTextChangedListener(new TextWatcher() {

			@Override
			public void afterTextChanged(Editable ed) {
				noteView.setText(ed.toString());
				noteView.setVisibility(View.VISIBLE);

			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
				noteView.setVisibility(View.INVISIBLE);

			}

			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
				// TODO Auto-generated method stub

			}

		});

		btnAddPic = (Button) popupAddView.findViewById(R.id.btn_addpic);
		btnAddPic.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				Uri imageUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory(), "workupload.jpg"));
				cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
				startActivityForResult(cameraIntent, Const.CAMERARESULT);
			}

		});

		btnSave = (Button) findViewById(R.id.btn_save);
		btnSave.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				try {
					insertRecord();
				} catch (IOException e) {
					e.printStackTrace();
				}
				finish();
			}

		});

		tm = new ToolsManager(this);
		cm = new CanvasManager(this, tm.getPaint());

		btnDel = (Button) findViewById(R.id.btn_del_content);
		btnDel.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				cm.clear();
			}

		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.newitem, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_add_content:
			if (popupAdd.isShowing()) {
				popupAdd.dismiss();
			} else {
				View v = getWindow().findViewById(Window.ID_ANDROID_CONTENT);
				popupAdd.showAsDropDown(v, 0, -v.getHeight());
			}
			break;

		default:
			return super.onOptionsItemSelected(item);
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == Const.CAMERARESULT) {
			cm.setBgPic(Environment.getExternalStorageDirectory() + "/workupload.jpg");
		}
	}

	public static Bitmap PicZoom(Bitmap bmp, int width, int height, boolean rotate) {
		int bmpWidth = bmp.getWidth();
		int bmpHeght = bmp.getHeight();
		Matrix matrix = new Matrix();
		matrix.postScale((float) width / bmpWidth, (float) height / bmpHeght);
		if (rotate)
			matrix.postRotate(90);
		return Bitmap.createBitmap(bmp, 0, 0, bmpWidth, bmpHeght, matrix, true);
	}

	private void insertRecord() throws IOException {

		File destDir = new File(Environment.getExternalStorageDirectory().getPath() + "/Memo/");
		if (!destDir.exists()) {
			destDir.mkdirs();
		}
		String note = editAddTextView.getText().toString();
		String picPath = Environment.getExternalStorageDirectory().getPath() + "/Memo/" + "memo_pic_data"
				+ System.currentTimeMillis() + ".png";
		String voicePath = "";
		cm.saveToPath(picPath);

		// insert record to datebase.
		SQLiteDatabase db = this.openOrCreateDatabase("datas", MODE_PRIVATE, null);
		ContentValues record = new ContentValues();
		record.put("note", note);
		record.put("pic_path", picPath);
		record.put("voice_path", voicePath);
		long rowid = db.insert("memo_datas", null, record);
		Log.i("axlecho", "插入数据库结果：" + rowid);
		db.close();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
		} else {
		}
	}

	class AnimotionManager {
		private SurfaceView sfv;
		private SurfaceHolder sfh;
		private final int DELETEANIMOTION = 0;
		private final int RESET = 1;
		private int height;
		private int width;
		private float tarWidthIn;
		private float tarWidthOut;
		private Timer timer;
		private Bitmap tarBtm;

		public AnimotionManager(Activity parent) {
			initImageView(parent);
		}

		public void setButtonBgAnimation(Button btn, int size) {
			size = size + 1;
			Drawable bgdrawable = btn.getBackground();
			int w = bgdrawable.getIntrinsicWidth();
			int h = bgdrawable.getIntrinsicHeight();
			Bitmap bitmap = Bitmap.createBitmap(w, h, Config.ARGB_4444);

			Paint paint = new Paint();
			paint.setAntiAlias(true);
			paint.setARGB(0xff, 0x22, 0x22, 0x22);
			paint.setTextSize(8);
			paint.setFakeBoldText(true);
			paint.setShadowLayer(2, 1.532f, 1.285f, 0xFF222222);
			Canvas canvas = new Canvas(bitmap);

			bitmap.eraseColor(Color.WHITE);
			canvas.drawText(String.valueOf(size), w / 2 + 3, h / 2 - 0.5f, paint);

			float x = -1;
			float y = -1;
			if (size <= 5) {

				x = w / 2.0f - 1;
				y = h / 2.0f + 2;
			} else {
				x = w / 2.0f - 1 - (size - 5) * 0.707f;
				y = h / 2.0f + 2 + (size - 5) * 0.707f;
			}

			canvas.drawCircle(x, y, size, paint);

			BitmapDrawable bd = new BitmapDrawable(bitmap);
			btn.setBackgroundDrawable(bd);
		}

		private void initImageView(Activity parent) {
			sfv = (SurfaceView) parent.findViewById(R.id.view_image_del);
			sfh = sfv.getHolder();
			sfv.setZOrderOnTop(true);
			sfh.setFormat(PixelFormat.TRANSLUCENT);
		}

		int test = 0;

		private void freeDeformation(float animWidthOutSize, float animWidth, float dx, float dy, Canvas cs) {

			// float dx = 1;
			// float dy = 4;
			float unitX = cs.getWidth() / dx;
			float unitY = cs.getHeight() / dy;
			float t = 0.4f;

			List<Bezier> bis = new ArrayList<Bezier>();
			for (int i = 0; i <= dx; i++) {
				Point psrc = new Point(width / dx * i, 0);
				Point pdst = new Point((tarWidthOut - animWidth) / dx * i + animWidth, height);
				Point c1 = new Point(psrc.x, height * t);
				Point c2 = new Point(pdst.x, height * (1 - t));
				Bezier bi = new Bezier(psrc, pdst, c1, c2);

				bis.add(bi);
			}

			for (int index = 1; index < bis.size(); index++) {
				List<Point> end = bis.get(index).getPoints(dy);
				List<Point> begin = bis.get(index - 1).getPoints(dy);

				for (int pointi = 1; pointi < dy + 1; pointi++) {
					float[] src = new float[] { 0, 0, // 左上
							unitX, 0,// 右上
							unitX, unitY,// 右下
							0, unitY // 左下
					};

					float xs[] = { begin.get(pointi - 1).x, end.get(pointi - 1).x, begin.get(pointi).x,
							end.get(pointi).x };
					float ys[] = { begin.get(pointi - 1).y, end.get(pointi - 1).y, begin.get(pointi).y,
							end.get(pointi).y };

					float[] dst = new float[] { 0, 0, // 左上
							xs[1] - xs[0], ys[1] - ys[0],// 右上
							xs[3] - xs[0], ys[3] - ys[0],// 右下
							xs[2] - xs[0], ys[2] - ys[0] // 左下
					};

					Matrix mx = new Matrix();
					mx.setPolyToPoly(src, 0, dst, 0, src.length >> 1);

					try {
						Bitmap bm = Bitmap.createBitmap(tarBtm, (int) unitX * (index - 1), (int) unitY * (pointi - 1),
								(int) unitX, (int) unitY, mx, false);
						// Bitmap bm = Bitmap.createBitmap(tarBtm, (int) unitX *
						// (index - 1), (int) unitY * (pointi - 1),
						// (int) unitX, (int) unitY);
						Log.i("axlecho", "count:" + ++test);
						cs.drawBitmap(bm, begin.get(pointi - 1).x, begin.get(pointi - 1).y - 1, null);
						bm.recycle();
						bm = null;

					} catch (IllegalArgumentException e) {
						Paint tpaint = new Paint();
						tpaint.setColor(Color.BLACK);
						tpaint.setAlpha(100);
						tpaint.setStrokeWidth(2);
						cs.drawLine(begin.get(pointi - 1).x, begin.get(pointi - 1).y, begin.get(pointi).x,
								begin.get(pointi).y, tpaint);
					}
				}
			}

		}

		private void drawPath(float aniWidth, Canvas cs) {
			Paint canvasClear = new Paint();
			canvasClear.setAlpha(0);
			canvasClear.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
			cs.drawRect(0, 0, width, height, canvasClear);

			Path tmpPath = new Path();
			tmpPath.moveTo(0, 0);
			tmpPath.cubicTo(0, height * 0.2f, aniWidth, height * 0.6f, aniWidth, height);
			tmpPath.lineTo(tarWidthOut, height);
			tmpPath.cubicTo(width, height * 0.2f, tarWidthOut, height * 0.6f, width, 0);
			tmpPath.lineTo(0, 0);

			Paint paint = new Paint();
			paint.setColor(0);
			paint.setAlpha(20);
			cs.drawPath(tmpPath, paint);
			cs.clipPath(tmpPath);
		}

		Handler handler = new Handler() {
			private int animWidth = 0;
			private int animHeigt = -1;

			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				switch (msg.what) {
				case DELETEANIMOTION:
					if (animWidth <= tarWidthIn) {
						Canvas ac = sfh.lockCanvas();

						// Debug.startMethodTracing();
						drawPath(animWidth, ac);
						freeDeformation(tarWidthOut, animWidth, 1, 1, ac);
						// Debug.stopMethodTracing();
						sfh.unlockCanvasAndPost(ac);
						animWidth += 20;
					} else {
						if (animHeigt > height) {
							timer.cancel();
							return;
						}
						animHeigt += 18;
						Rect r = new Rect(0, 0, width, animHeigt);
						Canvas canvas = sfh.lockCanvas(r);
						Paint canvasClear = new Paint();
						canvasClear.setAlpha(0);
						canvasClear.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
						canvas.drawRect(0, 0, width, animHeigt, canvasClear);
						sfh.unlockCanvasAndPost(canvas);
					}
					break;
				case RESET:
					animWidth = 0;
					animHeigt = 0;
					break;
				default:
					break;
				}
			}
		};

		public void delAnimotion(Bitmap srcBtm) {
			tarBtm = srcBtm;
			height = srcBtm.getHeight() - btnDel.getHeight() - 10;
			width = srcBtm.getWidth();

			tarWidthIn = width - btnDel.getWidth() - 5.0f;
			tarWidthOut = width - 5.0f;

			handler.sendEmptyMessage(RESET);
			timer = new Timer();
			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					handler.sendEmptyMessage(DELETEANIMOTION);
				}
			}, 0, 5);

			handler.sendEmptyMessage(DELETEANIMOTION);

		}

	}

	class CanvasManager {

		private Bitmap btmImage;
		private ImageView imageView;
		private Canvas canvasImage;

		private Bitmap btmSurface;
		private ImageView imageSurfaceView;
		private Canvas canvasSurface;

		private Path tmpPath;
		private float old_x;
		private float old_y;

		private Paint paint;
		private AnimotionManager am;

		public CanvasManager(Activity parent, Paint paint) {
			this.paint = paint;
			initImageView(parent);
			initImageSurfaceView(parent);
			am = new AnimotionManager(parent);
		}

		public void initImageView(Activity parent) {
			imageView = (ImageView) parent.findViewById(R.id.view_image_context);
			ViewTreeObserver vto = imageView.getViewTreeObserver();
			vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
				@Override
				public void onGlobalLayout() {
					imageView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
					btmImage = Bitmap.createBitmap(imageView.getWidth(), imageView.getHeight(), Config.ARGB_8888);
					imageView.setImageBitmap(btmImage);
					canvasImage = new Canvas(btmImage);
				}
			});
		}

		public void initImageSurfaceView(Activity parent) {
			imageSurfaceView = (ImageView) parent.findViewById(R.id.view_image_surface);
			ViewTreeObserver vto = imageSurfaceView.getViewTreeObserver();
			vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
				@Override
				public void onGlobalLayout() {
					imageSurfaceView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
					btmSurface = Bitmap.createBitmap(imageSurfaceView.getWidth(), imageSurfaceView.getHeight(),
							Config.ARGB_8888);
					imageSurfaceView.setImageBitmap(btmSurface);
					canvasSurface = new Canvas(btmSurface);
				}
			});

			imageSurfaceView.setOnTouchListener(new OnTouchListener() {
				@Override
				public boolean onTouch(View arg0, MotionEvent me) {
					if (me.getAction() == MotionEvent.ACTION_DOWN) {
						old_x = me.getX();
						old_y = me.getY();
						tmpPath = new Path();
						tmpPath.moveTo(me.getX(), me.getY());
					} else if (me.getAction() == MotionEvent.ACTION_MOVE) {
						if (tmpPath != null)
							canvasSurface.drawPath(tmpPath, paint);
						final float dx = Math.abs(me.getX() - old_x);
						final float dy = Math.abs(me.getY() - old_y);

						// 两点之间的距离大于等于3时，生成贝塞尔绘制曲线
						if (dx >= 3 || dy >= 3) {
							// 设置贝塞尔曲线的操作点为起点和终点的一半
							float cX = (me.getX() + old_x) / 2;
							float cY = (me.getY() + old_y) / 2;
							// 二次贝塞尔，实现平滑曲线；previousX, previousY为操作点，cX, cY为终点
							tmpPath.quadTo(old_x, old_y, cX, cY);
						}

						old_x = me.getX();
						old_y = me.getY();
						imageSurfaceView.invalidate();
					} else if (me.getAction() == MotionEvent.ACTION_UP) {
						tmpPath.lineTo(me.getX(), me.getY());
						canvasSurface.drawPath(tmpPath, paint);
						imageSurfaceView.invalidate();
					}
					return true;
				}
			});
		}

		public void setBgPic(String path) {
			Bitmap camerabitmap = BitmapFactory.decodeFile(path);
			if (null != camerabitmap) {
				// 下面这两句是对图片按照一定的比例缩放，这样就可以完美地显示出来。

				int oldWidth = camerabitmap.getWidth();
				int oldHeight = camerabitmap.getHeight();
				int newWidth = canvasImage.getWidth();
				int newHeight = canvasImage.getHeight();
				boolean flagRotate = false;
				int scale = 1;
				if ((oldWidth - oldHeight) * (newWidth - newHeight) < 0) {
					flagRotate = true;
					scale = oldWidth / newHeight;
				} else {
					scale = oldWidth / newWidth;
				}
				Bitmap b = PicZoom(camerabitmap, camerabitmap.getWidth() / scale, camerabitmap.getHeight() / scale,
						flagRotate);

				canvasImage.drawBitmap(b, 0, 0, null);

				File f = new File(Environment.getExternalStorageDirectory() + "/workupload.jpg");
				f.delete();
			}
		}

		public void saveToPath(String path) throws IOException {
			// combine two layout
			canvasImage.drawBitmap(btmSurface, 0, 0, null);

			// save the image context.
			File f = new File(path);
			f.createNewFile();
			FileOutputStream fOut = new FileOutputStream(f);
			btmImage.compress(Bitmap.CompressFormat.PNG, 100, fOut);
			fOut.flush();
			fOut.close();
		}

		public void clear() {

			Bitmap bm = btmSurface.copy(Config.ARGB_8888, false);
			am.delAnimotion(bm);
			clearBg();
		}

		public void clearSurface() {

			Paint canvasClear = new Paint();
			canvasClear.setAlpha(0);
			canvasClear.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
			canvasSurface.drawRect(0, 0, canvasSurface.getWidth(), canvasSurface.getHeight(), canvasClear);
			imageSurfaceView.invalidate();
		}

		public void clearBg() {
			Paint canvasClear = new Paint();
			canvasClear.setAlpha(0);
			canvasClear.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
			canvasImage.drawRect(0, 0, canvasImage.getWidth(), canvasImage.getHeight(), canvasClear);
			imageView.invalidate();
		}
	}

	class ToolsManager {
		private Button btnEraser;
		private Button btnPen;

		private Button btnSelectColor;
		private View popupColorView;
		private PopupWindow popupColor;
		private Button btnSelectGreen;
		private Button btnSelectBlue;
		private Button btnSelectRed;
		private Button btnSelectYellow;
		private Button btnSelectBlack;
		private Button btnSelectIvory;
		private Button btnSelectPurple;

		private Button btnSelectSize;
		private View popupSizeView;
		private PopupWindow popupSize;
		private SeekBar seekbarSize;
		private TextView penSizeView;

		private Paint paint;

		private ColorSelectOnClickListener csOnClickListener;

		private AnimotionManager am;

		public ToolsManager(Activity parent) {

			csOnClickListener = new ColorSelectOnClickListener(parent.getResources());
			am = new AnimotionManager(parent);
			initPaint();
			initPenEraser(parent);
			initPopupSize(parent);
			initPopupColor(parent);

		}

		private void initPenEraser(Activity parent) {
			btnEraser = (Button) parent.findViewById(R.id.btn_eraser);
			btnEraser.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					paint.setAlpha(0);
					paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
					// btnSelectColor.setVisibility(View.GONE);
					// btnEraser.setVisibility(View.GONE);
					// btnPen.setVisibility(View.VISIBLE);
					btnPen.setBackgroundDrawable(getResources().getDrawable(R.drawable.pen));
					btnEraser.setBackgroundDrawable(getResources().getDrawable(R.drawable.eraserpress));
				}

			});

			btnPen = (Button) parent.findViewById(R.id.btn_pen);
			btnPen.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					paint.setAlpha(255);
					paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
					// btnSelectColor.setVisibility(View.VISIBLE);
					// btnEraser.setVisibility(View.VISIBLE);
					// btnPen.setVisibility(View.GONE);
					btnPen.setBackgroundDrawable(getResources().getDrawable(R.drawable.penpress));
					btnEraser.setBackgroundDrawable(getResources().getDrawable(R.drawable.eraser));
				}
			});

			// btnPen.setVisibility(View.GONE);
			btnPen.setBackgroundDrawable(getResources().getDrawable(R.drawable.penpress));
		}

		private void initPaint() {
			paint = new Paint();
			paint.setColor(Const.DEFAULTCOLOR);
			paint.setStrokeWidth(Const.DEFAULTPENSIZE);
			paint.setAntiAlias(true);
			paint.setStyle(Style.STROKE);
		}

		private int popupSizeHeight = -1;
		private int popupColorHeight = -1;

		private void initPopupSize(Activity parent) {
			popupSizeView = parent.getLayoutInflater().inflate(R.layout.menu_selectsize, null, true);
			popupSize = new PopupWindow(popupSizeView, LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, true);
			popupSize.setBackgroundDrawable(new BitmapDrawable());
			popupSize.setOutsideTouchable(true);

			penSizeView = (TextView) popupSizeView.findViewById(R.id.view_penSize);
			seekbarSize = (SeekBar) popupSizeView.findViewById(R.id.seekbar_selectsize);
			seekbarSize.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

				@Override
				public void onProgressChanged(SeekBar arg0, int progress, boolean fromUser) {
					penSizeView.setText("" + progress);
					paint.setStrokeWidth(progress);
					am.setButtonBgAnimation(btnSelectSize, progress);
				}

				@Override
				public void onStartTrackingTouch(SeekBar arg0) {

				}

				@Override
				public void onStopTrackingTouch(SeekBar arg0) {

				}

			});

			int w = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
			int h = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
			popupSizeView.measure(w, h);
			popupSizeHeight = popupSizeView.getMeasuredHeight();

			btnSelectSize = (Button) findViewById(R.id.btn_selectsize);
			btnSelectSize.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (popupSize.isShowing()) {
						popupSize.dismiss();
					} else {
						popupSize.showAsDropDown(v, 0, -(v.getHeight() + popupSizeHeight));
					}
				}

			});

			am.setButtonBgAnimation(btnSelectSize, seekbarSize.getProgress());
		}

		private void initPopupColor(Activity parent) {
			popupColorView = parent.getLayoutInflater().inflate(R.layout.menu_selectcolor, null, true);
			popupColor = new PopupWindow(popupColorView, LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, true);
			popupColor.setBackgroundDrawable(new BitmapDrawable());
			popupColor.setOutsideTouchable(true);

			btnSelectGreen = (Button) popupColorView.findViewById(R.id.btn_select_green);
			btnSelectBlue = (Button) popupColorView.findViewById(R.id.btn_select_blue);
			btnSelectRed = (Button) popupColorView.findViewById(R.id.btn_select_red);
			btnSelectYellow = (Button) popupColorView.findViewById(R.id.btn_select_yellow);
			btnSelectBlack = (Button) popupColorView.findViewById(R.id.btn_select_black);
			btnSelectIvory = (Button) popupColorView.findViewById(R.id.btn_select_ivory);
			btnSelectPurple = (Button) popupColorView.findViewById(R.id.btn_select_purple);

			btnSelectGreen.setOnClickListener(csOnClickListener);
			btnSelectBlue.setOnClickListener(csOnClickListener);
			btnSelectRed.setOnClickListener(csOnClickListener);
			btnSelectYellow.setOnClickListener(csOnClickListener);
			btnSelectBlack.setOnClickListener(csOnClickListener);
			btnSelectIvory.setOnClickListener(csOnClickListener);
			btnSelectPurple.setOnClickListener(csOnClickListener);

			int w = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
			int h = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
			popupColorView.measure(w, h);
			popupColorHeight = popupColorView.getMeasuredHeight();

			btnSelectColor = (Button) findViewById(R.id.btn_selectcolor);
			btnSelectColor.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (popupColor.isShowing()) {
						popupColor.dismiss();
					} else {
						popupColor.showAsDropDown(v, 0, -(v.getHeight() + popupColorHeight + 5));

					}
				}

			});
		}

		private class ColorSelectOnClickListener implements OnClickListener {
			private Resources r;

			public ColorSelectOnClickListener(Resources r) {
				this.r = r;
			}

			@Override
			public void onClick(View v) {
				paint.setAlpha(255);
				paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
				switch (v.getId()) {
				case R.id.btn_select_green:
					paint.setColor(r.getColor(R.color.green));
					break;
				case R.id.btn_select_black:
					paint.setColor(r.getColor(R.color.black));
					break;
				case R.id.btn_select_blue:
					paint.setColor(r.getColor(R.color.blue));
					break;
				case R.id.btn_select_ivory:
					paint.setColor(r.getColor(R.color.ivory));
					break;
				case R.id.btn_select_purple:
					paint.setColor(r.getColor(R.color.purple));
					break;
				case R.id.btn_select_red:
					paint.setColor(r.getColor(R.color.red));
					break;
				case R.id.btn_select_yellow:
					paint.setColor(r.getColor(R.color.yellow));
					break;
				default:
					break;
				}

				popupColor.dismiss();
			}
		}

		public Paint getPaint() {
			return paint;
		}
	}

}

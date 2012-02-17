package com.snda.gyue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.snda.gyue.network.NetFiles;
import com.snda.gyue.utils.ImageUtils;
import com.snda.gyue.utils.ShareUtils;

public class ViewArticleActivity extends Activity implements OnClickListener {

	Button btnBack;
	ProgressBar pbRefreshing;
	TextView tvArticle;
	TextView tvTitle, tvDate;
	ScrollView layContent;
	TextView tvSeeWeb;
	RelativeLayout layLoading, laySharing;
	ImageGetter iGetter;
	Handler hPack;
	ImageView imgShareTencent, imgShareSina, imgShareTencent2, imgShareSina2;
	TextView tvDownloadApk;
	RelativeLayout layZoom;
	Button btnZoomIn, btnZoomOut;

	boolean inProgress = false;
	boolean tmrEd = false;

	int fontSize = 16;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.view_article);

		btnBack = (Button) findViewById(R.id.btnBack);
		pbRefreshing = (ProgressBar) findViewById(R.id.pbRefreshing);
		tvArticle = (TextView) findViewById(R.id.tvArticle);
		tvTitle = (TextView) findViewById(R.id.tvTitle);
		tvDate = (TextView) findViewById(R.id.tvDate);
		layContent = (ScrollView) findViewById(R.id.layContent);
		tvSeeWeb = (TextView) findViewById(R.id.tvSeeWeb);
		layLoading = (RelativeLayout) findViewById(R.id.layLoading);
		laySharing = (RelativeLayout) findViewById(R.id.laySharing);
		imgShareTencent = (ImageView) findViewById(R.id.imgShareTencent);
		imgShareSina = (ImageView) findViewById(R.id.imgShareSina);
		imgShareTencent2 = (ImageView) findViewById(R.id.imgShareTencent2);
		imgShareSina2 = (ImageView) findViewById(R.id.imgShareSina2);
		tvDownloadApk = (TextView) findViewById(R.id.tvDownloadApk);
		layZoom = (RelativeLayout) findViewById(R.id.layZoom);
		btnZoomIn = (Button) findViewById(R.id.btnZoomIn);
		btnZoomOut = (Button) findViewById(R.id.btnZoomOut);

		btnBack.setOnClickListener(this);
		tvSeeWeb.setOnClickListener(this);
		imgShareSina.setOnClickListener(this);
		imgShareTencent.setOnClickListener(this);
		imgShareSina2.setOnClickListener(this);
		imgShareTencent2.setOnClickListener(this);
		tvDownloadApk.setOnClickListener(this);
		btnZoomIn.setOnClickListener(this);
		btnZoomOut.setOnClickListener(this);

		// TextSize
		fontSize = PreferenceManager.getDefaultSharedPreferences(this).getInt("font-size", 24);

		tvTitle.setText(GlobalInstance.currentArticle.getTitle());
		tvDate.setText(GlobalInstance.currentArticle.getDate());
		tvArticle.setTextSize(fontSize);

		if (GlobalInstance.currentArticle.getDownloadApkUrl() != null
				&& !GlobalInstance.currentArticle.getDownloadApkUrl().equals("")) {
			tvDownloadApk.setVisibility(View.VISIBLE);
		}

		boolean needShowDownload = getIntent().getBooleanExtra("needShowDownload", false);
		if (needShowDownload && tvDownloadApk.getVisibility() == View.GONE) {
			tvDownloadApk.setText(R.string.go_web);
			tvDownloadApk.setVisibility(View.VISIBLE);
		}

		hPack = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				if (msg.what == 99) {
					int y = layContent.getScrollY();
					setTextView();
					layContent.scrollTo(0, y);
				}
				super.handleMessage(msg);
			}
		};

		if (!getIntent().getBooleanExtra("no_pic", false)) {
			NetFiles.doDownloadImagePackT(this, getImages(GlobalInstance.currentArticle.getComment()), hPack);
		}

		iGetter = new ImageGetter() {

			@Override
			public Drawable getDrawable(String source) {
				Drawable drawable = null;

				if (!getIntent().getBooleanExtra("no_pic", false)) {

					String local = NetFiles.buildLocalFileName(source);
					BitmapFactory.Options bop = new BitmapFactory.Options();

					File fImg = new File(local);
					if (fImg.length() > 102400) {
						bop.inSampleSize = 2;
					} else {
						bop.inSampleSize = 1;
					}

					if (!fImg.exists()) {

						return new BitmapDrawable(getResources(), BitmapFactory.decodeResource(getResources(),
								R.drawable.empty, bop));
					}
					Bitmap bmp = ImageUtils.doMatrix(BitmapFactory.decodeFile(local, bop), 0,
							GlobalInstance.metric.widthPixels, GlobalInstance.metric.heightPixels);
					drawable = new BitmapDrawable(getResources(), bmp);
					drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
					return drawable;
				}
				return new BitmapDrawable(getResources(),
						BitmapFactory.decodeResource(getResources(), R.drawable.empty));
			}
		};

		GlobalInstance.currentArticle.setComment(cutLastBR(GlobalInstance.currentArticle.getComment()));
		setTextView();
	}

	private void setTextView() {
		tmrEd = false;
		final Handler hTmr = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				if (msg.what == 1) {
					tmrEd = true;
					if (!inProgress) {
						pbRefreshing.setVisibility(View.GONE);
						layLoading.setVisibility(View.GONE);
						layZoom.setVisibility(View.VISIBLE);
					}
				}
				super.handleMessage(msg);
			}
		};
		final Timer tmr = new Timer();
		tmr.schedule(new TimerTask() {
			@Override
			public void run() {
				tmr.cancel();
				hTmr.sendEmptyMessage(1);
			}
		}, 1000);

		inProgress = true;
		pbRefreshing.setVisibility(View.VISIBLE);
		layLoading.setVisibility(View.VISIBLE);
		layZoom.setVisibility(View.GONE);
		final Handler h = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				if (msg.what == 1) {
					inProgress = false;
					if (tmrEd) {
						pbRefreshing.setVisibility(View.GONE);
						layLoading.setVisibility(View.GONE);
						layZoom.setVisibility(View.VISIBLE);
					}
				}
				super.handleMessage(msg);
			}
		};

		tvArticle.post(new Runnable() {

			@Override
			public void run() {
				String comment = GlobalInstance.currentArticle.getComment();
//				Log.e("ARTICLE", comment);
				tvArticle.setText(Html.fromHtml(comment, iGetter, null));
				h.sendEmptyMessage(1);
			}
		});
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btnZoomIn:
			fontSize++;
			tvArticle.setTextSize(fontSize);
			PreferenceManager.getDefaultSharedPreferences(this).edit().putInt("font-size", fontSize).commit();
			break;
		case R.id.btnZoomOut:
			fontSize--;
			tvArticle.setTextSize(fontSize);
			PreferenceManager.getDefaultSharedPreferences(this).edit().putInt("font-size", fontSize).commit();
			break;
		case R.id.tvSeeWeb:
			Intent inSeeWeb = new Intent(Intent.ACTION_VIEW);
			inSeeWeb.setData(Uri.parse(GlobalInstance.currentArticle.getLink()));
			startActivity(inSeeWeb);
			break;
		case R.id.btnBack:
			finish();
			break;
		case R.id.imgShareSina:
		case R.id.imgShareSina2:
			if (GlobalInstance.sinaName.equals("")) {
				Toast.makeText(this, R.string.not_bind_sina, Toast.LENGTH_LONG).show();
				return;
			}
			shareToSinaT();
			break;
		case R.id.imgShareTencent:
		case R.id.imgShareTencent2:
			if (GlobalInstance.tencentName.equals("")) {
				Toast.makeText(this, R.string.not_bind_tencent, Toast.LENGTH_LONG).show();
				return;
			}
			shareToTencentT();
			break;
		case R.id.tvDownloadApk:
			Intent inDownload = new Intent(Intent.ACTION_VIEW);
			if (GlobalInstance.currentArticle.getDownloadApkUrl() == null
					|| GlobalInstance.currentArticle.getDownloadApkUrl().equals("")) {
				inDownload.setData(Uri.parse(GlobalInstance.currentArticle.getLink()));
			} else {
				inDownload.setData(Uri.parse(GlobalInstance.currentArticle.getDownloadApkUrl()));
			}
			startActivity(inDownload);
			break;
		}

	}

	private void shareToSinaT() {
		laySharing.setVisibility(View.VISIBLE);
		pbRefreshing.setVisibility(View.VISIBLE);
		layZoom.setVisibility(View.GONE);
		final Handler h = new Handler() {

			@Override
			public void handleMessage(Message msg) {
				if (msg.what == 1) {
					Toast.makeText(ViewArticleActivity.this,
							(msg.arg1 == 1 ? R.string.share_sina_ok : R.string.share_sina_fail), Toast.LENGTH_LONG)
							.show();
					laySharing.setVisibility(View.GONE);
					pbRefreshing.setVisibility(View.GONE);
					layZoom.setVisibility(View.VISIBLE);
				}
				super.handleMessage(msg);
			}

		};

		new Thread(new Runnable() {

			@Override
			public void run() {
				boolean bSina = ShareUtils.shareArticleToSina(GlobalInstance.currentArticle);
				Message msg = new Message();
				msg.what = 1;
				msg.arg1 = (bSina ? 1 : 0);
				h.sendMessage(msg);
			}
		}).start();
	}

	private void shareToTencentT() {
		laySharing.setVisibility(View.VISIBLE);
		pbRefreshing.setVisibility(View.VISIBLE);
		layZoom.setVisibility(View.GONE);
		final Handler h = new Handler() {

			@Override
			public void handleMessage(Message msg) {
				if (msg.what == 1) {
					Toast.makeText(ViewArticleActivity.this,
							(msg.arg1 == 1 ? R.string.share_tencent_ok : R.string.share_tencent_fail),
							Toast.LENGTH_LONG).show();
					laySharing.setVisibility(View.GONE);
					pbRefreshing.setVisibility(View.GONE);
					layZoom.setVisibility(View.VISIBLE);
				}
				super.handleMessage(msg);
			}

		};

		new Thread(new Runnable() {

			@Override
			public void run() {
				boolean bTencent = ShareUtils.shareArticleToTencent(GlobalInstance.currentArticle);
				Message msg = new Message();
				msg.what = 1;
				msg.arg1 = (bTencent ? 1 : 0);
				h.sendMessage(msg);
			}
		}).start();
	}

	public static List<String> getImages(String htmlStr) {
		Pattern p_image;
		Matcher m_image;
		List<String> pics = new ArrayList<String>();

		String regEx_img = "http://[([a-z0-9]|.|/|\\-)]+.[(jpg)|(bmp)|(gif)|(png)]";
		p_image = Pattern.compile(regEx_img, Pattern.CASE_INSENSITIVE);
		m_image = p_image.matcher(htmlStr);
		while (m_image.find()) {
			pics.add(m_image.group());
		}
		return pics;
	}
	
	private String cutLastBR(String comment) {
		String tmp = comment;
		while (tmp.endsWith("<br />")) {
			tmp = tmp.substring(0, tmp.length()-6).trim();
		}
		return tmp;
	}
}
package com.socks.jiandan.ui;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.nostra13.universalimageloader.utils.DiskCacheUtils;
import com.socks.jiandan.R;
import com.socks.jiandan.base.BaseActivity;
import com.socks.jiandan.constant.ToastMsg;
import com.socks.jiandan.utils.CacheUtil;
import com.socks.jiandan.utils.FileUtil;
import com.socks.jiandan.utils.ScreenSizeUtil;
import com.socks.jiandan.utils.ShareUtil;
import com.socks.jiandan.utils.ShowToast;

import java.io.File;

import butterknife.ButterKnife;
import butterknife.InjectView;
import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher;

/**
 * 图片详情页
 * Created by zhaokaiqiang on 15/4/21.
 */
public class ImageDetailActivity extends BaseActivity implements View.OnClickListener {

	@InjectView(R.id.img_back)
	ImageButton img_back;
	@InjectView(R.id.img_share)
	ImageButton img_share;
	@InjectView(R.id.web_gif)
	WebView webView;
	@InjectView(R.id.img)
	PhotoView img;
	@InjectView(R.id.progress)
	ProgressBar progress;

	@InjectView(R.id.tv_like)
	TextView tv_like;
	@InjectView(R.id.tv_unlike)
	TextView tv_unlike;
	@InjectView(R.id.img_comment)
	ImageButton img_comment;
	@InjectView(R.id.img_download)
	ImageButton img_download;
	@InjectView(R.id.ll_bottom_bar)
	LinearLayout ll_bottom_bar;
	@InjectView(R.id.rl_top_bar)
	RelativeLayout rl_top_bar;

	private String author;
	private String[] img_urls;
	private String id;
	private String threadKey;
	private boolean is_need_webview;

	private DisplayImageOptions options;
	private ImageLoader imageLoader;
	private File imgCacheFile;
	private String imgPath;

	private boolean isBarShow = true;
	private boolean isImgHaveLoad = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_image_detail);
		ButterKnife.inject(this);
		initView();
		initData();
	}

	@Override
	public void initView() {
		img_back.setOnClickListener(this);
		img_share.setOnClickListener(this);
		tv_like.setOnClickListener(this);
		tv_unlike.setOnClickListener(this);
		img_comment.setOnClickListener(this);
		img_download.setOnClickListener(this);

	}

	@Override
	public void onClick(View v) {

		switch (v.getId()) {
			case R.id.img_back:
				finish();
				break;
			case R.id.img_share:
				String[] urls = img_urls[0].split("\\.");
				File cacheFile = imageLoader.getDiskCache().get(img_urls[0]);

				//如果不存在，则使用缩略图进行分享
				if (!cacheFile.exists()) {
					String picUrl = img_urls[0];
					picUrl = picUrl.replace("mw600", "small").replace("mw1200", "small").replace
							("large", "small");
					cacheFile = imageLoader.getDiskCache().get(picUrl);
				}

				File newFile = new File(CacheUtil.getSharePicName
						(cacheFile, urls));

				if (FileUtil.copyTo(cacheFile, newFile)) {
					ShareUtil.sharePicture(this, newFile.getAbsolutePath(),
							"分享自煎蛋增强版 " + img_urls[0]);
				} else {
					ShowToast.Short(ToastMsg.LOAD_SHARE);
				}
				break;
			case R.id.tv_like:
				ShowToast.Short("OO");
				break;
			case R.id.tv_unlike:
				ShowToast.Short("XX");
				break;
			case R.id.img_comment:
				ShowToast.Short("吐槽");
				break;
			case R.id.img_download:
				ShowToast.Short("保存");
				break;
		}

	}

	@Override
	public void initData() {
		Intent intent = getIntent();
		author = intent.getStringExtra("img_author");
		img_urls = intent.getStringArrayExtra("img_url");
		id = intent.getStringExtra("img_id");
		threadKey = intent.getStringExtra("thread_key");
		is_need_webview = intent.getBooleanExtra("is_need_webview", false);

		imageLoader = ImageLoader.getInstance();
		options = new DisplayImageOptions.Builder()
				.cacheInMemory(true)
				.cacheOnDisk(true)
				.bitmapConfig(Bitmap.Config.RGB_565)
				.resetViewBeforeLoading(true)
				.considerExifParams(true)
				.imageScaleType(ImageScaleType.EXACTLY)
				.build();

		if (is_need_webview) {
			webView.getSettings().setJavaScriptEnabled(true);
			webView.addJavascriptInterface(new JSObject(), "external");
			webView.setWebViewClient(new WebViewClient() {
				@Override
				public boolean shouldOverrideUrlLoading(WebView view, String url) {
					webView.loadUrl(url);
					return true;
				}

			});

			webView.setWebChromeClient(new WebChromeClient());
			webView.setBackgroundColor(Color.BLACK);

			img.setVisibility(View.GONE);

			imageLoader.displayImage(img_urls[0], img, options, new
					SimpleImageLoadingListener() {

						@Override
						public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {

							progress.setVisibility(View.GONE);

							imgCacheFile = DiskCacheUtils.findInCache(img_urls[0], imageLoader.getDiskCache());
							if (imgCacheFile != null) {
								imgPath = "file://" + imgCacheFile.getAbsolutePath();
								showImgInWebView(imgPath);
								isImgHaveLoad = true;
							}

						}

						@Override
						public void onLoadingStarted(String imageUri, View view) {
							progress.setVisibility(View.VISIBLE);
						}

						@Override
						public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
							progress.setVisibility(View.GONE);
							ShowToast.Short("加载失败" + failReason.getType().name());
						}
					});

		} else {

			imageLoader.loadImage(img_urls[0], options, new
					SimpleImageLoadingListener() {
						@Override
						public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {

							progress.setVisibility(View.GONE);

							if (loadedImage.getHeight() > ScreenSizeUtil
									.getScreenWidth(ImageDetailActivity.this)) {
								imgCacheFile = DiskCacheUtils.findInCache(img_urls[0], imageLoader.getDiskCache());
								if (imgCacheFile != null) {
									imgPath = "file://" + imgCacheFile.getAbsolutePath();
									img.setVisibility(View.GONE);
									showImgInWebView(imgPath);
									isImgHaveLoad = true;
								}
							} else {
								img.setImageBitmap(loadedImage);
								isImgHaveLoad = true;
							}

						}

						@Override
						public void onLoadingStarted(String imageUri, View view) {
							progress.setVisibility(View.VISIBLE);
						}

						@Override
						public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
							progress.setVisibility(View.GONE);
						}
					});

		}

		img.setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {
			@Override
			public void onViewTap(View view, float x, float y) {
				toggleBar();
			}
		});

	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		toggleBar();
	}

	private void toggleBar() {

		if (isImgHaveLoad) {

			if (isBarShow) {
				//隐藏
				isBarShow = false;

				ObjectAnimator
						.ofFloat(ll_bottom_bar, "translationY", 0, ll_bottom_bar.getHeight())
						.setDuration(400)
						.start();

				ObjectAnimator
						.ofFloat(rl_top_bar, "translationY", 0, -rl_top_bar.getHeight())
						.setDuration(400)
						.start();

			} else {
				//显示
				isBarShow = true;

				ObjectAnimator
						.ofFloat(ll_bottom_bar, "translationY", ll_bottom_bar.getHeight(), 0)
						.setDuration(400)
						.start();

				ObjectAnimator
						.ofFloat(rl_top_bar, "translationY", -rl_top_bar.getHeight(), 0)
						.setDuration(400)
						.start();

			}
		}

	}


	/**
	 * 使用ImageView加载GIF图片
	 *
	 * @param s
	 */
	private void showImgInWebView(final String s) {
		if (webView != null) {
			webView.loadDataWithBaseURL("", "<!doctype html> <html lang=\"en\"> <head> <meta charset=\"UTF-8\"> <title></title><style type=\"text/css\"> html,body{width:100%;height:100%;margin:0;padding:0;background-color:black;} *{ -webkit-tap-highlight-color: rgba(0, 0, 0, 0);}#box{ width:100%;height:100%; display:table; text-align:center; background-color:black;} body{-webkit-user-select: none;user-select: none;-khtml-user-select: none;}#box span{ display:table-cell; vertical-align:middle;} #box img{  width:100%;} </style> </head> <body> <div id=\"box\"><span><img src=\"img_url\" alt=\"\"></span></div> <script type=\"text/javascript\" >document.body.onclick=function(e){window.external.onClick();e.preventDefault(); };function load_img(){var url=document.getElementsByTagName(\"img\")[0];url=url.getAttribute(\"src\");var img=new Image();img.src=url;if(img.complete){\twindow.external.img_has_loaded();\treturn;};img.onload=function(){window.external.img_has_loaded();};img.onerror=function(){\twindow.external.img_loaded_error();};};load_img();</script></body> </html>".replace("img_url", s), "text/html", "utf-8", "");
		}
	}

	public class JSObject {

		@JavascriptInterface
		public void img_has_loaded() {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
				}
			});
		}

		@JavascriptInterface
		public void img_loaded_error() {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					ShowToast.Short(ToastMsg.LOAD_FAILED);
				}
			});
		}

		@JavascriptInterface
		public void onClick() {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					toggleBar();
				}
			});
		}
	}

}
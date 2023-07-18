package com.privatecircle.markets;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private static final int INPUT_FILE_REQUEST_CODE = 1;
    private static final String TAG = MainActivity.class.getSimpleName();
    private ValueCallback<Uri[]> uploadMessage;
    private String cameraPhotoPath = null;
    private long size = 0;

    // Storage Permissions variables
    private static final int PERMISSSION_REQUEST_CODE = 47;
    private static final String[] PERMISSIONS_ALL = {
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.CAMERA
    };

    private WebView webView;
    private ProgressBar progressBar;
    private String[] allowedUrls = {
            "https://accounts.google.com/o/oauth2/v2/auth",
            "https://www.linkedin.com/oauth/v2/authorization"
    };

    private boolean doubleBackToExitPressedOnce = false;
    private Handler exitHandler;

    /**
     * The function handles the retrieval and processing of images from a data object, and then passes
     * the results to the `uploadMessage` object.
     *
     * @param requestCode The `requestCode` parameter is an integer value that represents the code that
     *                    was used to start the activity for result. It is used to identify which activity is returning a
     *                    result when multiple activities are started for result.
     * @param resultCode  The `resultCode` parameter is an integer value that represents the result of
     *                    the activity that was started with `startActivityForResult()`. It indicates whether the activity
     *                    completed successfully or not. The value `Activity.RESULT_OK` indicates that the activity
     *                    completed successfully, while other values may indicate different outcomes or errors.
     * @param data        The `data` parameter is an `Intent` object that contains the result data from the
     *                    activity that was started for a result. It can contain various types of data, depending on the
     *                    activity that was started. In this case, it is used to retrieve images from the data object
     *                    using the `get
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != INPUT_FILE_REQUEST_CODE || uploadMessage == null) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }
        try {
            String file_path = cameraPhotoPath.replace("file:", "");
            File file = new File(file_path);
            size = file.length();

        } catch (Exception e) {
            Log.e("Error!", "Error while opening image file" + e.getLocalizedMessage());
        }

        // The above code is handling the retrieval and processing of images from a data object. It first
        // checks if the data object or the cameraPhotoPath is not null. If not null, it tries to retrieve the
        // images from the data object using the getClipData() method. If an exception occurs, it logs the
        // error message.
        if (data != null || cameraPhotoPath != null) {
            Integer count = 0;
            ClipData images = null;
            // The above code is trying to retrieve the images from the data object using the getClipData() method.
            // If an exception occurs, it will log the error message.
            try {
                images = data.getClipData();
            } catch (Exception e) {
                Log.e("Error!", e.getLocalizedMessage());
            }

            // The above code is checking the values of the variables `images` and `data` to determine
            // the value of the variable `count`.
            if (images == null && data != null && data.getDataString() != null) {
                count = data.getDataString().length();
            } else if (images != null) {
                count = images.getItemCount();
            }
            Uri[] results = new Uri[count];
            // Check that the response is a good one
            // The code is checking the result code of an activity. If the result code is
            // RESULT_OK, it then checks if the size is not 0. If the size is not 0, it checks if the
            // cameraPhotoPath is not null. If it is not null, it creates a new Uri array with a single
            // element containing the Uri parsed from the cameraPhotoPath.
            if (resultCode == Activity.RESULT_OK) {
                if (size != 0) {
                    // If there is not data, then we may have taken a photo
                    if (cameraPhotoPath != null) {
                        results = new Uri[]{Uri.parse(cameraPhotoPath)};
                    }
                } else if (data.getClipData() == null) {
                    results = new Uri[]{Uri.parse(data.getDataString())};
                } else {

                    for (int i = 0; i < images.getItemCount(); i++) {
                        results[i] = images.getItemAt(i).getUri();
                    }
                }
            }

            // The above code is invoking the `onReceiveValue` method of the `uploadMessage` object and passing the
            // `results` parameter. After that, it sets the `uploadMessage` object to `null`.
            uploadMessage.onReceiveValue(results);
            uploadMessage = null;
        }
    }

    /**
     * The function checks if the app has read, write, and camera permissions, and if not, it requests
     * them from the user.
     *
     * @param activity The "activity" parameter is an instance of the Activity class in Android. It
     *                 represents the current activity that is calling this method.
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have read or write permission
        int writePermission = ActivityCompat.checkSelfPermission(activity.getApplicationContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int readPermission = ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.READ_EXTERNAL_STORAGE);
        int cameraPermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA);

        if (writePermission != PackageManager.PERMISSION_GRANTED || readPermission != PackageManager.PERMISSION_GRANTED || cameraPermission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_ALL,
                    PERMISSSION_REQUEST_CODE
            );
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        exitHandler = new Handler();
        verifyStoragePermissions(this);

        webView = findViewById(R.id.webview);
        WebSettings webViewSettings = webView.getSettings();

        progressBar = findViewById(R.id.loading_progressbar);
        // set progress bar color
        progressBar.setProgressTintList(ColorStateList.valueOf(Color.rgb(0,69,248)));

        // The above code is setting the WebViewClient of the WebView to a new instance of the WebViewClient class.
        webView.setWebViewClient(new WebViewClient() {
            /**
             * The function checks if a URL starts with "mailto:" or "tel:" and opens the corresponding
             * app, otherwise it checks if the URL belongs to a specific website or matches a specific
             * condition and either loads it in the WebView or opens it in another Activity.
             *
             * @param view The WebView instance that is making the request.
             * @param request The request parameter is of type WebResourceRequest and represents the
             * request made by the WebView to load a resource. It contains information such as the URL
             * of the resource, the HTTP headers, and other metadata.
             * @return The method shouldOverrideUrlLoading is returning a boolean value.
             */
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (
                        request.getUrl().toString().startsWith("mailto:") || request.getUrl().toString().startsWith("tel:")
                ) {
                    view.getContext().startActivity(
                            new Intent(Intent.ACTION_VIEW, request.getUrl()));
                    return true;
                }

                return false;
            }
        });

        webViewSettings.setUserAgentString(System.getProperty("http.agent"));
        webViewSettings.setJavaScriptEnabled(true);
        webViewSettings.setBuiltInZoomControls(true);
        webViewSettings.setDisplayZoomControls(false);
        webViewSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webViewSettings.setDomStorageEnabled(true);
        webViewSettings.setAllowFileAccess(true);
        webViewSettings.setAllowFileAccessFromFileURLs(true);
        webViewSettings.setTextZoom(100);
        webViewSettings.setUseWideViewPort(true);
//      webViewSettings.setLoadWithOverviewMode(true);
        webView.setWebChromeClient(new WebChromeClient() {
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> filePath, WebChromeClient.FileChooserParams fileChooserParams) {
                // Double check that we don't have any existing callbacks
                if (uploadMessage != null) {
                    uploadMessage.onReceiveValue(null);
                }
                uploadMessage = filePath;
                Log.e("FileCooserParams => ", filePath.toString());

                // The above code is creating an intent to launch the device's camera application to
                // take a picture.
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                // The above code is checking if there is an activity available to handle the "take
                // picture" intent. If there is, it creates a file to store the photo, sets the file
                // path as an extra in the intent, and sets the intent to capture the image and store
                // it in the specified file. If the file creation is unsuccessful, it logs an error.
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    // Create the File where the photo should go
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                        takePictureIntent.putExtra("PhotoPath", cameraPhotoPath);
                    } catch (IOException ex) {
                        // Error occurred while creating the File
                        Log.e(TAG, "Unable to create Image File", ex);
                    }

                    // Continue only if the File was successfully created
                    if (photoFile != null) {
                        cameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                    } else {
                        takePictureIntent = null;
                    }
                }

                // The above code is creating an Intent object for selecting content. Specifically, it
                // is creating an intent to get content from the user, with a focus on selecting
                // multiple image files. The intent is set to allow multiple selections and the type is
                // set to "image/*", which means it will only show options for selecting image files.
                Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                contentSelectionIntent.setType("image/*");

                // The above code is creating an array of Intents called `intentArray`. If the
                // `takePictureIntent` is not null, then the `intentArray` is initialized with a single
                // element, which is the `takePictureIntent`. Otherwise, if `takePictureIntent` is
                // null, the `intentArray` is initialized with an array of size 2.
                Intent[] intentArray;
                if (takePictureIntent != null) {
                    intentArray = new Intent[]{takePictureIntent};
                } else {
                    intentArray = new Intent[2];
                }

                // The above code is creating an intent to open a chooser dialog for selecting images.
                // It sets the contentSelectionIntent as the main intent for the chooser and adds
                // additional intents to be shown in the chooser. It also sets the title of the chooser
                // dialog as "Image Chooser". Finally, it starts the chooser activity and expects a
                // result with the selected images.
                Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
                startActivityForResult(Intent.createChooser(chooserIntent, "Select images"), 1);

                return true;

            }

            /**
             * The function updates the progress of a WebView loading process and sets the progress on
             * a ProgressBar.
             *
             * @param view The WebView instance on which the progress change occurred.
             * @param newProgress The new progress value of the WebView loading, represented as an
             * integer. It ranges from 0 to 100, where 0 indicates no progress and 100 indicates that
             * the page has been fully loaded.
             */
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                progressBar.setProgress(newProgress);
            }
        });

        // set webAppUrl
        String webAppUrl = "https://markets.privatecircle.co/";

        webView.loadUrl(webAppUrl);
    }

    /**
     * The function checks if a given URL starts with any of the allowed URLs.
     *
     * @param url The "url" parameter is a string that represents the URL that needs to be checked.
     * @return The method is returning a boolean value. It returns true if the given URL starts with
     * any of the allowed URLs, and false otherwise.
     */
    public boolean checkUrl(String url) {
        for (String allowedUrl : allowedUrls) {
            if (url.startsWith(allowedUrl)) {
                return true;
            }
        }
        return false;
    }


//    private class CustomWebViewClient extends WebViewClient {
//        @Override
//        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
//            if (
//                    request.getUrl().toString().startsWith("mailto:") || request.getUrl().toString().startsWith("tel:")
//            ) {
//                view.getContext().startActivity(
//                        new Intent(Intent.ACTION_VIEW, request.getUrl()));
//                return true;
//            }
//
//            if ("markets-staging.privatecircle.co".equals(request.getUrl().getHost())) {
//                // This is my website, so do not override; let my WebView load the page
//                return false;
//            }
//            // Otherwise, the link is not for a page on my site, so launch another Activity that handles URLs
//            Intent intent = new Intent(Intent.ACTION_VIEW, request.getUrl());
//            startActivity(intent);
//            return true;
//        }
//    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File imageFile = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        return imageFile;
    }

//    // Handle back functionality with android hardware button navigation.
//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        // Check if the key event was the Back button and if there's history
//        if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
//            webView.goBack();
//            return true;
//        }
//        // If it wasn't the Back key or there's no web page history, bubble up to the default
//        // system behavior (probably exit the activity)
//        return super.onKeyDown(keyCode, event);
//    }

    @Override
    public void onBackPressed() {
        // Check if the web view can go back
        if (webView.canGoBack()) {
            // If the web view can go back, go back
            webView.goBack();
            return;
        }
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show();

        // Reset the flag after 3 seconds
        exitHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 3000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove any pending exitHandler callbacks to prevent memory leaks
        exitHandler.removeCallbacksAndMessages(null);
    }

    public void showSoftKeyboard(View view) {
        if (view.requestFocus()) {
            InputMethodManager imm = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }
}
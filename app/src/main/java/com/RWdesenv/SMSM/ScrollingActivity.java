package com.RWdesenv.SMSM;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.woxthebox.draglistview.DragItem;
import com.woxthebox.draglistview.DragListView;

import org.jcodec.api.android.AndroidSequenceEncoder;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Rational;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

import android.os.Handler;


public class ScrollingActivity extends AppCompatActivity {
    private static final String PROJECT_LINK = "https://play.google.com/store/apps/details?id=com.RWdesenv.SMSM";
    private static final int CAMERA_REQUEST = 4711;
    public static String PACKAGE_NAME;
    private ActionBar ab = null;
    private FFmpeg ffmpeg = null;

    private boolean optHigh = false;
    private boolean optSlow = false;

    private boolean optAmovie = false;
    private boolean optRap = false;

    private ArrayList<Pair<Long, PicEntry>> picEntries;
    private EntryAdapter entryAdapter;
    private TextView emptyView;

    private String shareFile = "";
    private AdView mAdView;
    private InterstitialAd mInterstitialAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scrolling);
        PACKAGE_NAME = getApplicationContext().getPackageName();

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });

        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(this,"ca-app-pub-2697339358784861/2817268596", adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                // The mInterstitialAd reference will be null until
                // an ad is loaded.
                mInterstitialAd = interstitialAd;
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {

                mInterstitialAd = null;
            }
        });
/*
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId("ca-app-pub-2697339358784861/2817268596");
        mInterstitialAd.loadAd(new AdRequest.Builder().build());
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                // Load the next interstitial.
                mInterstitialAd.loadAd(new AdRequest.Builder().build());
            }
        });
*/
        mAdView = findViewById(R.id.adView);
//        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST);
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1111);
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 2222);
        }

        try {
            ab = getSupportActionBar();
            if (ab != null) {
                ab.setDisplayShowHomeEnabled(true);
                ab.setHomeButtonEnabled(true);
                ab.setDisplayUseLogoEnabled(true);
                ab.setLogo(R.mipmap.ic_launcher);
                ab.setTitle("  " + getString(R.string.app_name));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        DragListView entryList = (DragListView) findViewById(R.id.picList);
        emptyView = (TextView) findViewById(android.R.id.empty);

        picEntries = new ArrayList<>();
        entryList.setLayoutManager(new LinearLayoutManager(this));
        entryAdapter = new EntryAdapter(this, picEntries, R.layout.entry_line, R.id.line_title, false);
        entryList.setAdapter(entryAdapter, true);
        entryList.setCanDragHorizontally(false);
        entryList.getRecyclerView().setVerticalScrollBarEnabled(true);
        entryList.setCustomDragItem(new MyDragItem(this, R.layout.entry_line));

        ffmpeg = FFmpeg.getInstance(this);
        try {
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {
                @Override
                public void onStart() {}
                @Override
                public void onFailure() {}
                @Override
                public void onSuccess() {}
                @Override
                public void onFinish() {}
            });
        } catch (FFmpegNotSupportedException e) {
            // Handle if FFmpeg is not supported by device
            emptyView.setText(R.string.no_support);
            ffmpeg = null;
        }
    }

    private void takePic() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST);
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
             ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1111);
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
             ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 2222);
        } else {

            emptyView.setVisibility(View.GONE);

            if (optHigh) {
                File image = createAppFile("_temp.jpg");

                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    Uri photoURI = FileProvider.getUriForFile(getApplicationContext(),
                            "com.RWdesenv.SMSM.fileprovider",
                            image
                    );
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    startActivityForResult(takePictureIntent, CAMERA_REQUEST);
                }
            } else {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(takePictureIntent, CAMERA_REQUEST);
                }
            }

        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_scrolling, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_movie) {
            if (mInterstitialAd != null) {
                mInterstitialAd.show(this);
            }
            emptyView.setVisibility(View.VISIBLE);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST);
            } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1111);
            } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 2222);
            } else {
                if (ab != null) ab.setTitle(R.string.processing);
                if (optAmovie) {
                    entryAdapter.asort();
                } else {
                    entryAdapter.sort();
                }
                shareFile = "";
                new ToMovieTask().execute(optRap, optSlow);
            }
            return true;
        } else if (id == R.id.action_share) {
            if (shareFile.length() > 0) {
                final Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                sharingIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.created_by));
                Uri furi = FileProvider.getUriForFile(
                        getApplicationContext(),
                        "com.RWdesenv.SMSM.fileprovider",
                        createAppFile(shareFile)
                );
                sharingIntent.setDataAndType(furi, getContentResolver().getType(furi));
                sharingIntent.putExtra(Intent.EXTRA_STREAM, furi);
                sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(sharingIntent, getString(R.string.action_share)));
            } else {
                emptyView.setText(R.string.action_picture_video);
            }
        } else if (id == R.id.action_amovie) {
            if (item.isChecked()) {
                item.setChecked(false);
                optAmovie = false;
            } else {
                item.setChecked(true);
                optAmovie = true;
            }
            return true;
        } else if (id == R.id.action_slow) {
            if (item.isChecked()) {
                item.setChecked(false);
                optSlow = false;
            } else {
                item.setChecked(true);
                optSlow = true;
            }
            return true;
        } else if (id == R.id.action_rapmovie) {
            if (item.isChecked()) {
                item.setChecked(false);
                optRap = false;
            } else {
                item.setChecked(true);
                optRap = true;
            }
            return true;
        } else if (id == R.id.action_high) {
            if (item.isChecked()) {
                item.setChecked(false);
                optHigh = false;
            } else {
                item.setChecked(true);
                optHigh = true;
            }
            return true;
      /*  } else if (id == R.id.action_info) {
            Intent intentProj = new Intent(Intent.ACTION_VIEW, Uri.parse(PROJECT_LINK));
            startActivity(intentProj);
            return true; */
        } else if (id == R.id.action_picture) {
            takePic();
            return true;
        } else if (id == R.id.action_rw) {
            goRW();
            return true;
        } else if (id == R.id.action_other_ads) {
            goSMSM();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void goSMSM() {
        //if (mInterstitialAd.isLoaded()) {
           // mInterstitialAd.show();
        //}
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("message/rfc822");
                i.putExtra(Intent.EXTRA_EMAIL  , new String[]{"rwdesenv.tech@gmail.com"});
                i.putExtra(Intent.EXTRA_SUBJECT, "Improvements...");
                try {
                    startActivity(Intent.createChooser(i, "Send mail..."));
                } catch (android.content.ActivityNotFoundException ex) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.RWdesenv.SMSM")));
                    } catch (android.content.ActivityNotFoundException anfe) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.RWdesenv.SMSM")));
                    }
                }
            }
        }, 6000);
    }

    private void goRW() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=RWdesenv")));
        } catch (android.content.ActivityNotFoundException anfe) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/search?q=RWdesenv")));
        }
    }

    private class ToMovieTask extends AsyncTask<Boolean, Void, Boolean>{
        private String nameBasic;
        private File file;

        @Override
        protected Boolean doInBackground(Boolean... bools) {
            FileChannelWrapper out = null;
            nameBasic = (new SimpleDateFormat("yyyyMMddHHmmss")).format(new Date());
            file = createAppFile(nameBasic + ".mp4");

            try {
                out = NIOUtils.writableFileChannel(file.getAbsolutePath());
                AndroidSequenceEncoder encoder;
                if (bools[1]) {
                    encoder = new AndroidSequenceEncoder(out, Rational.R(8, 1));
                } else {
                    encoder = new AndroidSequenceEncoder(out, Rational.R(13, 1));
                }
                if (bools[0]) entryAdapter.sort();

                for (Pair<Long, PicEntry> pe : picEntries) {
                    encoder.encodeImage(pe.second.pic);
                }

                if (bools[0]) {
                    entryAdapter.asort();
                    for (Pair<Long, PicEntry> pe : picEntries) {
                        encoder.encodeImage(pe.second.pic);
                    }
                }

                encoder.finish();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return false;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            } finally {
                NIOUtils.closeQuietly(out);

                if (ffmpeg != null) {
                    // ------------------------------------------------- makes a gif file
                    String[] cmdgif = {"-i", file.getPath(), getAppFolder().getPath() + "/" + nameBasic + ".gif"};
                    conversion(cmdgif, false);

                    // --------------------------------------------- makes whatsapp compatible file
                    String[] cmd_wa = {
                            "-i", file.getPath(),
                            "-c:v", "libx264",
                            "-profile:v", "baseline",
                            "-level", "3.0",
                            "-pix_fmt", "yuv420p",
                            getAppFolder().getPath() + "/" + nameBasic + "whatsapp.mp4"
                    };
                    conversion(cmd_wa, true);
                }

            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            if (ab != null) {
                ab.setTitle("  " + getString(R.string.app_name));
            }
            if (aBoolean) {
                emptyView.setText(R.string.ok);
                shareFile = nameBasic + ".gif";
                picEntries.clear();
                entryAdapter.notifyDataSetChanged();
            } else {
                emptyView.setText(R.string.fail);
                shareFile = "";
                picEntries.clear();
                entryAdapter.notifyDataSetChanged();
            }
            super.onPostExecute(aBoolean);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
            PicEntry pe = new PicEntry();
            long order = picEntries.size();
            pe.title = String.format("%03d", order);

            if (optHigh) {
                Bitmap photo = BitmapFactory.decodeFile(
                        getAppFolder().getPath() + "/_temp.jpg"
                );
                pe.pic = Bitmap.createScaledBitmap(
                        photo,
                        2 * (photo.getWidth() / 10),
                        2 * (photo.getHeight() / 10),
                        true
                );
                File f = createAppFile("_temp.jpg");
                try {
                    f.getCanonicalFile().delete();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Bitmap photo = (Bitmap) data.getExtras().get("data");
                pe.pic = Bitmap.createScaledBitmap(
                        photo,
                        (photo.getWidth() / 2) * 2,
                        (photo.getHeight() / 2) * 2,
                        true
                );
            }

            picEntries.add(new Pair<Long, PicEntry>(order, pe));
            emptyView.setText("");
            entryAdapter.asort();
            entryAdapter.notifyDataSetChanged();

            Toast.makeText(this, R.string.picadd, Toast.LENGTH_SHORT).show();
        }
    }

    private void conversion(final String[] cmd, final boolean delFinal) {

        try {
            // to execute "ffmpeg -version" command you just need to pass "-version"
            ffmpeg.execute(cmd, new ExecuteBinaryResponseHandler() {

                @Override
                public void onStart() { }

                @Override
                public void onProgress(String message) { }

                @Override
                public void onFailure(String message) {
                    emptyView.setText(R.string.fail);
                    shareFile = "";
                    picEntries.clear();
                    entryAdapter.notifyDataSetChanged();
                }

                @Override
                public void onSuccess(String message) {
                    if (delFinal) {
                        File f = createAppFile(cmd[1]);
                        try {
                            if (f==null) return;
                            f.getCanonicalFile().delete();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onFinish() { }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            // Handle if FFmpeg is already running
            e.printStackTrace();
        }
    }

    private File getAppFolder() {
        File file = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            file = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                    getPackageName()
            );
        } else {
            file = new File(Environment.getExternalStorageDirectory() + "/Documents/"+getPackageName());
        }

        try {
            if (!file.exists()) file.mkdirs();
        } catch (Exception e) {
            return null;
        }
        return file;
    }

    private File createAppFile(String name) {
        File file = getAppFolder();

        String path = file.getPath() + "/" + name;
        try {
            file = new File(path);
            if (!file.exists()) file.createNewFile();
        } catch (Exception e) {
            return null;
        }
        return file;
    }

    private static class MyDragItem extends DragItem {

        MyDragItem(Context context, int layoutId) {
            super(context, layoutId);
        }

        @Override
        public void onBindDragView(View clickedView, View dragView) {
            CharSequence text = ((TextView) clickedView.findViewById(R.id.line_title)).getText();
            Drawable dr = ((ImageView) clickedView.findViewById(R.id.line_pic)).getDrawable();
            ((TextView) dragView.findViewById(R.id.line_title)).setText(text);
            ((ImageView) dragView.findViewById(R.id.line_pic)).setImageDrawable(dr);
        }
    }
}

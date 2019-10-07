package com.example.boademo;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.renderscript.ScriptGroup;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.box.androidsdk.content.BoxApi;
import com.box.androidsdk.content.BoxApiFile;
import com.box.androidsdk.content.BoxApiFolder;
import com.box.androidsdk.content.BoxConfig;
import com.box.androidsdk.content.BoxConstants;
import com.box.androidsdk.content.BoxException;
import com.box.androidsdk.content.auth.BoxAuthentication;
import com.box.androidsdk.content.listeners.ProgressListener;
import com.box.androidsdk.content.models.BoxDownload;
import com.box.androidsdk.content.models.BoxFile;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.models.BoxIteratorItems;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.requests.BoxRequestsFile;
import com.example.boademo.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;


import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Text;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Date;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;


public class UploadDownload extends AppCompatActivity implements BoxAuthentication.AuthListener {

    private ArrayAdapter<BoxItem> mAdapter;
    private ListView mListView;
    private BoxApiFolder mFolderApi;
    private BoxApiFile mFileApi;
    private FloatingActionButton mFab;
    private static final int READ_REQUEST_CODE = 42;
    private static final int WRITE_REQUEST_CODE = 43;
    private BoxSession session;
    private SwipeRefreshLayout mRefresh;
    private Toolbar toolbar;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch(item.getItemId()) {
            case R.id.logout :
                Intent intent = new Intent(UploadDownload.this, MainActivity.class);
                startActivity(intent);
                finish();
                session.logout();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_download);
        mRefresh = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
        mListView = (ListView) this.findViewById(R.id.List);
        mAdapter = new BoxItemAdapter(this);
        mListView.setAdapter(mAdapter);
        mFab = (FloatingActionButton) this.findViewById(R.id.floatingActionButton);
        toolbar = (Toolbar) this.findViewById(R.id.toolbar);
        toolbar.setTitle("Bank Of America");
        setSupportActionBar(toolbar);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE} , 1);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE} , 2);
        }



        mRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadList();
                mRefresh.setRefreshing(false);
            }
        });
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                startActivityForResult(intent, READ_REQUEST_CODE);
            }
        });

        configure();
        Log.i("init", "calleds");
        initializeSession();

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {

                }
                return;
            }
            case 2: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {

                }
                return;
            }

        }
    }


    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                final Uri uri = data.getData();


                new Thread() {
                    @Override
                    public void run() {
                        String name = getFileName(uri);
                        Log.i("name", name);
                        File test = new File(uri.getPath());
                        Log.i("Tag", test.getName());
                        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + name);
                        /*while (file != null) {
                            Log.d("FILE", file + " exists=" + file.exists());
                            file = file.getParentFile();
                        }*/
                        Log.i("test", "URI: " + file.getAbsolutePath());
                        try {
                            file.createNewFile();
                            //InputStream uploadStream = new FileInputStream(file);
                            BoxFile uploadedFile = mFileApi.getUploadRequest(file , "89279863257").send();
                        } catch (BoxException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            }
        }

    }

    public void loadList() {

        new Thread() {
            @Override
            public void run() {
                try {
                    final BoxIteratorItems folderItems = mFolderApi.getItemsRequest("/89279863257").send();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mAdapter.clear();
                            for (BoxItem boxItem: folderItems) {
                                mAdapter.add(boxItem);
                            }
                        }
                    });
                } catch (BoxException e) {
                    e.printStackTrace();
                }

            }
        }.start();
    }
    private void clearAdapter() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.clear();
            }
        });
    }

    @Override
    public void onAuthCreated(BoxAuthentication.BoxAuthenticationInfo info) {
        mFolderApi = new BoxApiFolder(session);
        mFileApi = new BoxApiFile(session);
        loadList();

    }

    @Override
    public void onAuthFailure(BoxAuthentication.BoxAuthenticationInfo info, Exception ex) {
        clearAdapter();
    }

    public void initializeSession() {
        //clearAdapter();
        session = new BoxSession(this);
        session.setSessionAuthListener(this);
        session.authenticate(this);

    }

    @Override
    public void onRefreshed(BoxAuthentication.BoxAuthenticationInfo info) {

    }

    @Override
    public void onLoggedOut(BoxAuthentication.BoxAuthenticationInfo info, Exception ex) {
        clearAdapter();
        initializeSession();
    }
    public void configure() {
        BoxConfig.CLIENT_ID = "og2yhjcbqc0rc4x2zzpcjamu9e832bbm";
        BoxConfig.CLIENT_SECRET = "qYg7jr9TV4ItU2Sw9FbwQxy1G5MUFfOX";
        BoxConfig.IS_DEBUG = true;
        BoxConfig.IS_LOG_ENABLED = true;
        BoxConfig.REDIRECT_URL = "https://localhost";
    }
    private class BoxItemAdapter extends ArrayAdapter<BoxItem> {
        public BoxItemAdapter(Context context) {
            super(context, 0);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final BoxItem item = getItem(position);
            final BoxApiFile fileApi = new BoxApiFile(session);
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.filelayout, parent, false);
            }

            ImageView download = (ImageView) convertView.findViewById(R.id.download);

            download.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new Thread() {
                        @Override
                        public void run() {
                            try {
                                File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), item.getName());
                                Log.i("Tag", file.getAbsolutePath());
                                if (!file.exists()) {
                                    file.mkdir();
                                    Log.i("Tag", "Directory Created");
                                }

                                BoxDownload fileDownload = fileApi.getDownloadRequest(file, item.getId()).send();
                                File output = fileDownload.getOutputFile();
                                if (!output.exists()) {
                                    output.createNewFile();
                                    Log.i("Tag", "File Created");
                                }
                                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                                intent.addCategory(Intent.CATEGORY_OPENABLE);
                                Log.i("Tag", fileDownload.getContentType());
                                intent.setType(fileDownload.getContentType());
                                intent.putExtra(Intent.EXTRA_TITLE, output.getName());
                                startActivityForResult(intent, WRITE_REQUEST_CODE);

                            }
                            catch (BoxException e) {
                                e.printStackTrace();
                            }
                            catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                    }.start();
                }
            });

            TextView name = (TextView) convertView.findViewById(R.id.name);
            name.setText(item.getName());
            return convertView;
        }

        public void checkPermission(String permission, int requestCode)
        {
            if (ContextCompat.checkSelfPermission(
                    UploadDownload.this,
                    permission)
                    == PackageManager.PERMISSION_DENIED) {
                ActivityCompat
                        .requestPermissions(
                                UploadDownload.this,
                                new String[] { permission },
                                requestCode);
            }
            else {
                Toast
                        .makeText(UploadDownload.this,
                                "Permission already granted",
                                Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }
}

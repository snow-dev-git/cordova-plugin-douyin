package jason.he.cordova.douyin;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.content.FileProvider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class Util {

    /**
     * Read bytes from InputStream
     *
     * @param inputStream
     * @return
     * @throws IOException
     * @link http://stackoverflow.com/questions/2436385/android-getting-from-a-uri-to-an-inputstream-to-a-byte-array
     */
    public static byte[] readBytes(InputStream inputStream) throws IOException {
        // this dynamically extends to take the bytes you read
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

        // this is storage overwritten on each iteration with bytes
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        // we need to know how may bytes were read to write them to the byteBuffer
        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }

        // and then we can return your byte array.
        return byteBuffer.toByteArray();
    }

    public static File getCacheFolder(Context context) {
        File cacheDir = null;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            cacheDir = new File(Environment.getExternalStorageDirectory(), "cache");
            if (!cacheDir.isDirectory()) {
                cacheDir.mkdirs();
            }
        }

        if (!cacheDir.isDirectory()) {
            cacheDir = context.getCacheDir(); // get system cache folder
        }

        return cacheDir;
    }

    public static File downloadAndCacheFile(Context context, String url) {
        URL fileURL = null;
        try {
            url = url.replace("http://", "https://");
            fileURL = new URL(url);

            Log.d(Douyin.TAG, String.format("Start downloading file at %s.", url));

            HttpURLConnection connection = (HttpURLConnection) fileURL.openConnection();
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                Log.e(Douyin.TAG, String.format("Failed to download file from %s, response code: %d.", url,
                        connection.getResponseCode()));
                return null;
            }

            InputStream inputStream = connection.getInputStream();

            File cacheDir = getCacheFolder(context);
            File contentFolderPath = context.getExternalFilesDir(null);

            File cacheFile = new File(contentFolderPath, url.substring(url.lastIndexOf("/") + 1));
            FileOutputStream outputStream = new FileOutputStream(cacheFile);

            byte buffer[] = new byte[4096];
            int dataSize;
            while ((dataSize = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, dataSize);
            }
            outputStream.close();

            Log.d(Douyin.TAG, String.format("File was downloaded and saved at %s.", cacheFile.getAbsolutePath()));

            return cacheFile;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String getFileUri(Context context, File file) {
        if (file == null || !file.exists()) {
            return null;
        }

        Uri contentUri = FileProvider.getUriForFile(context, "com.app.dsb.fileprovider", // 要与`AndroidManifest.xml`里配置的`authorities`一致，假设你的应用包名为com.example.app
                file);

        // 授权给微信访问路径
        context.grantUriPermission("com.ss.android.ugc.aweme", // 这里填微信包名
                contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

        return contentUri.toString(); // contentUri.toString() 即是以"content://"开头的用于共享的路径
        // String filePath = context.getExternalFilesDir(null) + "/shareData/test.png";
        // // 该filePath对应于xml/file_provider_paths里的第一行配置：，因此才可被共享
        // File file = new File(filePath);
        // // 使用contentPath作为文件路径进行分享
        // // 要与`AndroidManifest.xml`里配置的`authorities`一致，假设你的应用包名为com.example.app
        // Uri contentUri = FileProvider.getUriForFile(context,
        // "com.example.app.fileprovider",file);
        // // 授权给抖音访问路径,这里填抖音包名
        // context.grantUriPermission("com.ss.android.ugc.aweme",
        // contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        // return contentUri.toString(); // contentUri.toString()
        // 即是以"content://"开头的用于共享的路径
    }

    public static String convertUriToPath(Context context, Uri uri) {
        if (uri == null)
            return null;
        String schema = uri.getScheme();
        if (TextUtils.isEmpty(schema) || ContentResolver.SCHEME_FILE.equals(schema)) {
            return uri.getPath();
        }
        if ("http".equals(schema))
            return uri.toString();
        if (ContentResolver.SCHEME_CONTENT.equals(schema)) {
            String[] projection = new String[] { MediaStore.MediaColumns.DATA };
            Cursor cursor = null;
            String filePath = "";
            try {
                cursor = context.getContentResolver().query(uri, projection, null, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        filePath = cursor.getString(0);
                    }

                    cursor.close();
                }
            } catch (Exception e) {
                // do nothing
            } finally {
                try {
                    if (null != cursor) {
                        cursor.close();
                    }
                } catch (Exception e2) {
                    // do nothing
                }
            }
            if (TextUtils.isEmpty(filePath)) {
                try {
                    ContentResolver contentResolver = context.getContentResolver();
                    String selection = MediaStore.Images.Media._ID + "= ?";
                    String id = uri.getLastPathSegment();
                    if (Build.VERSION.SDK_INT >= 19 && !TextUtils.isEmpty(id) && id.contains(":")) {
                        id = id.split(":")[1];
                    }
                    String[] selectionArgs = new String[] { id };
                    cursor = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, selection,
                            selectionArgs, null);
                    if (cursor.moveToFirst()) {
                        filePath = cursor.getString(0);
                    }
                    if (null != cursor) {
                        cursor.close();
                    }

                } catch (Exception e) {
                    // do nothing
                } finally {
                    try {
                        if (cursor != null) {
                            cursor.close();
                        }
                    } catch (Exception e) {
                        // do nothing
                    }
                }
            }
            return filePath;
        }
        return null;
    }

    public static String getImagePath(Context context, Uri uri, String selection) {
        String path = null;
        Cursor cursor = context.getContentResolver().query(uri, null, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }

            cursor.close();
        }
        return path;
    }
}

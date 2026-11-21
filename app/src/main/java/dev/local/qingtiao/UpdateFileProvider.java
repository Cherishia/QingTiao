package dev.local.qingtiao;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileNotFoundException;

/** Shares only the finalized update, with the installer that receives its read grant. */
public final class UpdateFileProvider extends ContentProvider {
    static final String APK_TYPE="application/vnd.android.package-archive";

    static File apk(Context context){return new File(context.getCacheDir(),"qingtiao-update.apk");}
    static Uri uri(Context context){return Uri.parse("content://"+context.getPackageName()+".update/QingTiao.apk");}

    @Override public boolean onCreate(){return true;}
    @Override public String getType(Uri target){return uri(getContext()).equals(target)?APK_TYPE:null;}

    @Override public ParcelFileDescriptor openFile(Uri target,String mode)throws FileNotFoundException {
        if(!"r".equals(mode)||!uri(getContext()).equals(target))throw new FileNotFoundException();
        return ParcelFileDescriptor.open(apk(getContext()),ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override public Cursor query(Uri target,String[] projection,String selection,String[] args,String order) {
        if(!uri(getContext()).equals(target))throw new IllegalArgumentException("Unknown update file");
        File file=apk(getContext());
        String[] columns=projection!=null?projection:new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE};
        MatrixCursor cursor=new MatrixCursor(columns,1);
        if(file.isFile()){
            Object[] row=new Object[columns.length];
            for(int i=0;i<columns.length;i++){
                if(OpenableColumns.DISPLAY_NAME.equals(columns[i]))row[i]="QingTiao.apk";
                else if(OpenableColumns.SIZE.equals(columns[i]))row[i]=file.length();
            }
            cursor.addRow(row);
        }
        return cursor;
    }

    @Override public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}
    @Override public int update(Uri uri,ContentValues values,String selection,String[] args){throw new UnsupportedOperationException();}
    @Override public int delete(Uri uri,String selection,String[] args){throw new UnsupportedOperationException();}
}

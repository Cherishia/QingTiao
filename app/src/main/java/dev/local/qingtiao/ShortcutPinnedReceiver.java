package dev.local.qingtiao;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/** The launcher sends this callback after it has actually pinned the shortcut. */
public final class ShortcutPinnedReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){
        Toast.makeText(context,"系统已确认添加桌面快捷方式",Toast.LENGTH_LONG).show();
    }
}

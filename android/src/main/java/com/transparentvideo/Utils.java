package com.transparentvideo;

import android.content.Context;
import android.os.Bundle;

public class Utils {
  public static int getRawResourceId(Context context, String resourceName) {
    return context.getResources().getIdentifier("" + resourceName, "raw", context.getPackageName());
  }
}

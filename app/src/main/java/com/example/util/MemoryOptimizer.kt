package com.example.util

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat

object MemoryOptimizer {

    private const val TAG = "NaomiMemoryOpt"

    /**
     * Checks if the current hardware is classified by Android as a Low-RAM device
     * (e.g. 512MB - 1GB RAM Sunmi V2 or Android Go edition).
     */
    fun isLowRam(context: Context): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val totalMb = getTotalRamMb(context)
        return am?.isLowRamDevice == true || totalMb <= 1024
    }

    fun getTotalRamMb(context: Context): Long {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return 1024
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        return memInfo.totalMem / (1024 * 1024)
    }

    fun getAvailableRamMb(context: Context): Long {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return 256
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        return memInfo.availMem / (1024 * 1024)
    }

    /**
     * Decode a resource bitmap directly downsampled to thermal printer width (max 384px)
     * using RGB_565 to consume 75% less RAM than ARGB_8888.
     */
    fun decodeSampledBitmap(context: Context, resId: Int, maxDimension: Int = 384): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeResource(context.resources, resId, options)
            options.inSampleSize = calculateInSampleSize(options, maxDimension, maxDimension)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.RGB_565
            options.inDither = true
            BitmapFactory.decodeResource(context.resources, resId, options)
        } catch (oom: OutOfMemoryError) {
            Log.e(TAG, "OOM prevented while decoding resource $resId. Invoking GC.")
            System.gc()
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding sampled resource: ${e.message}")
            null
        }
    }

    /**
     * Safely decodes a user-picked icon from URI, downsampling to max 384px width with RGB_565.
     * Prevents giant gallery images (e.g. 48MP photos) from exhausting heap on 1GB RAM POS terminals.
     */
    fun decodeSampledBitmapFromUri(context: Context, uri: Uri, maxDimension: Int = 384): Bitmap? {
        return try {
            // First pass: measure dimensions
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            options.inSampleSize = calculateInSampleSize(options, maxDimension, maxDimension)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.RGB_565

            // Second pass: decode downscaled
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
        } catch (oom: OutOfMemoryError) {
            Log.e(TAG, "OOM prevented decoding custom icon URI. Invoking GC.")
            System.gc()
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding custom icon URI: ${e.message}")
            null
        }
    }

    /**
     * Converts a vector drawable to a high-contrast white-background Bitmap
     * suitable for 58mm thermal raster dithering.
     */
    fun vectorToBitmap(context: Context, drawableId: Int, width: Int = 256, height: Int = 256): Bitmap? {
        return try {
            val drawable = ContextCompat.getDrawable(context, drawableId) ?: return null
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE) // White paper background
            drawable.setBounds(0, 0, width, height)
            drawable.draw(canvas)
            bitmap
        } catch (oom: OutOfMemoryError) {
            System.gc()
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }
}

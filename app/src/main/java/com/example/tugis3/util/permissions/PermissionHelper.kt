package com.example.tugis3.util.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Merkezi permission yönetim utility'si
 * Tüm uygulama genelinde tutarlı permission kontrolü sağlar
 */
object PermissionHelper {

    /**
     * GNSS/GPS için gerekli tüm izinler
     */
    fun getLocationPermissions(): List<String> {
        return buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        }
    }

    /**
     * Bluetooth için gerekli izinler (API seviyesine göre)
     */
    fun getBluetoothPermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            // Android 11 ve öncesi
            listOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION // Bluetooth scan için gerekli
            )
        }
    }

    /**
     * Dosya erişimi için gerekli izinler
     */
    fun getStoragePermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            // Android 12 ve öncesi
            listOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    /**
     * Kamera izni
     */
    fun getCameraPermission(): String = Manifest.permission.CAMERA

    /**
     * Tüm izinlerin verilip verilmediğini kontrol eder
     */
    fun hasPermissions(context: Context, permissions: List<String>): Boolean {
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Eksik izinleri döndürür
     */
    fun getMissingPermissions(context: Context, permissions: List<String>): List<String> {
        return permissions.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Location izinlerinin verilip verilmediğini kontrol eder
     */
    fun hasLocationPermissions(context: Context): Boolean {
        return hasPermissions(context, getLocationPermissions())
    }

    /**
     * Bluetooth izinlerinin verilip verilmediğini kontrol eder
     */
    fun hasBluetoothPermissions(context: Context): Boolean {
        return hasPermissions(context, getBluetoothPermissions())
    }

    /**
     * Storage izinlerinin verilip verilmediğini kontrol eder
     */
    fun hasStoragePermissions(context: Context): Boolean {
        return hasPermissions(context, getStoragePermissions())
    }

    /**
     * GNSS + Bluetooth + Storage tüm izinler
     */
    fun getAllRequiredPermissions(): List<String> {
        return buildList {
            addAll(getLocationPermissions())
            addAll(getBluetoothPermissions())
            addAll(getStoragePermissions())
            add(getCameraPermission())
        }.distinct()
    }
}


package stu.xiaohei.iphonebridge;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.io.File;

import android.app.AlertDialog;
import android.content.DialogInterface;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "iPhoneBridge";
    private static final String PREFS_NAME = "iPhoneBridgePrefs";
    private static final String PREF_LAST_DEVICE = "lastConnectedDevice";
    public static final String EXTRA_DEVICE_ADDRESS = "extra_device_address";
    
    // ANCS Service UUID
    private static final String SERVICE_ANCS = "7905F431-B5CE-4E99-A40F-4B1E122D00D0";
    private static final String CHAR_NOTIFICATION_SOURCE = "9FBF120D-6301-42D9-8C58-25E699A21DBD";
    private static final String CHAR_CONTROL_POINT = "69D1D8F3-45E1-49A8-9821-9BBDFDAAD9D9";
    private static final String CHAR_DATA_SOURCE = "22EAC6E9-24D6-4BB5-BE44-B36ACE7C7BFB";
    private static final String DESCRIPTOR_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private BluetoothGatt mConnectedGatt;
    private BluetoothDevice mTargetDevice;
    
    private BridgeService mBridgeService;
    private boolean mServiceBound = false;
    
    private TextView mStatusText;
    private TextView mDeviceInfoText;
    private Button mScanButton;
    private Button mConnectButton;
    private Button mAutoConnectButton;
    private ListView mNotificationList;
    private NotificationAdapter mNotificationAdapter;
    
    private Handler mHandler = new Handler();
    private boolean mScanning = false;
    private static final long SCAN_PERIOD = 10000;
    
    private List<NotificationItem> mNotifications = new ArrayList<>();
    private NotificationHandler mNotificationHandler;

    // In onCreate or initViews, after context is available
    // mNotificationHandler = new NotificationHandler(new File(getFilesDir(), "notifications.json"));
    
    // 通知项数据类
    public static class NotificationItem {
        public String uid;
        public String title;
        public String message;
        public String app;
        public String time;
        public int categoryId;
        public int eventId;
        
        public NotificationItem(String uid) {
            this.uid = uid;
            this.time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        }
    }
    
    // 自定义适配器
    private class NotificationAdapter extends ArrayAdapter<NotificationItem> {
        public NotificationAdapter(Context context) {
            super(context, 0);
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(
                    android.R.layout.simple_list_item_2, parent, false);
            }
            
            NotificationItem item = getItem(position);
            TextView text1 = convertView.findViewById(android.R.id.text1);
            TextView text2 = convertView.findViewById(android.R.id.text2);
            
            // 与通知详情页面保持一致的显示逻辑
            String displayTitle = "";
            String displaySubtitle = "";
            
            // 对于所有类型的通知，将消息内容的第一行作为标题
            String fullContent = "";
            if (item.title != null && !item.title.isEmpty()) {
                fullContent += item.title;
            }
            if (item.message != null && !item.message.isEmpty()) {
                if (!fullContent.isEmpty()) fullContent += "\n";
                fullContent += item.message;
            }
            
            if (!fullContent.isEmpty()) {
                String[] lines = fullContent.split("\n", 2);
                displayTitle = lines[0];
                displaySubtitle = item.time;
                if (lines.length > 1 && !lines[1].isEmpty()) {
                    displaySubtitle += " - " + lines[1];
                }
            } else {
                // 空通知已经在添加时被过滤，这里不应该出现
                displayTitle = "未知通知";
                displaySubtitle = item.time;
            }
            
            if (item.eventId == NotificationHandler.EVENT_ID_NOTIFICATION_REMOVED) {
                displayTitle += " (已移除)";
            }
            
            text1.setText(displayTitle);
            text2.setText(displaySubtitle);
            
            return convertView;
        }
    }
    
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BridgeService.LocalBinder binder = (BridgeService.LocalBinder) service;
            mBridgeService = binder.getService();
            mServiceBound = true;
            
            mBridgeService.setServiceCallback(new BridgeService.ServiceCallback() {
                @Override
                public void onConnectionStateChanged(boolean connected) {
                    runOnUiThread(() -> {
                        updateStatus(connected ? "已连接" : "已断开");
                        mConnectButton.setText(connected ? "断开连接" : "连接设备");
                        mAutoConnectButton.setEnabled(true); // Re-enable auto-connect button
                    });
                }
                
                @Override
                public void onNotificationReceived(NotificationHandler.NotificationInfo info) {
                    runOnUiThread(() -> addNotificationToList(info));
                }
                
                @Override
                public void onServiceReady() {
                    runOnUiThread(() -> updateStatus("服务就绪，正在接收通知"));
                }
            });
        }
        
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mServiceBound = false;
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_improved);
        
        initBluetooth();
        initViews();
        checkPermissions();
        registerBondReceiver();
        requestBatteryOptimizationWhitelist();
        
        mNotificationHandler = new NotificationHandler(new File(getFilesDir(), "notifications.json"));

        // 检查是否从通知启动并带有设备地址
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(BridgeService.EXTRA_DEVICE_ADDRESS)) {
            String deviceAddress = intent.getStringExtra(BridgeService.EXTRA_DEVICE_ADDRESS);
            if (deviceAddress != null && mBluetoothAdapter != null) {
                try {
                    mTargetDevice = mBluetoothAdapter.getRemoteDevice(deviceAddress);
                    updateDeviceInfo(mTargetDevice);
                    mConnectButton.setEnabled(true);
                    // 延迟连接，确保服务已绑定
                    mHandler.postDelayed(() -> {
                        if (mServiceBound && mBridgeService != null) {
                            connectDevice();
                        } else {
                            Toast.makeText(this, "服务未就绪，无法连接设备", Toast.LENGTH_SHORT).show();
                        }
                    }, 1000); // 延迟1秒，确保服务绑定
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Invalid Bluetooth address from notification: " + deviceAddress, e);
                }
            }
        }

        // 启动服务
        Intent serviceIntent = new Intent(this, BridgeService.class);
        startService(serviceIntent);
        bindService(serviceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }
    
    private void initViews() {
        mStatusText = findViewById(R.id.statusText);
        mDeviceInfoText = findViewById(R.id.deviceInfoText);
        mScanButton = findViewById(R.id.scanButton);
        mConnectButton = findViewById(R.id.connectButton);
        mAutoConnectButton = findViewById(R.id.autoConnectButton);
        mNotificationList = findViewById(R.id.notificationList);
        
        mNotificationAdapter = new NotificationAdapter(this);
        mNotificationList.setAdapter(mNotificationAdapter);
        
        mScanButton.setOnClickListener(v -> startScan());
        mConnectButton.setOnClickListener(v -> toggleConnection());
        mAutoConnectButton.setOnClickListener(v -> {
            if (mServiceBound && mBridgeService != null) {
                mBridgeService.startAutoReconnect();
                updateStatus("正在尝试自动连接...");
                mAutoConnectButton.setEnabled(false); // Disable button immediately
            }
        });
        
        mNotificationList.setOnItemClickListener((parent, view, position, id) -> {
            NotificationItem item = mNotificationAdapter.getItem(position);
            if (item != null) {
                Intent intent = NotificationDetailActivity.createIntent(this, item.uid);
                startActivity(intent);
            }
        });
        
        mConnectButton.setEnabled(false);
        
        // Load last connected device
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String lastDeviceAddress = prefs.getString(PREF_LAST_DEVICE, null);
        if (lastDeviceAddress != null) {
            BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager != null) {
                BluetoothAdapter adapter = bluetoothManager.getAdapter();
                if (adapter != null) {
                    try {
                        mTargetDevice = adapter.getRemoteDevice(lastDeviceAddress);
                        updateDeviceInfo(mTargetDevice);
                        mConnectButton.setEnabled(true);
                        updateStatus("已加载上次连接设备: " + (mTargetDevice.getName() != null ? mTargetDevice.getName() : mTargetDevice.getAddress()));
                    } catch (IllegalArgumentException e) {
                        Log.e(TAG, "Invalid Bluetooth address stored: " + lastDeviceAddress, e);
                        updateStatus("上次连接设备地址无效");
                        updateDeviceInfo(null); // Reset if invalid
                    }
                } else {
                    updateDeviceInfo(null); // Reset if adapter is null
                }
            } else {
                updateDeviceInfo(null); // Reset if bluetoothManager is null
            }
        } else {
            updateDeviceInfo(null); // No last device found
        }
    }
    
    private void updateDeviceInfo(BluetoothDevice device) {
        if (device == null) {
            mDeviceInfoText.setText("未选择设备");
        } else {
            String info = String.format("设备: %s\n地址: %s", 
                device.getName() != null ? device.getName() : "未知设备",
                device.getAddress());
            mDeviceInfoText.setText(info);
        }
    }
    
    private void toggleConnection() {
        if (mServiceBound && mBridgeService != null) {
            if (mBridgeService.isConnected()) {
                mBridgeService.disconnect();
            } else if (mTargetDevice != null) {
                connectDevice();
            }
        }
    }
    
    private void autoConnectLastDevice() {
        if (mServiceBound && mBridgeService != null) {
            mBridgeService.startAutoReconnect();
            updateStatus("正在尝试自动连接...");
        } else {
            Toast.makeText(this, "服务未就绪", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void checkPermissions() {
        List<String> permissions = new ArrayList<>();
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) 
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) 
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
        }
        
        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, 
                permissions.toArray(new String[0]), 1);
        }
    }
    
    private void initBluetooth() {
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            updateStatus("蓝牙不可用");
            return;
        }
        
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            updateStatus("设备不支持蓝牙");
            return;
        }
        
        // 检查Android 12+的蓝牙权限
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) 
                    != PackageManager.PERMISSION_GRANTED) {
                updateStatus("需要蓝牙连接权限");
                return;
            }
        }
        
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        } else {
            mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
            updateStatus("蓝牙已就绪");
        }
    }
    
    private void registerBondReceiver() {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBondReceiver, filter);
    }
    
    private void startScan() {
        if (mBluetoothLeScanner == null) {
            updateStatus("蓝牙扫描器未初始化");
            return;
        }
        
        // 检查扫描权限
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) 
                    != PackageManager.PERMISSION_GRANTED) {
                updateStatus("需要蓝牙扫描权限");
                return;
            }
        }
        
        if (!mScanning) {
            // 清除之前的扫描结果
            mNotifications.clear();
            mNotificationAdapter.notifyDataSetChanged();
            
            mHandler.postDelayed(() -> {
                mScanning = false;
                if (mBluetoothLeScanner != null) {
                    mBluetoothLeScanner.stopScan(mScanCallback);
                }
                updateStatus("扫描完成");
                mScanButton.setText("开始扫描");
            }, SCAN_PERIOD);
            
            mScanning = true;
            
            // 使用更智能的扫描设置
            ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
            
            mBluetoothLeScanner.startScan(null, settings, mScanCallback);
            updateStatus("正在扫描设备...");
            mScanButton.setText("停止扫描");
        } else {
            mScanning = false;
            mBluetoothLeScanner.stopScan(mScanCallback);
            updateStatus("扫描已停止");
            mScanButton.setText("开始扫描");
        }
    }
    
    private void connectDevice() {
        if (mTargetDevice != null) {
            if (mBluetoothAdapter.getBondedDevices().contains(mTargetDevice)) {
                // 已配对，直接连接
                if (mServiceBound && mBridgeService != null) {
                    mBridgeService.connectToDevice(mTargetDevice);
                    updateStatus("正在连接...");
                    
                    // 保存设备地址
                    SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                    editor.putString(PREF_LAST_DEVICE, mTargetDevice.getAddress());
                    editor.apply();
                }
            } else {
                // 未配对，先配对
                mTargetDevice.createBond();
                updateStatus("正在配对...");
            }
        }
    }
    
    private void addNotificationToList(NotificationHandler.NotificationInfo info) {
        NotificationItem item = new NotificationItem(info.uid);
        item.title = info.title;
        item.message = info.message;
        item.app = info.appId;
        item.categoryId = info.categoryId;
        item.eventId = info.eventId;
        
        // 根据事件类型处理
        if (info.eventId == NotificationHandler.EVENT_ID_NOTIFICATION_REMOVED) {
            // 删除通知
            for (int i = 0; i < mNotificationAdapter.getCount(); i++) {
                if (mNotificationAdapter.getItem(i).uid.equals(info.uid)) {
                    mNotificationAdapter.remove(mNotificationAdapter.getItem(i));
                    break;
                }
            }
        } else {
            // 检查通知内容是否为空，如果为空则不添加到列表
            String fullContent = "";
            if (info.title != null && !info.title.isEmpty()) {
                fullContent += info.title;
            }
            if (info.message != null && !info.message.isEmpty()) {
                if (!fullContent.isEmpty()) fullContent += "\n";
                fullContent += info.message;
            }
            
            if (fullContent.isEmpty()) {
                // 如果没有有效内容，不添加到通知列表
                return;
            }
            
            // 添加或更新通知
            boolean found = false;
            for (int i = 0; i < mNotificationAdapter.getCount(); i++) {
                if (mNotificationAdapter.getItem(i).uid.equals(info.uid)) {
                    mNotificationAdapter.getItem(i).title = info.title;
                    mNotificationAdapter.getItem(i).message = info.message;
                    found = true;
                    break;
                }
            }
            
            if (!found) {
                mNotificationAdapter.insert(item, 0);
            }
        }
        
        mNotificationAdapter.notifyDataSetChanged();
    }
    
    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            String deviceName = device.getName();
            
            if (deviceName != null && (deviceName.contains("Heart Rate") || 
                    deviceName.contains("iPhone") || deviceName.contains("iPad") ||
                    deviceName.contains("Apple") || deviceName.contains("AirPods"))) {
                
                mTargetDevice = device;
                updateStatus("发现设备: " + deviceName);
                updateDeviceInfo(device);
                mConnectButton.setEnabled(true);
                
                // 停止扫描
                mScanning = false;
                mBluetoothLeScanner.stopScan(mScanCallback);
                mScanButton.setText("开始扫描");
            }
        }
    };
    
    private BroadcastReceiver mBondReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction())) {
                int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                if (bondState == BluetoothDevice.BOND_BONDED) {
                    updateStatus("配对成功");
                    Toast.makeText(MainActivity.this, "配对成功！", Toast.LENGTH_SHORT).show();
                    if (mTargetDevice != null) {
                        connectDevice();
                    }
                } else if (bondState == BluetoothDevice.BOND_NONE) {
                    updateStatus("配对失败");
                }
            }
        }
    };
    
    private void updateStatus(String status) {
        mStatusText.setText(status);
    }
    
    private void requestBatteryOptimizationWhitelist() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(getPackageName())) {
                new AlertDialog.Builder(this)
                    .setTitle("电池优化提示")
                    .setMessage("为了确保应用在后台稳定运行，持续接收 iPhone 通知，请将本应用添加到电池优化白名单。这有助于防止系统在后台杀死应用。")
                    .setPositiveButton("前往设置", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                                intent.setData(Uri.parse("package:" + getPackageName()));
                                startActivity(intent);
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to request battery optimization whitelist", e);
                                // Fallback to general battery optimization settings if specific request fails
                                try {
                                    Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                                    startActivity(intent);
                                    Toast.makeText(MainActivity.this, "请手动将此应用添加到电池优化白名单以确保后台运行", Toast.LENGTH_LONG).show();
                                } catch (Exception ex) {
                                    Log.e(TAG, "Failed to open battery optimization settings", ex);
                                    Toast.makeText(MainActivity.this, "无法打开电池优化设置，请手动前往设置", Toast.LENGTH_LONG).show();
                                }
                            }
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
            }
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 当Activity回到前台时，尝试自动连接
        if (mServiceBound && mBridgeService != null) {
            mBridgeService.startAutoReconnect();
            updateStatus("应用回到前台，正在尝试自动连接...");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 当Activity进入后台时，不需要断开连接，服务会继续运行
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        if (mServiceBound) {
            unbindService(mServiceConnection);
            mServiceBound = false;
        }
        
        unregisterReceiver(mBondReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_clear_notifications) {
            if (mServiceBound && mBridgeService != null) {
                mBridgeService.clearAllNotifications();
                mNotificationAdapter.clear();
                mNotificationAdapter.notifyDataSetChanged();
                Toast.makeText(this, "所有通知已清空", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
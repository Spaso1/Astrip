package org.astral.astrip.ui.home;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.*;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.*;
import com.baidu.mapapi.model.LatLng;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.astral.astrip.R;
import org.astral.astrip.been.AmapReverseGeocodeResponse;
import org.astral.astrip.been.Pointer;
import org.astral.astrip.been.Project;
import org.astral.astrip.databinding.FragmentHomeBinding;

import java.io.IOException;
import java.util.*;

public class HomeFragment extends Fragment {
    private MapView mapView;
    private BaiduMap baiduMap;
    private FragmentHomeBinding binding;
    private LocationManager locationManager;
    private String city;
    private double x;
    private double y;
    private boolean flag = true;
    private String detail;
    private String province;
    private Bundle savedInstanceState;
    private List<LatLng> markerPoints = new ArrayList<>();
    private List<Marker> markers = new ArrayList<>();
    private Polyline polyline;
    private SharedPreferences save;
    private boolean isDes = false;
    private LinearLayout hor;
    private Map<String,Pointer> designs = new HashMap<>();
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);
        SDKInitializer.setAgreePrivacy(requireContext().getApplicationContext(),true);
        SDKInitializer.initialize(requireContext().getApplicationContext());
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},0x123);
        FloatingActionButton fab = binding.fab;
        menu(fab);
        this.savedInstanceState = savedInstanceState;
        save = requireContext().getSharedPreferences("save", Context.MODE_PRIVATE);
        return root;
    }
    @SuppressLint("MissingInflatedId")
    private void menu(FloatingActionButton fab) {
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 创建并显示 MaterialDialog
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
                View dialogView = getLayoutInflater().inflate(R.layout.menu_dialog, null);
                builder.setView(dialogView);

                // 找到选项按钮并设置点击事件
                MaterialButton option1 = dialogView.findViewById(R.id.option1);
                option1.setOnClickListener(v -> {
                    // 处理选项1的点击事件
                    openProject() ;
                });

                MaterialButton option2 = dialogView.findViewById(R.id.option2);
                option2.setOnClickListener(v -> {
                    // 处理选项2的点击事件
                    design();
                    option1.setVisibility(View.GONE);
                    option2.setText("正在规划");
                    hor = dialogView.findViewById(R.id.hor);
                    Toast.makeText(getContext(), "正在规划", Toast.LENGTH_SHORT).show();
                });
                builder.setPositiveButton("关闭", (dialog, which) -> {
                    dialog.dismiss();
                });
                if(isDes) {
                    option2.setText("正在规划");
                    option1.setVisibility(View.GONE);
                    hor = dialogView.findViewById(R.id.hor);
                    Toast.makeText(getContext(), "正在规划", Toast.LENGTH_SHORT).show();
                }
                builder.show();
            }
        });
    }
    private void openProject() {
        Map<String,Pointer> paths = new TreeMap<>();
        Pointer start = new Pointer();
        start.setPay(0);
        start.setType("start");
        start.setX(x);
        start.setY(y);
        start.setPointName("start");
        paths.put("0start",start);

        Pointer p1 = new Pointer();
        p1.setX(x + 0.01 * 2);
        p1.setY(y + 0.01 * 6);
        p1.setType("point");
        p1.setPointName("point1");
        paths.put("1point" + 1,p1);

        Pointer over = new Pointer();
        over.setX(x + 0.03);
        over.setY(y);
        over.setType("over");
        over.setPointName("over");
        paths.put("2over",over);

        Project project = new Project();
        project.setId(1);
        project.setName("测试项目");
        project.setDate_create("2023-07-01");
        project.setDate_start("2023-07-01");
        project.setDate_end("2023-07-01");
        project.setPaths(paths);
        addMarkersAndPolyline(project);
    }
    private void design() {
        isDes = true;
    }
    private void start() {
        mapView = binding.bmapView;
        mapView.onCreate(getContext(), savedInstanceState);

        baiduMap = mapView.getMap();

        // 设置地图中心点
        LatLng latLng = new LatLng(y, x); // 北京市经纬度
        baiduMap.setMapStatus(MapStatusUpdateFactory.newLatLngZoom(latLng, 13)); // 缩放级别调整为

        // 添加独特样式的标记
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.logo); // 自定义图标资源
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 200, 130, true); // 缩放到 100x100 像素
        BitmapDescriptor descriptor = BitmapDescriptorFactory.fromBitmap(scaledBitmap);
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .title("舞萌痴位置")
                .icon(descriptor); // 使用自定义图标
        baiduMap.addOverlay(markerOptions);

        // 设置地图点击监听器
        baiduMap.setOnMapClickListener(new BaiduMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng point) {
                // 处理地图点击事件
                Toast.makeText(requireContext(), "点击了地图: " + point.toString(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onMapPoiClick(MapPoi poi) {
                // 处理兴趣点点击事件

                if (isDes) {
                    //MaterialAlertDialogBuilder
                    handlePoiClick(poi);
                }else {
                    Toast.makeText(requireContext(), "点击了兴趣点: " + poi.getName(), Toast.LENGTH_SHORT).show();
                    //输出poi
                    Log.d("poi", poi.getName());
                    Log.d("poi", poi.getUid());
                    Log.d("poi", poi.getPosition().toString());
                }
            }
        });
        baiduMap.setOnMarkerClickListener(new BaiduMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                showMarkerInfoDialog(marker);
                return true; // 返回 true 表示已处理点击事件
            }
        });
    }
    private void handlePoiClick(MapPoi poi) {
        // 创建 Pointer 对象
        Pointer pointer = new Pointer();
        pointer.setPointName(poi.getName());
        pointer.setPointId(0); // 可以根据需要设置 ID
        pointer.setPointUUID(poi.getUid());
        pointer.setTime(new Date().toString()); // 设置当前时间
        pointer.setType("poi");
        pointer.setX(poi.getPosition().longitude);
        pointer.setY(poi.getPosition().latitude);
        pointer.setPay(0); // 可以根据需要设置支付信息

        // 显示弹窗
        showPointerInfoDialog(pointer);
    }
    @SuppressLint("MissingInflatedId")
    private void showPointerInfoDialog(Pointer pointer) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.design_dialog, null);
        builder.setView(dialogView);

        // 找到 TextInputEditText
         TextInputEditText pointNameEditText = dialogView.findViewById(R.id.pointName);
        TextInputEditText pointIdEditText = dialogView.findViewById(R.id.pointId);
        TextInputEditText pointUUIDEditText = dialogView.findViewById(R.id.pointUUID);
        TextInputEditText timeEditText = dialogView.findViewById(R.id.time);
        TextInputEditText typeEditText = dialogView.findViewById(R.id.type);
        TextInputEditText xEditText = dialogView.findViewById(R.id.x);
        TextInputEditText yEditText = dialogView.findViewById(R.id.y);
        TextInputEditText payEditText = dialogView.findViewById(R.id.pay);

        // 填充信息
        pointNameEditText.setText(pointer.getPointName());
        pointIdEditText.setText(String.valueOf(pointer.getPointId()));
        pointUUIDEditText.setText(pointer.getPointUUID());
        timeEditText.setText(pointer.getTime());
        typeEditText.setText(pointer.getType());
        xEditText.setText(String.valueOf(pointer.getX()));
        yEditText.setText(String.valueOf(pointer.getY()));
        payEditText.setText(String.valueOf(pointer.getPay()));

        // 设置确定按钮
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 获取输入的信息
                String pointName = pointNameEditText.getText().toString();
                int pointId = Integer.parseInt(pointIdEditText.getText().toString());
                String pointUUID = pointUUIDEditText.getText().toString();
                String time = timeEditText.getText().toString();
                String type = typeEditText.getText().toString();
                double x = Double.parseDouble(xEditText.getText().toString());
                double y = Double.parseDouble(yEditText.getText().toString());
                int pay = Integer.parseInt(payEditText.getText().toString());

                // 创建 Pointer 对象
                Pointer newPointer = new Pointer();
                newPointer.setPointName(pointName);
                newPointer.setPointId(pointId);
                newPointer.setPointUUID(pointUUID);
                newPointer.setTime(time);
                newPointer.setType(type);
                newPointer.setX(x);
                newPointer.setY(y);
                newPointer.setPay(pay);

                if (type.equals("终点")) {
                    isDes = false;
                }
                designs.put(designs.size() + "", newPointer);
                Project project = new Project();
                project.setPaths(designs);
                addMarkersAndPolyline(project);
                //创造TextView加入hor
                TextView textView = new TextView(requireContext());
                textView.setText(newPointer.getPointName());
                textView.setTextSize(20);
                textView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //点击实现询问删除
                        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
                        builder.setTitle("删除");
                        builder.setMessage("确定删除吗？");
                        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                hor.removeView(textView);

                            }
                        });
                    }
                });
                textView.isTextSelectable();
                textView.isEnabled();
                hor.addView(textView);
                hor.setVisibility(View.VISIBLE);
                if (type.equals("终点")) {
                    Toast.makeText(getContext(), "规划完成~进入下一步", Toast.LENGTH_SHORT).show();
                    isDes = false;
                }
            }
        });

        // 设置取消按钮
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.show();
    }


    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 0x123 && grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // 创建 LocationManager 对象
            locationManager = (LocationManager) requireActivity().getSystemService(Context.LOCATION_SERVICE);
            // 获取最新的定位信息
            Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKnownLocation != null) {
                // 调用高德地图 API 进行逆地理编码
                reverseGeocode(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
            } else {
                Toast.makeText(requireActivity().getApplicationContext(), "无法获取最新定位信息", Toast.LENGTH_SHORT).show();
                Log.d("Location", "无法获取最新定位信息");
            }
        }

        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 12000, 16f, new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    Log.d("Location", "onLocationChanged");
                    if (flag) {
                        // 调用高德地图 API 进行逆地理编码
                        reverseGeocode(location.getLatitude(), location.getLongitude());
                    }
                }

                @Override
                public void onProviderDisabled(@NonNull String provider) {
                    Toast.makeText(requireActivity().getApplicationContext(), "关闭定位", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.d("Location", "GPS定位失败");
        }
    }

    // 调用高德地图 API 进行逆地理编码
    private void reverseGeocode(double latitude, double longitude) {
        new Thread(() -> {
            try {
                // 构建请求 URL
                this.x = longitude;
                this.y = latitude;
                String url = "https://restapi.amap.com/v3/geocode/regeo?key=234cad2e2f0706e54c92591647a363c3&location=" + longitude + "," + latitude;
                Log.d("Location", url);
                // 发起网络请求
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    // 使用 Gson 解析 JSON
                    Gson gson = new Gson();
                    Log.d("Location", responseData);
                    AmapReverseGeocodeResponse geocodeResponse = gson.fromJson(responseData, AmapReverseGeocodeResponse.class);
                    if (geocodeResponse.getStatus().equals("1")) { // 状态码 "1" 表示成功
                        AmapReverseGeocodeResponse.Regeocode regeocode = geocodeResponse.getRegeocode();
                        AmapReverseGeocodeResponse.AddressComponent addressComponent = regeocode.getAddressComponent();
                        // 解析地址信息
                        String address = regeocode.getFormattedAddress();
                        String province = addressComponent.getProvince();
                        String city;
                        try {
                            city = addressComponent.getCity().get(0).replace("市", "");
                        } catch (Exception e) {
                            city = addressComponent.getProvince().replace("市", "");
                        }
                        // 更新 UI
                        String finalCity = city;
                        getActivity().runOnUiThread(() -> {
                            this.detail = address;
                            this.province = province;
                            this.city = finalCity;
                            start();
                        });
                    } else {
                        Log.d("Location", "高德地图 API 调用失败，尝试使用 Android 自带 Geocoder");
                        fallbackToGeocoder(latitude, longitude); // 调用备用方案
                    }
                } else {
                    Log.d("Location", "高德地图 API 调用失败，尝试使用 Android 自带 Geocoder");
                    fallbackToGeocoder(latitude, longitude); // 调用备用方案
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.d("Location", "高德地图 API 调用失败，尝试使用 Android 自带 Geocoder");
                fallbackToGeocoder(latitude, longitude); // 调用备用方案
            }
        }).start();
    }

    // 备用方案：使用 Android 自带的 Geocoder 进行逆地理编码
    private void fallbackToGeocoder(double latitude, double longitude) {
        try {
            Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String detail = address.getAddressLine(0);
                String province = address.getAdminArea();
                String city = address.getLocality();
                // 更新 UI
                try {
                    requireActivity().runOnUiThread(() -> {
                        this.detail= detail;
                        this.province = province;
                        this.city = city;
                    });
                }catch (Exception e) {

                }

            } else {
                Log.d("Location", "Android 自带 Geocoder 获取地址失败");
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("Location", "Android 自带 Geocoder 获取地址失败");
        }
    }
    private void addMarkersAndPolyline(Project project) {
        Map<String, Pointer> paths = project.getPaths();
        List<LatLng> points = new ArrayList<>();
        for (Pointer pointer : paths.values()) {
            Log.d(pointer.getPointName(), pointer.getX() + " " + pointer.getY());
            if(pointer.getX() == 0.0 && pointer.getY() == 0.0) {
                continue;
            }
            points.add(new LatLng(pointer.getY(), pointer.getX()));
        }

        // 清除之前的 Marker 和 Polyline
        for (Marker marker : markers) {
            marker.remove();
        }
        if (polyline != null) {
            polyline.remove();
        }

        // 添加新的 Marker
        for (Pointer pointer : paths.values()) {
            LatLng point = new LatLng(pointer.getY(), pointer.getX());
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.logo);
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 100, 65, true); // 缩放到 100x65 像素
            BitmapDescriptor descriptor = BitmapDescriptorFactory.fromBitmap(scaledBitmap);
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(point)
                    .icon(descriptor);

            // 设置额外信息
            Bundle bundle = new Bundle();
            bundle.putString("name", pointer.getType());
            markerOptions.extraInfo(bundle);
            Marker marker = (Marker) baiduMap.addOverlay(markerOptions);
            markers.add(marker);
        }

        // 添加 Polyline，只连接相邻的 Pointer
        if (points.size() > 1) {
            List<LatLng> polylinePoints = new ArrayList<>();
            for (int i = 0; i < points.size() - 1; i++) {
                polylinePoints.add(points.get(i));
                polylinePoints.add(points.get(i + 1));
            }

            OverlayOptions polylineOptions = new PolylineOptions()
                    .points(polylinePoints)
                    .width(10)
                    .color(0xAAFF0000);
            polyline = (Polyline) baiduMap.addOverlay(polylineOptions);
        }

    }
    private void showMarkerInfoDialog(Marker marker) {
        Bundle extraInfo = marker.getExtraInfo();
        try {
            Log.d("Location", extraInfo.getString("name"));
        } catch (Exception e) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getContext(), "乌蒙痴位置", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
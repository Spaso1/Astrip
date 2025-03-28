package org.astral.astrip.ui.home;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.*;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.*;
import com.baidu.mapapi.model.LatLng;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.astral.astrip.R;
import org.astral.astrip.been.AmapReverseGeocodeResponse;
import org.astral.astrip.databinding.FragmentHomeBinding;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

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
        return root;
    }
    private void menu(FloatingActionButton fab) {
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 创建并显示 MaterialDialog
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
                builder.setView(R.layout.menu_dialog);

                builder.setPositiveButton("关闭", (dialog, which) -> {
                    dialog.dismiss();
                });

                builder.show();
            }
        });
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

        // 设置标记点击监听器
        baiduMap.setOnMarkerClickListener(new BaiduMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                Toast.makeText(requireContext(), "点击了标记: " + marker.getTitle(), Toast.LENGTH_SHORT).show();
                return true; // 返回 true 表示已处理点击事件
            }
        });

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
                Toast.makeText(requireContext(), "点击了兴趣点: " + poi.getName(), Toast.LENGTH_SHORT).show();
                //输出poi
                Log.d("poi", poi.getName());
                Log.d("poi", poi.getUid());
                Log.d("poi", poi.getPosition().toString());
            }
        });
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
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
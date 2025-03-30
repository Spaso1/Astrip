package org.astral.astrip.ui.home;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.location.*;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.*;
import com.baidu.mapapi.model.LatLng;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.skydoves.colorpickerview.ColorEnvelope;
import com.skydoves.colorpickerview.ColorPickerView;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.astral.astrip.MainActivity;
import org.astral.astrip.R;
import org.astral.astrip.adapter.PointsAdapter;
import org.astral.astrip.been.AmapReverseGeocodeResponse;
import org.astral.astrip.been.LikePlace;
import org.astral.astrip.been.Pointer;
import org.astral.astrip.been.Project;
import org.astral.astrip.databinding.FragmentHomeBinding;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
    private ListView pointsListView;
    private PointsAdapter pointsAdapter;
    private int duilieSum;
    private int run = 0;
    private List<Project> projects = new ArrayList<>();
    private List<Marker> likeMarkers = new ArrayList<>();
    private List<Marker> useLikeMarkers = new ArrayList<>();
    private List<LikePlace> likePlaces = new ArrayList<>();
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);
        SDKInitializer.setAgreePrivacy(requireContext().getApplicationContext(),true);
        SDKInitializer.initialize(requireContext().getApplicationContext());
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        setDefault();

        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},0x123);
        FloatingActionButton fab = binding.fab;
        menu(fab);
        duilieSum = 1;
        this.savedInstanceState = savedInstanceState;
        save = requireContext().getSharedPreferences("save", Context.MODE_PRIVATE);
        return root;
    }

    private void readLikePlace() {
        try {
            for(Marker marker : likeMarkers) {
                marker.remove();
            }
            String json = save.getString("likePlaces", "");
            if (!json.isEmpty()) {
                likePlaces = new Gson().fromJson(json, new TypeToken<List<LikePlace>>() {}.getType());

                for (LikePlace likePlace : likePlaces) {
                    Log.d("read", "readLikePlace: " + likePlace.getName());
                    LatLng latLng = new LatLng(likePlace.getY(), likePlace.getX());
                    addLikeMarker(latLng, likePlace.getName());
                }
            }
        }catch (Exception e) {

        }
    }
    private void saveLikePlace() {
        try {
            String json = new Gson().toJson(likePlaces);
            Log.d("save", "saveLikePlace: " + json);

            save.edit().putString("likePlaces", json).apply();
            save.edit().apply();
        }catch (Exception e) {
e.printStackTrace();
        }
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
                pointsListView = dialogView.findViewById(R.id.pointsListView);
                pointsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        Pointer pointer = (Pointer) adapterView.getItemAtPosition(i);
                        doCheck(pointer);
                    }
                });
                pointsAdapter = new PointsAdapter(requireContext(), new ArrayList<>(designs.values()));
                pointsListView.setAdapter(pointsAdapter);


                // 找到选项按钮并设置点击事件
                MaterialButton option1 = dialogView.findViewById(R.id.option1);
                MaterialButton option2 = dialogView.findViewById(R.id.option2);
                MaterialButton option3 = dialogView.findViewById(R.id.option3);
                option1.setOnClickListener(v -> {
                    // 处理选项1的点击事件
                    openProject() ;
                });

                option2.setOnClickListener(v -> {
                    // 处理选项2的点击事件
                    design();
                    designs = new HashMap<>();
                    duilieSum = 1;
                    option1.setVisibility(View.GONE);
                    option2.setText("正在规划");
                    Toolbar toolbar = ((MainActivity) requireActivity()).findViewById(R.id.toolbar);
                    toolbar.setTitle("规划中");
                    Toast.makeText(getContext(), "正在规划", Toast.LENGTH_SHORT).show();
                    if(polyline != null) {
                        polyline.remove();
                    }
                    //清空Marker
                    for (Marker marker : markers) {
                        marker.remove();
                    }
                    option3.setVisibility(View.VISIBLE);

                });

                option3.setOnClickListener(v -> {
                    // 处理选项2的点击事件

                    option1.setVisibility(View.VISIBLE);
                    option2.setText("规划");

                    isDes = false;
                    option3.setVisibility(View.GONE);


                    exitProject();
                });
                MaterialButton option4 = dialogView.findViewById(R.id.option4);
                option4.setOnClickListener(v -> {
                    // 处理选项2的点击事件
                    isDes = false;
                    exitProject();
                });

                MaterialButton option5 = dialogView.findViewById(R.id.option5);
                option5.setOnClickListener(v -> {
                    design();
                    //打开一个项目
                    openProjectSingle();
                });

                builder.setPositiveButton("关闭", (dialog, which) -> {
                    dialog.dismiss();
                });
                if(isDes) {
                    option2.setText("正在规划");
                    option1.setVisibility(View.GONE);
                    option2.setEnabled(false);
                    option4.setVisibility(View.GONE);
                    option5.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "正在规划", Toast.LENGTH_SHORT).show();
                }
                if(isDes) {
                    option3.setVisibility(View.VISIBLE);
                }else {
                    option3.setVisibility(View.GONE);
                    option5.setVisibility(View.VISIBLE);
                }

                builder.show();
            }
        });
    }
    private void exitProject() {
        pointsAdapter.notifyDataSetChanged();
        //清空地图上线
        if(polyline != null) {
            polyline.remove();
        }
        //清空Marker
        for (Marker marker : markers) {
            marker.remove();
        }
        if(isDes) {
            isDes = false;
        }
        designs = new HashMap<>();
        duilieSum = 1;

        Toolbar toolbar = ((MainActivity) requireActivity()).findViewById(R.id.toolbar);
        toolbar.setTitle("主页");
        Toast.makeText(getContext(), "已退出", Toast.LENGTH_SHORT).show();
    }
    private void openProject() {
        // 从 Shared 中读取
        try {
            String json = save.getString("projects", "");
            projects = new Gson().fromJson(json, new TypeToken<List<Project>>() {}.getType());
            if (projects != null && !projects.isEmpty()) {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
                // 使用多选框选择打开项目
                builder.setTitle("选择项目");

                // 准备项目名称数组
                final String[] projectNames = projects.stream()
                        .map(Project::getName)
                        .toArray(String[]::new);

                // 选择的项目索引
                final boolean[] selectedProjects = new boolean[projectNames.length];

                builder.setMultiChoiceItems(projectNames, selectedProjects, (dialog, which, isChecked) -> {
                    // 处理选择事件
                    selectedProjects[which] = isChecked;
                });

                builder.setPositiveButton("打开", (dialog, which) -> {
                    // 处理打开按钮点击事件
                    List<Project> selectedProjectList = new ArrayList<>();
                    for (int i = 0; i < selectedProjects.length; i++) {
                        if (selectedProjects[i]) {
                            selectedProjectList.add(projects.get(i));
                        }
                    }
                    if (!selectedProjectList.isEmpty()) {
                        // 打开选中的项目
                        openSelectedProjects(selectedProjectList);

                        dialog.dismiss();
                    } else {
                        Toast.makeText(requireContext(), "请选择一个项目", Toast.LENGTH_SHORT).show();
                    }
                });

                builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());

                builder.show();
            } else {
                Toast.makeText(requireContext(), "没有项目可打开", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void openProjectSingle() {
        // 从 Shared 中读取
        try {
            String json = save.getString("projects", "");
            projects = new Gson().fromJson(json, new TypeToken<List<Project>>() {}.getType());
            if (projects != null && !projects.isEmpty()) {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
                // 使用单选框选择打开项目
                builder.setTitle("选择项目");

                // 准备项目名称数组
                final String[] projectNames = projects.stream()
                        .map(Project::getName)
                        .toArray(String[]::new);

                // 选择的项目索引
                final int[] selectedProjectIndex = {-1};

                builder.setSingleChoiceItems(projectNames, -1, (dialog, which) -> {
                    // 处理选择事件
                    selectedProjectIndex[0] = which;
                });

                builder.setPositiveButton("打开", (dialog, which) -> {
                    // 处理打开按钮点击事件
                    if (selectedProjectIndex[0] >= 0) {
                        Project selectedProject = projects.get(selectedProjectIndex[0]);
                        openSelectedProject(selectedProject);
                        duilieSum = selectedProject.getPaths().size() + 1;
                        designs= selectedProject.getPaths();
                        Toast.makeText(getContext(), "已打开", Toast.LENGTH_SHORT).show();
                        //摄像机移动中心,缩放
                        double minX = selectedProject.getPaths().values().stream().mapToDouble(p -> p.getX()).min().orElse(0);
                        double maxX = selectedProject.getPaths().values().stream().mapToDouble(p -> p.getX()).max().orElse(0);
                        double minY = selectedProject.getPaths().values().stream().mapToDouble(p -> p.getY()).min().orElse(0);
                        double maxY = selectedProject.getPaths().values().stream().mapToDouble(p -> p.getY()).max().orElse(0);
                        int zoom = (int)( (Math.min(Math.log(360 / (maxX - minX)), Math.log(360 / (maxY - minY)))) * 1.7);
                        baiduMap.animateMapStatus(MapStatusUpdateFactory.newLatLngZoom(new LatLng(selectedProject.getPaths().values().stream().mapToDouble(p -> p.getY()).average().orElse(0),
                                selectedProject.getPaths().values().stream().mapToDouble(p -> p.getX()).average().orElse(0)), zoom));
                        //计算应该缩小到
                        design();
                        dialog.dismiss();
                    } else {
                        Toast.makeText(requireContext(), "请选择一个项目", Toast.LENGTH_SHORT).show();
                    }
                });

                builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());

                builder.show();
            } else {
                Toast.makeText(requireContext(), "没有项目可打开", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openSelectedProjects(List<Project> projects) {
        // 清空之前的 Marker 和 Polyline
        for (Marker marker : markers) {
            marker.remove();
        }
        if (polyline != null) {
            polyline.remove();
        }

        // 添加新的 Marker 和 Polyline
        addMarkersAndPolyline(projects);

        // 设置 Toolbar 标题
        StringBuilder titleBuilder = new StringBuilder("项目: ");
        List<Pointer> points = new ArrayList<>();

        for (int i = 0; i < projects.size(); i++) {
            titleBuilder.append(projects.get(i).getName());
            points.addAll(projects.get(i).getPaths().values());
            if (i < projects.size() - 1) {
                titleBuilder.append(", ");
            }
        }
        Toolbar toolbar = ((MainActivity) requireActivity()).findViewById(R.id.toolbar);
        toolbar.setTitle(titleBuilder.toString());
        double minX = points.stream().mapToDouble(p -> p.getX()).min().orElse(0);
        double maxX = points.stream().mapToDouble(p -> p.getX()).max().orElse(0);
        double minY = points.stream().mapToDouble(p -> p.getY()).min().orElse(0);
        double maxY = points.stream().mapToDouble(p -> p.getY()).max().orElse(0);
        int zoom = (int)( (Math.min(Math.log(360 / (maxX - minX)), Math.log(360 / (maxY - minY)))) * 1.7);
        Log.d("HomeFragment", "save: " + zoom);
        baiduMap.animateMapStatus(MapStatusUpdateFactory.newLatLngZoom(new LatLng(points.stream().mapToDouble(p -> p.getY()).average().orElse(0),
                points.stream().mapToDouble(p -> p.getX()).average().orElse(0)), zoom));

        Toast.makeText(getContext(), "项目已打开", Toast.LENGTH_SHORT).show();
    }



    private void design() {
        isDes = true;
    }
    private void start() {
        if (run == 0) {
            run = 1;
            try  {
                mapView = binding.bmapView;
                mapView.onCreate(getContext(), savedInstanceState);

                baiduMap = mapView.getMap();

                // 设置地图中心点
                LatLng latLng = new LatLng(y, x); // 北京市经纬度
                baiduMap.setMapStatus(MapStatusUpdateFactory.newLatLngZoom(latLng, 13)); // 缩放级别调整为

                // 添加独特样式的标记

                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.mmexport3ce9739b640a7e4ce6464c7392896ad6_1743239672087); // 自定义图标资源
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 120, 120, true); // 缩放到 100x100 像素
                BitmapDescriptor descriptor = BitmapDescriptorFactory.fromBitmap(scaledBitmap);
                MarkerOptions markerOptions = new MarkerOptions()
                        .position(latLng)
                        .title("本人位置")
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
                        } else {
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
                        if (likeMarkers.contains(marker) && !useLikeMarkers.contains(marker)) {
                            //MaterialAlertDialogBuilder
                            handleMarkerClick(marker);
                        }else {
                            showMarkerInfoDialog(marker);
                        }
                        return true; // 返回 true 表示已处理点击事件
                    }
                });
                baiduMap.setOnMapLongClickListener(new BaiduMap.OnMapLongClickListener() {

                    @Override
                    public void onMapLongClick(LatLng latLng) {
                        createLikeMarker(latLng);
                    }
                });

                readLikePlace();

            }catch (Exception e) {

            }
        }
    }
    private void addLikeMarker(final LatLng latLng,String markerName) {
        // 创建 Bundle 对象并设置 x, y, name 内容
        Log.d("addLikeMarker", "addLikeMarker: " + latLng.toString());
        Bundle extraInfo = new Bundle();
        if (latLng.latitude < -90 || latLng.latitude > 90 || latLng.longitude < -180 || latLng.longitude > 180) {
            Log.e("addLikeMarker", "Invalid LatLng: " + latLng);
            return;
        }

        extraInfo.putDouble("x", latLng.longitude);
        extraInfo.putDouble("y", latLng.latitude);
        extraInfo.putString("name", markerName);
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.mmexportbba7d96e1e5550eab0dc29ed9d4715a3_1743252114743);
        bitmap = createMarkerBitmapWithTime(bitmap, "no");
        BitmapDescriptor descriptor = BitmapDescriptorFactory.fromBitmap(bitmap);
        // 创建 MarkerOptions 并附加 Bundle
        if (baiduMap == null) {
            Log.e("addLikeMarker", "BaiduMap is not initialized");
            return;
        }
        if (extraInfo.isEmpty()) {
            Log.e("addLikeMarker", "ExtraInfo is empty");
            return;
        }
        if (descriptor == null) {
            Log.e("addLikeMarker", "BitmapDescriptor creation failed");
            return;
        }
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .title(markerName)
                .icon(descriptor)
                .extraInfo(extraInfo);
        if (markerOptions == null || markerOptions.getPosition() == null || markerOptions.getIcon() == null) {
            Log.e("addLikeMarker", "MarkerOptions configuration error");
            return;
        }
        Marker marker = (Marker) baiduMap.addOverlay(markerOptions);
        if (marker != null) {
            likeMarkers.add(marker);
        } else {
            Log.e("addLikeMarker", "Failed to add marker to map");
        }
    }
    @SuppressLint("MissingInflatedId")
    private void createLikeMarker(LatLng latLng) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle("添加收藏");
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_like, null);
        TextView text = dialogView.findViewById(R.id.text);
        text.setText(latLng.toString());
        TextInputEditText markerNameInput = dialogView.findViewById(R.id.markerNameInput);
        builder.setView(dialogView);

        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // 获取标记名称
                String markerName = markerNameInput.getText().toString().trim();
                if (markerName.isEmpty()) {
                    Toast.makeText(requireContext(), "请输入标记名称", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 创建 Bundle 对象并设置 x, y, name 内容
                Bundle extraInfo = new Bundle();
                extraInfo.putDouble("y", latLng.longitude);
                extraInfo.putDouble("x", latLng.latitude);
                extraInfo.putString("name", markerName);
                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.mmexportbba7d96e1e5550eab0dc29ed9d4715a3_1743252114743);
                Bitmap newBitmap = createMarkerBitmapWithTime(bitmap, "no");

                BitmapDescriptor descriptor = BitmapDescriptorFactory.fromBitmap(newBitmap);
                // 创建 MarkerOptions 并附加 Bundle
                MarkerOptions markerOptions = new MarkerOptions()
                        .position(latLng)
                        .title(markerName)
                        .icon(descriptor)
                        .extraInfo(extraInfo);
                Marker marker = (Marker) baiduMap.addOverlay(markerOptions);
                likeMarkers.add(marker); // 将标记添加到 likeMarkers 列表中
                LikePlace likePlace = new LikePlace();
                likePlace.setName(markerName);
                likePlace.setX(latLng.longitude);
                likePlace.setY(latLng.latitude);
                likePlaces.add(likePlace);
                saveLikePlace();
            }
        });

        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        builder.show();
    }


    private void handlePoiClick(MapPoi poi) {
        // 创建 Pointer 对象
        Pointer pointer = new Pointer();
        pointer.setPointName(poi.getName());
        pointer.setPointId(0); // 可以根据需要设置 ID
        pointer.setPointUUID(poi.getUid());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        String datetime = sdf.format(new Date());
        pointer.setTime(datetime); // 设置当前时间
        pointer.setType("poi");
        pointer.setX(poi.getPosition().longitude);
        pointer.setY(poi.getPosition().latitude);
        pointer.setPay(0); // 可以根据需要设置支付信息

        // 显示弹窗
        showPointerInfoDialog(pointer);
    }
    private void handleMarkerClick(Marker marker) {
        // 创建 Pointer 对象
        Pointer pointer = new Pointer();
        pointer.setPointName(marker.getName());
        pointer.setPointId(0); // 可以根据需要设置 ID
        UUID uuid = new UUID(new Random().nextInt(10), new Random().nextInt(15));
        pointer.setPointUUID(uuid.toString());
        pointer.setType("poi");
        Bundle extraInfo = marker.getExtraInfo();
        double x = (double) extraInfo.get("x");
        double y = (double) extraInfo.get("y");
        String name = (String) extraInfo.get("name");
        pointer.setPointName(name);

        pointer.setX(x);
        pointer.setY(y);
        pointer.setPay(0); // 可以根据需要设置支付信息
        //UUID就是在List中的位置
        int index = likeMarkers.indexOf(marker);
        pointer.setPointUUID(index + "");

        // 显示弹窗
        showPointerInfoDialog(pointer,1);
    }
    @SuppressLint("MissingInflatedId")
    private void showPointerInfoDialog(Pointer pointer ) {
        showPointerInfoDialog(pointer,0);
    }
    @SuppressLint("MissingInflatedId")
    private void showPointerInfoDialog(Pointer pointer,int typer) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.design_dialog, null);
        builder.setView(dialogView);

        // 找到 TextInputEditText 和 Button
        TextInputEditText pointNameEditText = dialogView.findViewById(R.id.pointName);
        TextInputEditText pointIdEditText = dialogView.findViewById(R.id.pointId);
        TextInputEditText pointUUIDEditText = dialogView.findViewById(R.id.pointUUID);
        TextInputEditText datetimeEditText = dialogView.findViewById(R.id.datetime);
        TextInputEditText typeEditText = dialogView.findViewById(R.id.type);
        TextInputEditText xEditText = dialogView.findViewById(R.id.x);
        TextInputEditText yEditText = dialogView.findViewById(R.id.y);
        TextInputEditText payEditText = dialogView.findViewById(R.id.pay);
        MaterialButton datetimePickerButton = dialogView.findViewById(R.id.datetimePickerButton);
        MaterialButton updateButton = dialogView.findViewById(R.id.updateButton);
        MaterialButton deleteButton = dialogView.findViewById(R.id.deleteButton);
        MaterialCheckBox isEndCheckBox = dialogView.findViewById(R.id.isEndCheckBox); // 找到 CheckBox

        // 填充信息
        pointNameEditText.setText(pointer.getPointName());
        pointIdEditText.setText(String.valueOf(duilieSum));
        pointUUIDEditText.setText(pointer.getPointUUID());
        datetimeEditText.setText(pointer.getTime());
        typeEditText.setText(pointer.getType());
        xEditText.setText(String.valueOf(pointer.getX()));
        yEditText.setText(String.valueOf(pointer.getY()));
        payEditText.setText(String.valueOf(pointer.getPay()));
        int s = 1;
        boolean b = false;
        String key = null;

        String oldTime = "0";
        //designs决定
        //获取designs内容和key
        for (Map.Entry<String, Pointer> entry : designs.entrySet()) {
            Pointer p = entry.getValue();
            if(p.getPointUUID().equals(pointer.getPointUUID())) {
                b = true;
                key = entry.getKey();
                Log.d("designs", "1");
                datetimeEditText.setText(p.getTime());
                oldTime = p.getTime();
                Log.d("designs", "2");
                pointNameEditText.setText(p.getPointName());
                Log.d("designs", "3");
                pointIdEditText.setText(p.getPointId() + "");
                Log.d("designs", "4");
                pointUUIDEditText.setText(p.getPointUUID());
                Log.d("designs", "5");
                typeEditText.setText(p.getType());
                Log.d("designs", "6");
                xEditText.setText(String.valueOf(p.getX()));
                Log.d("designs", "7");
                yEditText.setText(String.valueOf(p.getY()));
                Log.d("designs", "8");
                payEditText.setText(String.valueOf(p.getPay()));
                break;
            }
            s ++;        
        }

        if (b || typer == 1) {
            deleteButton.setVisibility(View.VISIBLE);
            updateButton.setVisibility(View.VISIBLE);
            pointIdEditText.setText(key);
        }
        // 设置日期时间选择器
        // 设置日期时间选择器
        String finalOldTime = oldTime;
        datetimePickerButton.setOnClickListener(v -> {
            // 创建日期选择器
            MaterialDatePicker.Builder<Long> datePickerBuilder = MaterialDatePicker.Builder.datePicker();
            datePickerBuilder.setTitleText("选择日期");

            // 如果 oldTime 不等于 "0"，设置默认日期
            if (!finalOldTime.equals("0")) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                try {
                    Date date = sdf.parse(finalOldTime);
                    datePickerBuilder.setSelection(date.getTime());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            MaterialDatePicker<Long> datePicker = datePickerBuilder.build();

            datePicker.show(getParentFragmentManager(), "DATE_PICKER_TAG");

            datePicker.addOnPositiveButtonClickListener(selection -> {
                // 创建时间选择器
                int hour = 12;
                int minute = 0;
                // 如果 oldTime 不等于 "0"，设置默认时间
                if (!finalOldTime.equals("0")) {
                    hour = Integer.parseInt(finalOldTime.split(" ")[1].split(":")[0]);
                    minute = Integer.parseInt(finalOldTime.split(" ")[1].split(":")[1]);
                }

                MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                        .setTimeFormat(TimeFormat.CLOCK_24H)
                        .setHour(hour)
                        .setMinute(minute)
                        .setTitleText("选择时间")
                        .build();



                timePicker.show(getParentFragmentManager(), "TIME_PICKER_TAG");

                timePicker.addOnPositiveButtonClickListener(dialog -> {
                    int hour1 = timePicker.getHour();
                    int minute1 = timePicker.getMinute();
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(selection);
                    calendar.set(Calendar.HOUR_OF_DAY, hour1);
                    calendar.set(Calendar.MINUTE, minute1);

                    // 格式化日期时间
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                    String datetime = sdf.format(calendar.getTime());
                    datetimeEditText.setText(datetime);
                });
            });
        });


        // 设置更新按钮
        int finalS = s;
        updateButton.setOnClickListener(v -> {
            // 获取输入的信息
            if (pointNameEditText.getText().toString().isEmpty() ||
                    pointIdEditText.getText().toString().isEmpty() ||
                    pointUUIDEditText.getText().toString().isEmpty() ||
                    datetimeEditText.getText().toString().isEmpty() ||
                    typeEditText.getText().toString().isEmpty() ||
                    xEditText.getText().toString().isEmpty() ||
                    yEditText.getText().toString().isEmpty() ||
                    payEditText.getText().toString().isEmpty() || typer == 1) {
                Toast.makeText(requireContext(), "请填写完整信息", Toast.LENGTH_SHORT).show();
                return;
            }
            String pointName = pointNameEditText.getText().toString();
            int pointId = Integer.parseInt(pointIdEditText.getText().toString());
            String pointUUID = pointUUIDEditText.getText().toString();
            String datetime = datetimeEditText.getText().toString();
            String type = typeEditText.getText().toString();
            double x = Double.parseDouble(xEditText.getText().toString());
            double y = Double.parseDouble(yEditText.getText().toString());
            int pay = Integer.parseInt(payEditText.getText().toString());

            // 更新 Pointer 对象
            pointer.setPointName(pointName);
            pointer.setPointId(pointId);
            pointer.setPointUUID(pointUUID);
            pointer.setTime(datetime);
            pointer.setType(type);
            pointer.setX(x);
            pointer.setY(y);
            pointer.setPay(pay);

            if (typer == 1) {
                likeMarkers.remove(Integer.parseInt(pointer.getPointUUID()));

                //取出likeMarkers,全部转成likePlace
                likePlaces.clear();
                for (Marker marker : likeMarkers) {
                    LikePlace likePlace = new LikePlace();
                    likePlace.setName(marker.getTitle());
                    likePlace.setX(marker.getPosition().latitude);
                    likePlace.setY(marker.getPosition().longitude);
                    likePlaces.add(likePlace);
                }
                saveLikePlace();
                readLikePlace();
                Toast.makeText(requireContext(), "已更新", Toast.LENGTH_SHORT).show();
                return;
            }
            // 更新适配器的数据集
            pointsAdapter.notifyDataSetChanged();
            designs.remove(finalS + "");
            if (!designs.containsKey(pointId + "")) {
                designs.put(pointId + "", pointer);
            }else {
                //两者交换位置
                Pointer p = designs.get(pointId + "");
                designs.put(pointId + "", pointer);
                Log.d("pointIdEditText", pointId +"|" + finalS);
                designs.put(finalS + "", p);
            }
            // 更新地图上的 Marker
            updateMarker(pointer);
            duilieSum ++;
            // 更新项目
            Project project = new Project();
            project.setPaths(designs);
            addMarkersAndPolyline(project);

            Toast.makeText(getContext(), "更新成功!", Toast.LENGTH_SHORT).show();

            if (type.equals("终点") || isEndCheckBox.isChecked()) {
                Toast.makeText(getContext(), "规划完成~进入下一步", Toast.LENGTH_SHORT).show();
                save();
            }
        });

        // 设置删除按钮
        deleteButton.setOnClickListener(v -> {
            // 从 designs 中删除 Pointer
            if (typer == 1) {
                likeMarkers.remove(Integer.parseInt(pointer.getPointUUID()));

                //取出likeMarkers,全部转成likePlace
                likePlaces.clear();
                for (Marker marker : likeMarkers) {
                    LikePlace likePlace = new LikePlace();
                    likePlace.setName(marker.getTitle());
                    likePlace.setX(marker.getPosition().latitude);
                    likePlace.setY(marker.getPosition().longitude);
                    likePlaces.add(likePlace);
                }
                saveLikePlace();
                readLikePlace();
                Toast.makeText(requireContext(), "已删除", Toast.LENGTH_SHORT).show();
                return;
            }

            designs.values().removeIf(p -> p.getPointUUID().equals(pointer.getPointUUID()));

            // 更新适配器的数据集
            pointsAdapter.clear();
            pointsAdapter.addAll(designs.values());
            pointsAdapter.notifyDataSetChanged();

            // 删除地图上的 Marker
            removeMarker(pointer);

            // 更新项目
            Project project = new Project();
            project.setPaths(designs);
            addMarkersAndPolyline(project);

            Toast.makeText(getContext(), "删除成功!", Toast.LENGTH_SHORT).show();
        });
        if (!b) {

            // 设置确定按钮
            builder.setPositiveButton("添加", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // 获取输入的信息
                    if (pointNameEditText.getText().toString().isEmpty() ||
                            pointIdEditText.getText().toString().isEmpty() ||
                            pointUUIDEditText.getText().toString().isEmpty() ||
                            datetimeEditText.getText().toString().isEmpty() ||
                            typeEditText.getText().toString().isEmpty() ||
                            xEditText.getText().toString().isEmpty() ||
                            yEditText.getText().toString().isEmpty() ||
                            payEditText.getText().toString().isEmpty()) {
                        Toast.makeText(requireContext(), "请填写完整信息", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String pointName = pointNameEditText.getText().toString();
                    String pointUUID = pointUUIDEditText.getText().toString();
                    int pointId = Integer.parseInt(pointIdEditText.getText().toString());
                    String datetime = datetimeEditText.getText().toString();
                    String type = typeEditText.getText().toString();
                    double x = Double.parseDouble(xEditText.getText().toString());
                    double y = Double.parseDouble(yEditText.getText().toString());
                    int pay = Integer.parseInt(payEditText.getText().toString());

                    // 创建 Pointer 对象
                    Pointer newPointer = new Pointer();
                    newPointer.setPointName(pointName);
                    newPointer.setPointId(pointId);
                    newPointer.setPointUUID(pointUUID);
                    newPointer.setTime(datetime);
                    newPointer.setType(type);
                    newPointer.setX(x);
                    newPointer.setY(y);
                    newPointer.setPay(pay);

                    if (type.equals("终点")) {
                        isDes = false;
                    }
                    designs.put(duilieSum + "", newPointer);
                    pointsAdapter.clear();
                    pointsAdapter.addAll(designs.values());
                    pointsAdapter.notifyDataSetChanged();
                    Project project = new Project();
                    project.setPaths(designs);
                    addMarkersAndPolyline(project);
                    duilieSum++;

                    Log.d("HomeFragment", "onClick: " + newPointer.getPointName());
                    if (type.equals("终点") || isEndCheckBox.isChecked()) {
                        Toast.makeText(getContext(), "规划完成~进入下一步", Toast.LENGTH_SHORT).show();
                        save();
                    }
                    Toast.makeText(getContext(), "添加成功!", Toast.LENGTH_SHORT).show();
                }
            });
        }
        // 设置取消按钮
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.show();
    }

    private void doCheck(Pointer pointer) {
        Log.d("HomeFragment", "doCheck: " + pointer.getTime());
        showPointerInfoDialog(pointer);
    }

    private void save() {
        Project project = new Project();
        project.setPaths(designs);
        //缩小摄像机到一个可以正好看到全部路线的地方

        double minX = project.getPaths().values().stream().mapToDouble(p -> p.getX()).min().orElse(0);
        double maxX = project.getPaths().values().stream().mapToDouble(p -> p.getX()).max().orElse(0);
        double minY = project.getPaths().values().stream().mapToDouble(p -> p.getY()).min().orElse(0);
        double maxY = project.getPaths().values().stream().mapToDouble(p -> p.getY()).max().orElse(0);
        int zoom = (int)( (Math.min(Math.log(360 / (maxX - minX)), Math.log(360 / (maxY - minY)))) * 1.7);
        Log.d("HomeFragment", "save: " + zoom);
        baiduMap.animateMapStatus(MapStatusUpdateFactory.newLatLngZoom(new LatLng(project.getPaths().values().stream().mapToDouble(p -> p.getY()).average().orElse(0),
                project.getPaths().values().stream().mapToDouble(p -> p.getX()).average().orElse(0)), zoom));
        //计算应该缩小到
        showSaveBottomSheet(project);
    }
    private void showSaveBottomSheet(Project project) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.bottom_sheet_save_dialog, null);
        bottomSheetDialog.setContentView(dialogView);

        // 找到按钮并设置点击事件
        MaterialButton cancelButton = dialogView.findViewById(R.id.cancelButton);
        MaterialButton saveButton = dialogView.findViewById(R.id.saveButton);
        // 设置 BottomSheetDialog 不可取消
        bottomSheetDialog.setCancelable(false);
        bottomSheetDialog.setCanceledOnTouchOutside(false);


        cancelButton.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            exitProject();
        });

        saveButton.setOnClickListener(v -> {
            // 保存项目逻辑
            saveProject(project);
            bottomSheetDialog.dismiss();
        });

        // 设置 BottomSheetDialog 的位置
        FrameLayout bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) bottomSheet.getLayoutParams();
            layoutParams.setMargins(0, 0, 0, 100); // 设置底部边距为 100dp
            bottomSheet.setLayoutParams(layoutParams);
        }

        bottomSheetDialog.show();
    }
    @SuppressLint("MissingInflatedId")
    private void saveProject(Project project) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle("保存项目");

        // 创建并设置对话框视图
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_save_project, null);
        builder.setView(dialogView);

        // 找到输入框
        TextInputEditText projectNameEditText = dialogView.findViewById(R.id.projectName);
        TextInputEditText dateStartEditText = dialogView.findViewById(R.id.dateStart);
        TextInputEditText dateEndEditText = dialogView.findViewById(R.id.dateEnd);
        // 设置日期选择器
        dateStartEditText.setOnClickListener(v -> {
            MaterialDatePicker.Builder<Long> datePickerBuilder = MaterialDatePicker.Builder.datePicker();
            datePickerBuilder.setTitleText("选择开始日期");
            MaterialDatePicker<Long> datePicker = datePickerBuilder.build();

            datePicker.show(getParentFragmentManager(), "DATE_START_PICKER_TAG");

            datePicker.addOnPositiveButtonClickListener(selection -> {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String dateStart = sdf.format(new Date(selection));
                dateStartEditText.setText(dateStart);
            });
        });

        dateEndEditText.setOnClickListener(v -> {
            MaterialDatePicker.Builder<Long> datePickerBuilder = MaterialDatePicker.Builder.datePicker();
            datePickerBuilder.setTitleText("选择结束日期");
            MaterialDatePicker<Long> datePicker = datePickerBuilder.build();

            datePicker.show(getParentFragmentManager(), "DATE_END_PICKER_TAG");

            datePicker.addOnPositiveButtonClickListener(selection -> {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String dateEnd = sdf.format(new Date(selection));
                dateEndEditText.setText(dateEnd);
            });
        });

        // 设置确定按钮
        builder.setPositiveButton("保存", (dialog, which) -> {
            // 获取输入的信息
            if (projectNameEditText.getText().toString().isEmpty() ||
                    dateStartEditText.getText().toString().isEmpty() ||
                    dateEndEditText.getText().toString().isEmpty()) {
                Toast.makeText(requireContext(), "请填写完整信息", Toast.LENGTH_SHORT).show();
                isDes=true;
                return;
            }

            String projectName = projectNameEditText.getText().toString();
            String dateStart = dateStartEditText.getText().toString();
            String dateEnd = dateEndEditText.getText().toString();

            // 设置项目信息
            project.setName(projectName);
            project.setDate_start(dateStart);
            project.setDate_end(dateEnd);

            // 保存项目到 SharedPreferences 或其他存储方式
            project.setDate_create(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
            setColorAndSave(project);
        });

        // 设置取消按钮
        builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());

        builder.show();
    }
    @SuppressLint("MissingInflatedId")
    private void setColorAndSave(Project project) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle("选择线条颜色");

        // 创建并设置对话框视图
        View dialogView = getLayoutInflater().inflate(R.layout.color_picker_dialog, null);
        builder.setView(dialogView);

        ColorPickerView colorPickerView = dialogView.findViewById(R.id.colorPickerView);
        colorPickerView.setVisibility(View.VISIBLE);
        colorPickerView.setInitialColor(Color.parseColor("#7FFF0000")); // 设置初始颜色为红色，透明度50%

        colorPickerView.setColorListener(new ColorEnvelopeListener() {
            @Override
            public void onColorSelected(ColorEnvelope colorEnvelope, boolean fromUser) {
                // 当用户选择了颜色时触发此方法
                int selectedColor = colorEnvelope.getColor();
                int alpha = 128; // 50% 透明度
                int argbColor = Color.argb(alpha, Color.red(selectedColor), Color.green(selectedColor), Color.blue(selectedColor));
                project.setColorARGB(String.format("#%08X", argbColor));
                Log.d("HomeFragment", "onColorSelected: " + String.format("#%08X", argbColor));
            }
        });

        builder.setPositiveButton("保存", (dialog, which) -> {
            projects.add(project);
            saveProjectToSharedPreferences();
            isDes = false;

            Toast.makeText(getContext(), "项目已保存!", Toast.LENGTH_SHORT).show();

            Toolbar toolbar = (Toolbar)  requireActivity().findViewById(R.id.toolbar);
            toolbar.setTitle("主页");
            dialog.dismiss();
        });
        builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());
        builder.show();
    }



    private void saveProjectToSharedPreferences() {
        Gson gson = new Gson();
        String projectJson = gson.toJson(projects);
        SharedPreferences.Editor editor = save.edit();
        editor.putString("projects", projectJson);
        editor.apply();
        designs = new HashMap<>();
        duilieSum = 0;
    }


    private void updateMarker(Pointer pointer) {
        for (Marker marker : markers) {
            Bundle extraInfo = marker.getExtraInfo();
            if (extraInfo != null && extraInfo.getString("uuid").equals(pointer.getPointUUID())) {
                marker.setPosition(new LatLng(pointer.getY(), pointer.getX()));
                marker.setTitle(pointer.getPointName());
                break;
            }
        }
    }

    private void removeMarker(Pointer pointer) {
        for (Marker marker : markers) {
            Bundle extraInfo = marker.getExtraInfo();
            if (extraInfo != null && extraInfo.getString("uuid").equals(pointer.getPointUUID())) {
                marker.remove();
                markers.remove(marker);
                break;
            }
        }
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
                run = 0;
                reverseGeocode(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
            } else {
                Toast.makeText(requireActivity().getApplicationContext(), "无法获取最新定位信息", Toast.LENGTH_SHORT).show();
                setDefault();
                Log.d("Location", "无法获取最新定位信息");
            }
        }

        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 120000, 160f, new LocationListener() {
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
                    setDefault();
                    Toast.makeText(requireActivity().getApplicationContext(), "关闭定位", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            setDefault();

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
                start();
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
    private void setDefault() {
        //定位北京
        x = 116.407525;
        y = 39.90403;
        start();
    }
    private void openSelectedProject(Project project) {
        Toolbar toolbar = ((MainActivity) requireActivity()).findViewById(R.id.toolbar);
        toolbar.setTitle(project.getName());
        addMarkersAndPolyline(project);
    }
    private String nowdate = "0";
    private int[] color = new int[3];
    private Bitmap createMarkerBitmapWithTime(Bitmap baseBitmap, String time) {
        if(time.equals("no")) {
            baseBitmap = Bitmap.createScaledBitmap(baseBitmap, 100, 110, true);
            return baseBitmap;
        }
        // 创建一个新的位图，宽度为原位图宽度加上文本宽度，高度为原位图高度
        baseBitmap = Bitmap.createScaledBitmap(baseBitmap, 60, 65, true); // 缩放到 60x65 像素

        int baseWidth = baseBitmap.getWidth() + 22;
        int baseHeight = baseBitmap.getHeight();
        int textWidth = (int) (time.length() * 20); // 根据字体大小调整
        int newWidth = baseWidth + textWidth - 50; // 加上一些间距
        int newHeight = baseHeight;

        Bitmap newBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(newBitmap);

        // 绘制原位图
        canvas.drawBitmap(baseBitmap, 0, 0, null);

        // 设置文本颜色和样式
        Paint textPaint = new Paint();
        textPaint.setTextSize(30); // 字体大小
        textPaint.setTextAlign(Paint.Align.LEFT); // 左对齐

        // 提取日期部分
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String currentDate = dateFormat.format(new Date());
        String markerDate = time.substring(0, 10); // 提取 yyyy-MM-dd 部分
        if (nowdate.equals("0")) {
            nowdate = markerDate;
            Random random = new Random();
            int red = random.nextInt(256);
            int green = random.nextInt(256);
            int blue = random.nextInt(256);
            color[0] = red;
            color[1] = green;
            color[2] = blue;
        }
        // 比较日期
        if (nowdate.equals(markerDate)) {
            textPaint.setColor(Color.rgb(color[0], color[1], color[2])); // 同一天使用绿色
        } else {
            // 不同一天使用随机颜色
            nowdate = markerDate;
            Random random = new Random();
            int red = random.nextInt(256);
            int green = random.nextInt(256);
            int blue = random.nextInt(256);
            color[0] = red;
            color[1] = green;
            color[2] = blue;
            textPaint.setColor(Color.rgb(color[0], color[1], color[2])); // 同一天使用绿色
        }

        // 绘制时间文本
        canvas.drawText(time, baseWidth - 20, baseHeight / 2 + 4, textPaint); // (x, y) 是文本起始坐标

        return newBitmap;
    }



    private void addMarkersAndPolyline(Project project) {
        Map<String, Pointer> paths = project.getPaths();
        List<LatLng> points = new ArrayList<>();
        List<Pointer> sortedPointers = new ArrayList<>(paths.values());
        Collections.sort(sortedPointers, Comparator.comparingInt(Pointer::getPointId));

        for (Pointer pointer : sortedPointers) {
            Log.d(pointer.getPointName(), pointer.getX() + " " + pointer.getY());
            if (pointer.getX() == 0.0 && pointer.getY() == 0.0) {
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
        for (Pointer pointer : sortedPointers) {
            LatLng point = new LatLng(pointer.getY(), pointer.getX());
            //随机选择图片0,1,2
            int random = (int) (Math.random() * 3);
            Bitmap bitmap = null;
            switch (random) {
                case 0:
                    bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.mmexport3ce9739b640a7e4ce6464c7392896ad6_1743239672087);
                    break;
                case 1:
                    bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.mmexporta952e89b082c27bff635a278e0643b9b_1743252098398);
                    break;
                case 2:
                    bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.mmexportbba7d96e1e5550eab0dc29ed9d4715a3_1743252114743);
                    break;
            }
// 创建包含时间信息的 Bitmap
            String time = pointer.getTime();
            Bitmap newBitmap = createMarkerBitmapWithTime(bitmap, time);

            BitmapDescriptor descriptor = BitmapDescriptorFactory.fromBitmap(newBitmap);

            MarkerOptions markerOptions = new MarkerOptions()
                    .position(point)
                    .icon(descriptor);

            // 设置额外信息
            Bundle bundle = new Bundle();
            bundle.putString("name", pointer.getType());
            bundle.putString("uuid", pointer.getPointUUID());
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

            // 获取项目颜色
            int polylineColor = 0xAAFF0000;
            try {
                polylineColor = Color.parseColor(project.getColorARGB());
            }catch (Exception e) {}

            OverlayOptions polylineOptions = new PolylineOptions()
                    .points(polylinePoints)
                    .width(10)
                    .color(polylineColor);
            polyline = (Polyline) baiduMap.addOverlay(polylineOptions);
        }
    }
    private void addMarkersAndPolyline(List<Project> projects) {
        // 清除之前的 Marker 和 Polyline
        for (Marker marker : markers) {
            marker.remove();
        }
        if (polyline != null) {
            polyline.remove();
        }

        for (Project project : projects) {
            Map<String, Pointer> paths = project.getPaths();
            List<LatLng> points = new ArrayList<>();
            List<Pointer> sortedPointers = new ArrayList<>(paths.values());
            Collections.sort(sortedPointers, Comparator.comparingInt(Pointer::getPointId));

            for (Pointer pointer : sortedPointers) {
                Log.d(pointer.getPointName(), pointer.getX() + " " + pointer.getY());
                if (pointer.getX() == 0.0 && pointer.getY() == 0.0) {
                    continue;
                }
                points.add(new LatLng(pointer.getY(), pointer.getX()));
            }

            // 添加新的 Marker
            for (Pointer pointer : sortedPointers) {
                LatLng point = new LatLng(pointer.getY(), pointer.getX());
                Bitmap bitmap = null;
                int random = (int) (Math.random() * 3);
                switch (random) {
                    case 0:
                        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.mmexport3ce9739b640a7e4ce6464c7392896ad6_1743239672087);
                        break;
                    case 1:
                        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.mmexporta952e89b082c27bff635a278e0643b9b_1743252098398);
                        break;
                    case 2:
                        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.mmexportbba7d96e1e5550eab0dc29ed9d4715a3_1743252114743);
                        break;
                }
// 创建包含时间信息的 Bitmap
                String time = pointer.getTime();
                Bitmap newBitmap = createMarkerBitmapWithTime(bitmap, time);

                BitmapDescriptor descriptor = BitmapDescriptorFactory.fromBitmap(newBitmap);

                MarkerOptions markerOptions = new MarkerOptions()
                        .position(point)
                        .icon(descriptor);

                // 设置额外信息
                Bundle bundle = new Bundle();
                bundle.putString("name", pointer.getType());
                bundle.putString("uuid", pointer.getPointUUID());
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

                // 获取项目颜色
                int polylineColor = 0xAAFF0000;
                try {
                    polylineColor = Color.parseColor(project.getColorARGB());
                }catch (Exception e) {}

                OverlayOptions polylineOptions = new PolylineOptions()
                        .points(polylinePoints)
                        .width(10)
                        .color(polylineColor);
                polyline = (Polyline) baiduMap.addOverlay(polylineOptions);
            }
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
        run = 0;
        binding = null;
    }
}
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
import android.graphics.Color;
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
import org.astral.astrip.been.Pointer;
import org.astral.astrip.been.Project;
import org.astral.astrip.databinding.FragmentHomeBinding;

import java.io.IOException;
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
        duilieSum = 1;
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
                option1.setOnClickListener(v -> {
                    // 处理选项1的点击事件
                    openProject() ;
                });

                MaterialButton option2 = dialogView.findViewById(R.id.option2);
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
                });
                MaterialButton option3 = dialogView.findViewById(R.id.option3);

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
        for (int i = 0; i < projects.size(); i++) {
            titleBuilder.append(projects.get(i).getName());
            if (i < projects.size() - 1) {
                titleBuilder.append(", ");
            }
        }
        Toolbar toolbar = ((MainActivity) requireActivity()).findViewById(R.id.toolbar);
        toolbar.setTitle(titleBuilder.toString());

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
                        showMarkerInfoDialog(marker);
                        return true; // 返回 true 表示已处理点击事件
                    }
                });
            }catch (Exception e) {

            }
        }
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
    @SuppressLint("MissingInflatedId")
    private void showPointerInfoDialog(Pointer pointer) {
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
        for(Pointer p : designs.values()) {
            Log.d("pointIdEditText", p.getPointUUID());
            Log.d("pointIdEditText", pointer.getPointUUID());
            if(p.getPointUUID().equals(pointer.getPointUUID())) {
                b = true;
                break;
            }
            s ++;
        }
        if (b) {
            deleteButton.setVisibility(View.VISIBLE);
            updateButton.setVisibility(View.VISIBLE);
            pointIdEditText.setText(s + "");
        }
        // 设置日期时间选择器
        datetimePickerButton.setOnClickListener(v -> {
            // 创建日期选择器
            MaterialDatePicker.Builder<Long> datePickerBuilder = MaterialDatePicker.Builder.datePicker();
            datePickerBuilder.setTitleText("Select Date");
            MaterialDatePicker<Long> datePicker = datePickerBuilder.build();

            datePicker.show(getParentFragmentManager(), "DATE_PICKER_TAG");

            datePicker.addOnPositiveButtonClickListener(selection -> {
                // 创建时间选择器
                MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                        .setTimeFormat(TimeFormat.CLOCK_24H)
                        .setHour(12)
                        .setMinute(0)
                        .setTitleText("Select Time")
                        .build();

                timePicker.show(getParentFragmentManager(), "TIME_PICKER_TAG");

                timePicker.addOnPositiveButtonClickListener(dialog -> {
                    int hour = timePicker.getHour();
                    int minute = timePicker.getMinute();
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(selection);
                    calendar.set(Calendar.HOUR_OF_DAY, hour);
                    calendar.set(Calendar.MINUTE, minute);

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
                    payEditText.getText().toString().isEmpty()) {
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
        });

        // 设置删除按钮
        deleteButton.setOnClickListener(v -> {
            // 从 designs 中删除 Pointer
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
                        isDes = false;
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

            Toast.makeText(getContext(), "项目已保存!", Toast.LENGTH_SHORT).show();
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
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.logo);
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 100, 65, true); // 缩放到 100x65 像素
            BitmapDescriptor descriptor = BitmapDescriptorFactory.fromBitmap(scaledBitmap);
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
            for (Pointer pointer : paths.values()) {
                Log.d(pointer.getPointName(), pointer.getX() + " " + pointer.getY());
                if (pointer.getX() == 0.0 && pointer.getY() == 0.0) {
                    continue;
                }
                points.add(new LatLng(pointer.getY(), pointer.getX()));
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
                    Log.d("Location", project.getColorARGB());
                    polylineColor = Color.parseColor(project.getColorARGB());
                }catch (Exception e) {
                    e.printStackTrace();
                }

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
package org.astral.astrip.ui.gallery;

import android.annotation.SuppressLint;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.astral.astrip.R;
import org.astral.astrip.adapter.ProjectAdapter;
import org.astral.astrip.been.Project;
import org.astral.astrip.databinding.FragmentGalleryBinding;
import org.astral.astrip.ui.home.HomeFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GalleryFragment extends Fragment {

    private FragmentGalleryBinding binding;
    private RecyclerView recyclerView;
    private ProjectAdapter projectAdapter;
    private List<Project> projects;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        GalleryViewModel galleryViewModel =
                new ViewModelProvider(this).get(GalleryViewModel.class);

        binding = FragmentGalleryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // 初始化 RecyclerView
        recyclerView = binding.recyclerView;
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        projects = new ArrayList<>();
        projectAdapter = new ProjectAdapter(projects);
        recyclerView.setAdapter(projectAdapter);
        //设置点击效果
        FloatingActionButton set = binding.set;
        // 从 SharedPreferences 中读取项目列表
        loadProjectsFromSharedPreferences();
        sett(set);
        return root;
    }
    private void sett(FloatingActionButton set) {
        set.setOnClickListener(new View.OnClickListener() {
            @Override
            @SuppressLint("MissingInflatedId")
            public void onClick(View view) {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
                // 显示导入,导出选项
                builder.setItems(new CharSequence[]{"导入", "导出","删除"}, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            // 导入
                            MaterialAlertDialogBuilder importBuilder = new MaterialAlertDialogBuilder(requireContext());
                            importBuilder.setTitle("导入");
                            View importDialogView = getLayoutInflater().inflate(R.layout.dialog_import, null);
                            importBuilder.setView(importDialogView);

                            TextInputEditText importEditText = importDialogView.findViewById(R.id.importEditText);
                            importBuilder.setPositiveButton("导入", (dialog1, which1) -> {
                                String json = importEditText.getText().toString();
                                if (json.isEmpty()) {
                                    Toast.makeText(requireContext(), "请输入有效的 JSON 内容", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                try {
                                    Gson gson = new Gson();
                                    List<Project> importedProjects = gson.fromJson(json, new TypeToken<List<Project>>() {}.getType());
                                    if (importedProjects != null && !importedProjects.isEmpty()) {
                                        projects.addAll(importedProjects);
                                        projectAdapter.setProjects(projects);
                                        projectAdapter.notifyDataSetChanged();
                                        saveProjectsToSharedPreferences();
                                        Toast.makeText(requireContext(), "导入成功", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(requireContext(), "导入的 JSON 内容无效", Toast.LENGTH_SHORT).show();
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    Toast.makeText(requireContext(), "导入失败，请检查 JSON 格式", Toast.LENGTH_SHORT).show();
                                }
                            });
                            importBuilder.setNegativeButton("取消", (dialog1, which1) -> dialog1.dismiss());
                            importBuilder.show();
                            break;
                        case 1:
                            // 导出
                            MaterialAlertDialogBuilder builder1 = new MaterialAlertDialogBuilder(requireContext());

                            builder1.setTitle("选择项目");
                            final String[] projectNames = projects.stream()
                                    .map(Project::getName)
                                    .toArray(String[]::new);
                            // 选择的项目索引
                            final boolean[] selectedProjects = new boolean[projectNames.length];

                            builder1.setMultiChoiceItems(projectNames, selectedProjects, (dialog1, which1, isChecked) -> {
                                // 处理选择事件
                                selectedProjects[which1] = isChecked;
                            });
                            builder1.setPositiveButton("导出", (dialog1, which1) -> {
                                // 处理打开按钮点击事件
                                List<Project> selectedProjectList = new ArrayList<>();
                                for (int i = 0; i < selectedProjects.length; i++) {
                                    if (selectedProjects[i]) {
                                        selectedProjectList.add(projects.get(i));
                                    }
                                }
                                if (!selectedProjectList.isEmpty()) {
                                    // 导出到剪切板
                                    ClipboardManager clipboard = (ClipboardManager) requireActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                                    StringBuilder sb = new StringBuilder();
                                    // selectedProjectList 转为 json
                                    Gson gson = new Gson();
                                    String json = gson.toJson(selectedProjectList);
                                    sb.append(json);
                                    clipboard.setText(sb.toString());
                                    Toast.makeText(requireContext(), "已复制到剪切板", Toast.LENGTH_SHORT).show();
                                    dialog1.dismiss();
                                } else {
                                    Toast.makeText(requireContext(), "请选择一个项目", Toast.LENGTH_SHORT).show();
                                }
                            });
                            builder1.setNegativeButton("取消", (dialog1, which1) -> dialog1.dismiss());
                            builder1.show();
                            break;
                        case 2: {
                            {
                                MaterialAlertDialogBuilder a = new MaterialAlertDialogBuilder(requireContext());

                                a.setTitle("选择项目");
                                final String[] a2 = projects.stream()
                                        .map(Project::getName)
                                        .toArray(String[]::new);
                                // 选择的项目索引
                                final boolean[] b2 = new boolean[a2.length];

                                a.setMultiChoiceItems(a2, b2, (dialog1, which1, isChecked) -> {
                                    // 处理选择事件
                                    b2[which1] = isChecked;
                                });
                                a.setPositiveButton("删除", (dialog1, which1) -> {
                                    // 处理打开按钮点击事件
                                    List<Project> selectedProjectList = new ArrayList<>();
                                    for (int i = 0; i < a2.length; i++) {
                                        if (b2[i]) {
                                            selectedProjectList.add(projects.get(i));
                                        }
                                    }

                                    if (!selectedProjectList.isEmpty()) {
                                        projects.removeAll(selectedProjectList);
                                        Toast.makeText(requireContext(), "已删除", Toast.LENGTH_SHORT).show();
                                        projectAdapter.notifyDataSetChanged();
                                        saveProjectsToSharedPreferences();
                                        dialog1.dismiss();
                                    } else {
                                        Toast.makeText(requireContext(), "请选择一个项目", Toast.LENGTH_SHORT).show();
                                    }

                                });
                                a.setNegativeButton("取消", (dialog1, which1) -> dialog1.dismiss());
                                a.show();
                                break;
                            }
                        }
                    }
                });
                builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());
                builder.show();
            }
        });

    }

    private void saveProjectsToSharedPreferences() {
        SharedPreferences save = requireContext().getSharedPreferences("save", requireContext().MODE_PRIVATE);
        SharedPreferences.Editor editor = save.edit();
        Gson gson = new Gson();
        String projectJson = gson.toJson(projects);
        editor.putString("projects", projectJson);
        editor.apply();
    }

    private void loadProjectsFromSharedPreferences() {
        SharedPreferences save = requireContext().getSharedPreferences("save", requireContext().MODE_PRIVATE);
        String json = save.getString("projects", "");
        if (!json.isEmpty()) {
            projects = new Gson().fromJson(json, new TypeToken<List<Project>>() {}.getType());
            projectAdapter.setProjects(projects);
            projectAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

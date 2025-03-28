package org.astral.astrip.ui.gallery;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.astral.astrip.R;
import org.astral.astrip.adapter.ProjectAdapter;
import org.astral.astrip.been.Project;
import org.astral.astrip.databinding.FragmentGalleryBinding;
import org.astral.astrip.ui.home.HomeFragment;

import java.util.ArrayList;
import java.util.List;

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

        // 从 SharedPreferences 中读取项目列表
        loadProjectsFromSharedPreferences();

        return root;
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

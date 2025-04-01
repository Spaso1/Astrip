package org.astral.astrip.adapter;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.astral.astrip.R;
import org.astral.astrip.been.Project;
import org.astral.astrip.been.Pointer;

import java.util.*;

public class ProjectReAdapter extends RecyclerView.Adapter<ProjectReAdapter.ProjectViewHolder> {

    private List<Project> projectList;
    private OnItemClickListener listener;

    public ProjectReAdapter() {
        this.projectList = new ArrayList<>();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setProjectList(List<Project> projectList) {
        this.projectList = projectList;
        Log.d("ProjectReAdapter", "setProjectList: " + projectList.size());
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_re_project, parent, false);
        return new ProjectViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        Project project = projectList.get(position);
        Log.d("ProjectReAdapter", "onBindViewHolder: " + project.getName());
        holder.projectName.setText(project.getName());
        holder.pay.setText("花费: " + calculateTotalPay(project));
        holder.sumLong.setText("总长 : " + String.valueOf(calculateTotalDistance(project)).split("\\.")[0] + "千米");

        // 设置点击监听器
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(project);
            }
        });
    }

    @Override
    public int getItemCount() {
        return projectList.size();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public interface OnItemClickListener {
        void onItemClick(Project project);
    }

    static class ProjectViewHolder extends RecyclerView.ViewHolder {
        ImageView backgroundLayout;
        TextView projectName;
        TextView pay;
        TextView sumLong;

        ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            backgroundLayout = itemView.findViewById(R.id.backgroundLayout);
            projectName = itemView.findViewById(R.id.projectName);
            pay = itemView.findViewById(R.id.pay);
            sumLong = itemView.findViewById(R.id.sumLong);
        }
    }

    private int calculateTotalPay(Project project) {
        int totalPay = 0;
        Map<String, Pointer> paths = project.getPaths();
        if (paths != null) {
            for (Pointer pointer : paths.values()) {
                totalPay += pointer.getPay();
            }
        }
        return totalPay;
    }

    private double calculateTotalDistance(Project project) {
        double totalDistance = 0.0;
        Map<String, Pointer> paths = project.getPaths();
        if (paths != null && paths.size() > 1) {
            List<Pointer> sortedPointers = new ArrayList<>(paths.values());
            Collections.sort(sortedPointers, Comparator.comparingInt(Pointer::getPointId));

            for (int i = 0; i < sortedPointers.size() - 1; i++) {
                Pointer current = sortedPointers.get(i);
                Pointer next = sortedPointers.get(i + 1);
                totalDistance += distance(current.getX(), current.getY(), next.getX(), next.getY());
            }
        }
        return totalDistance;
    }

    private double distance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // 地球半径，单位为千米
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}

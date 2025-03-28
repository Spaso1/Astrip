package org.astral.astrip.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.astral.astrip.been.Pointer;
import org.astral.astrip.been.Project;
import org.astral.astrip.R;

import java.util.List;

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder> {

    private List<Project> projects;

    public ProjectAdapter(List<Project> projects) {
        this.projects = projects;
    }

    public void setProjects(List<Project> projects) {
        this.projects = projects;
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_project, parent, false);
        return new ProjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        Project project = projects.get(position);
        holder.nameTextView.setText(project.getName());
        int pay = 0;

        for (Pointer pointer : project.getPaths().values()) {
            pay += pointer.getPay();
        }

        holder.costTextView.setText("花期 " + pay + "");
        holder.startDateTextView.setText("起始 " +project.getDate_start());
        holder.endDateTextView.setText("终点 " +project.getDate_end());
    }

    @Override
    public int getItemCount() {
        return projects.size();
    }

    static class ProjectViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView costTextView;
        TextView startDateTextView;
        TextView endDateTextView;

        ProjectViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            costTextView = itemView.findViewById(R.id.costTextView);
            startDateTextView = itemView.findViewById(R.id.startDateTextView);
            endDateTextView = itemView.findViewById(R.id.endDateTextView);
        }
    }
}

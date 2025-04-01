package org.astral.astrip.adapter;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.astral.astrip.R;
import org.astral.astrip.been.Pointer;
import org.astral.astrip.been.Project;

import java.text.SimpleDateFormat;
import java.util.*;

public class PointReAdapter extends RecyclerView.Adapter<PointReAdapter.PointerViewHolder> {
    private String nowdate = "0";
    private List<Pointer> projectList;
    private Map<Pointer,Integer[]> map = new HashMap<>();
    private OnItemClickListener listener;
    private int[] color = new int[3];
    public PointReAdapter() {
        this.projectList = new ArrayList<>();
    }
    private int id = 1;
    @SuppressLint("NotifyDataSetChanged")
    public void setProjectList(List<Pointer> projectList) {
        //按照id排序
        projectList.sort(Comparator.comparingInt(Pointer::getPointId));
        this.projectList = projectList;
        for (Pointer pointer : projectList) {
            String time = pointer.getTime();
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
                map.put(pointer,new Integer[]{color[0], color[1], color[2]});
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
                map.put(pointer,new Integer[]{color[0], color[1], color[2]});
            }
        }
        Log.d("PointerReAdapter", "setPointerList: " + projectList.size());
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PointerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_re_pointer, parent, false);
        return new PointerViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull PointerViewHolder holder, int position) {
        Pointer project = projectList.get(position);
        Log.d("ProjectReAdapter", "onBindViewHolder: " + project.getPointName());
        // 设置点击监听器
        Paint textPaint = new Paint();
        String time = project.getTime();
        holder.pontName.setText("Id : 1   " + project.getPointName());
        holder.time.setText( time.substring(0, 10));
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(project);
            }
        });
        holder.pontName.setTextColor(Color.rgb(Objects.requireNonNull(map.get(project))[0], Objects.requireNonNull(map.get(project))[1], Objects.requireNonNull(map.get(project))[2]));
    }

    @Override
    public int getItemCount() {
        return projectList.size();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public interface OnItemClickListener {
        void onItemClick(Pointer project);
    }

    static class PointerViewHolder extends RecyclerView.ViewHolder {
        TextView pontName;
        TextView time;

        PointerViewHolder(@NonNull View itemView) {
            super(itemView);
            pontName = itemView.findViewById(R.id.pointName);
            time = itemView.findViewById(R.id.time);
        }
    }
}

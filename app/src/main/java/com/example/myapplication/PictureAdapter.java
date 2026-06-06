package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class PictureAdapter extends RecyclerView.Adapter<PictureAdapter.ViewHolder> {

    private List<Integer> mData; // This is the name we will use
    private List<Boolean> mGuessedStatus; // Track if each item is guessed
    private int selectedPosition = RecyclerView.NO_POSITION;

    public PictureAdapter(List<Integer> pictureList, List<Boolean> guessedStatus) {
        // We take the incoming 'pictureList' and save it into our internal 'mData'
        this.mData = pictureList;
        this.mGuessedStatus = guessedStatus;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_logos_to_guess, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Use mData here
        int imageResId = mData.get(position);
        boolean isGuessed = (mGuessedStatus != null && position < mGuessedStatus.size()) ? mGuessedStatus.get(position) : false;

        holder.imageView.setImageResource(imageResId);
        holder.itemView.setSelected(selectedPosition == position);

        // Make untouchable if guessed
        if (isGuessed) {
            holder.itemView.setAlpha(0.7f); // Optional: dim it slightly to show it's "inactive"
            holder.itemView.setOnClickListener(null);
            holder.itemView.setClickable(false);
            holder.itemView.setFocusable(false);
        } else {
            holder.itemView.setAlpha(1.0f);
            holder.itemView.setClickable(true);
            holder.itemView.setFocusable(true);
            holder.itemView.setOnClickListener(v -> {
                int previousPosition = selectedPosition;
                selectedPosition = holder.getAdapterPosition();
                notifyItemChanged(previousPosition);
                notifyItemChanged(selectedPosition);

                Context context = v.getContext();
                try {
                    Intent intent = new Intent(context, GuessActivity.class);
                    intent.putExtra("SELECTED_IMAGE", imageResId);
                    context.startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(context, "Could not open the game. Try again!", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        // Use mData here too!
        return (mData != null) ? mData.size() : 0;
    }

    public void updateData(List<Integer> newList, List<Boolean> newStatus) {
        this.mData = newList;
        this.mGuessedStatus = newStatus;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        public ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
        }
    }
}
package com.jtms.chromecasttest.view;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.jtms.chromecasttest.R;
import com.jtms.chromecasttest.musicPlayer.TrackMediaInfo;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Created by jochemtoolenaar on 11/02/15.
 */
public class PlaylistItemAdapter extends RecyclerView.Adapter<PlaylistItemAdapter.ItemViewHolder> {



    //region properties
    private ArrayList<TrackMediaInfo> tracks;
    private MainActivity ctx;
    //endregion

    public PlaylistItemAdapter(ArrayList<TrackMediaInfo> tracks, MainActivity ctx){
        this.tracks = tracks;
        this.ctx = ctx;
    }


    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {


        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.listitem_soundcloud, parent, false);


        return new ItemViewHolder(v,ctx);
    }

    @Override
    public void onBindViewHolder(ItemViewHolder holder, int position) {
        TrackMediaInfo track = tracks.get(position);

        holder.artistName.setText(track.artistName);
        holder.trackTitle.setText(track.title);
        holder.index = position;

        String uri = track.artworkImageUri;
        if(!uri.isEmpty()){
            Picasso.with(ctx).load(uri).resize(100,100).centerCrop().into(holder.thumbnailImage);
        }


    }

    @Override
    public int getItemCount() {
        return tracks == null ? 0 : tracks.size();
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        // each data item is just a string in this case
        public int index;
        public ImageView thumbnailImage;
        public TextView artistName;
        public TextView trackTitle;
        MainActivity activity;
        public ItemViewHolder(View v, MainActivity activity) {
            super(v);
            this.activity = activity;
            thumbnailImage = (ImageView)v.findViewById(R.id.imageView_trackThumb);
            artistName = (TextView)v.findViewById(R.id.textView_artistName);
            trackTitle = (TextView)v.findViewById(R.id.textView_trackTitle);
            v.setOnClickListener(this);

        }

        @Override
        public void onClick(View v) {
            if(activity != null && activity.getMusicPlayer() != null){
                if(activity.getMusicPlayer().getPlaylist(false) == null){
                    activity.getMusicPlayer().startPlaylist(activity.getPlaylist(),index);
                }else{
                    activity.getMusicPlayer().startPlayingAtIndex(index);
                }
            }
        }
    }
}

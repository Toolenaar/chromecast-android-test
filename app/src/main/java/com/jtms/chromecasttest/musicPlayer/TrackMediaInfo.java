package com.jtms.chromecasttest.musicPlayer;

import android.net.Uri;

import com.google.android.gms.common.images.WebImage;

/**
 * Created by jochem on 2/10/2015.
 */
public class TrackMediaInfo {
    public String title;
    public WebImage artworkImage;
    public String artworkImageUri;
    public String artistName;
    public String streamUri;

    public TrackMediaInfo(String title,String artistName,String artworkImageUri,String streamUri){
        this.title = title;
        this.artistName = artistName;
        this.artworkImageUri = artworkImageUri;
        this.streamUri = streamUri;
        artworkImage = new WebImage(Uri.parse(artworkImageUri),500,500);
    }

}

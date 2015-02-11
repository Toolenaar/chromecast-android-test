package com.jtms.chromecasttest.musicPlayer;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.KeyEvent;

import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.RemoteMediaPlayer;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.jtms.chromecasttest.ChromeCastController;

/**
 * Created by jochem toolenaar on 11/02/15.
 *
 * extends the music player if connected to a chromecast handles playback events on the chromecast instead of phone
 *
 */
public class ChromecastMusicPlayer extends MusicPlayer {


    private static final String TAG = "ChromecastMusicPlayer";
    private static final double VOLUME_INCREMENT = 0.1;

    //region fields & properties
    public boolean isConnectedToChromecast(){
        return (!(controller == null || controller.getApiClient() == null)) && controller.getApiClient().isConnected();
    }
    private ChromeCastController controller;
    private RemoteMediaPlayer remoteMediaPlayer;
    //endregion

    public ChromecastMusicPlayer(Context ctx,ChromeCastController controller) {
        super(ctx);
        this.controller = controller;
    }



    @Override
    public void startPlayingAtIndex(int index) {
        if(isConnectedToChromecast()){
            setupRemoteMediaPlayer(getPlaylist(true).get(index));
        }else{
            super.startPlayingAtIndex(index);
        }
    }

    public void pauseLocal(){
        super.pause();
    }
    @Override
    public void pause() {
        if(isConnectedToChromecast()) {
            pauseChromecast();
        }else{
            super.pause();
        }


    }

    @Override
    public void play() {

        if(isConnectedToChromecast()){
            playChromecast();
        }else{
            super.play();
        }

    }

    private void playChromecast(){
        if(remoteMediaPlayer != null) {
            remoteMediaPlayer.play(controller.getApiClient()).setResultCallback(
                    new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
                        @Override
                        public void onResult(RemoteMediaPlayer.MediaChannelResult result) {
                            Status status = result.getStatus();
                            if (!status.isSuccess()) {
                                Log.w("MainActivity", "Unable to toggle pause: "
                                        + status.getStatusCode());
                            }
                        }
                    });
        }
    }
    /**
     * pauses the playback on tv
     */
    private void pauseChromecast(){
        if(remoteMediaPlayer != null){
            remoteMediaPlayer.pause(controller.getApiClient()).setResultCallback(
                    new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
                        @Override
                        public void onResult(RemoteMediaPlayer.MediaChannelResult result) {
                            Status status = result.getStatus();
                            if (!status.isSuccess()) {
                                Log.w("MainActivity", "Unable to toggle pause: "
                                        + status.getStatusCode());
                            }
                        }
                    });
        }

    }


    public void setupRemoteMediaPlayer(TrackMediaInfo info){

        remoteMediaPlayer = new RemoteMediaPlayer();
        remoteMediaPlayer.setOnStatusUpdatedListener(
                new RemoteMediaPlayer.OnStatusUpdatedListener() {
                    @Override
                    public void onStatusUpdated() {
                        MediaStatus mediaStatus = remoteMediaPlayer.getMediaStatus();
                        // boolean isPlaying = mediaStatus.getPlayerState() ==
                        //     MediaStatus.PLAYER_STATE_PLAYING;

                    }
                });

        remoteMediaPlayer.setOnMetadataUpdatedListener(
                new RemoteMediaPlayer.OnMetadataUpdatedListener() {
                    @Override
                    public void onMetadataUpdated() {
                        MediaInfo mediaInfo = remoteMediaPlayer.getMediaInfo();
                        // MediaMetadata metadata = mediaInfo.getMetadata();

                    }
                });



        controller.launchChannelWithMedia(remoteMediaPlayer, ChromeCastController.createTrackMediaInfo(info,
                "audio/mp3", MediaMetadata.MEDIA_TYPE_MUSIC_TRACK));
    }

    public boolean handleSystemVolume(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (action == KeyEvent.ACTION_DOWN) {
                    if (remoteMediaPlayer != null) {
                        double currentVolume = Cast.CastApi.getVolume(controller.getApiClient());
                        if (currentVolume < 1.0) {
                            try {
                                Cast.CastApi.setVolume(controller.getApiClient(),
                                        Math.min(currentVolume + VOLUME_INCREMENT, 1.0));
                            } catch (Exception e) {
                                Log.e(TAG, "unable to set volume", e);
                            }
                        }
                    } else {
                        Log.e(TAG, "dispatchKeyEvent - volume up");
                    }
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_DOWN) {
                    if (remoteMediaPlayer != null) {
                        double currentVolume = Cast.CastApi.getVolume(controller.getApiClient());
                        if (currentVolume > 0.0) {
                            try {
                                Cast.CastApi.setVolume(controller.getApiClient(),
                                        Math.max(currentVolume - VOLUME_INCREMENT, 0.0));
                            } catch (Exception e) {
                                Log.e(TAG, "unable to set volume", e);
                            }
                        }
                    } else {
                        Log.e(TAG, "dispatchKeyEvent - volume down");
                    }
                }
                return true;
            default:
                return false;
        }
    }
}

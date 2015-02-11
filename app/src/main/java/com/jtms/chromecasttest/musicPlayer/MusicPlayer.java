package com.jtms.chromecasttest.musicPlayer;


import android.content.Context;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.widget.Toast;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 * Created by jochemtoolenaar on 19/12/14.
 */
public class MusicPlayer implements MediaPlayer.OnErrorListener,AudioManager.OnAudioFocusChangeListener  {


    //region getters & setters

    public boolean isShuffle() {
        return shuffle;
    }

    public boolean isRepeat() {
        return repeat;
    }

    public TrackMediaInfo getCurrentTrack() {
        return currentTrack;
    }
    public int getCurrentProgress(){
        return mediaPlayer == null ? 0 :mediaPlayer.getCurrentPosition();
    }

    public boolean isPlaying(){
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public ArrayList<TrackMediaInfo> getPlaylist(boolean ignoreShuffle) {
        if(shuffle && !ignoreShuffle){
            if(shuffleList == null){
                setShuffledList();
            }
            return shuffleList;
        }
        return playlist;
    }
    public boolean isStarted(){return  hasStarted;}


    //endregion

    //region
    private boolean isLoading;
    private MediaPlayer mediaPlayer;
    private boolean hasStarted;
    private boolean shuffle;
    private boolean repeat;
    private Context ctx;
    private ArrayList<TrackMediaInfo> playlist;
    private ArrayList<TrackMediaInfo> shuffleList;
    private TrackMediaInfo currentTrack;
    private WifiManager.WifiLock wifiLock;
    private int currentIndex;
    private ArrayList<MusicPlaybackChangedListener> listeners;
    //endregion

    public MusicPlayer(Context ctx){
        listeners = new ArrayList<>();
        this.ctx = ctx;

    }

    public void startPlaylist(ArrayList<TrackMediaInfo> playlist,int startIndex) {
        this.playlist = playlist;
        setShuffledList();
        startPlayingAtIndex(startIndex);

    }


    public void startPlayingAtIndex(int index){
        isLoading = true;


        AudioManager am = (AudioManager)ctx.getSystemService(Context.AUDIO_SERVICE);
        int result = am.requestAudioFocus(this,
                // Use the music stream.
                AudioManager.STREAM_MUSIC,
                // Request permanent focus.
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            currentIndex = index;
            currentTrack = getPlaylist(false).get(index);
            hasStarted = true;

            if(mediaPlayer != null){
                dispose();
            }
            //set up the media player and setup a wifi lock so wifi stays on whle streaming music

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setWakeMode(ctx, PowerManager.PARTIAL_WAKE_LOCK);
            wifiLock = ((WifiManager) ctx.getSystemService(Context.WIFI_SERVICE))
                    .createWifiLock(WifiManager.WIFI_MODE_FULL, "musicPlayerLock");

            wifiLock.acquire();


            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

            try {
                mediaPlayer.setDataSource(currentTrack.streamUri);
                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        isLoading = false;
                        mediaPlayer.start();
                        if(listeners != null && listeners.size() != 0){
                            for(MusicPlaybackChangedListener listener : listeners){
                                listener.onMusicStartedPlaying();
                            }

                        }
                    }
                });
                mediaPlayer.prepareAsync();
                if(listeners != null && listeners.size() != 0){
                    for(MusicPlaybackChangedListener listener : listeners){
                        listener.onTrackChanged(currentTrack);
                    }

                }
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        if(currentIndex == getPlaylist(false).size() - 1 && !repeat){

                        }else{
                            skipNext();
                        }
                    }
                });


            } catch (IOException e) {
                e.printStackTrace();
                isLoading = false;
                //notify user something has gone wrong
                showErrorToast();
            }
        }



    }
    public void setRepeat(boolean repeat){
        this.repeat = repeat;
    }
    public void setShuffle(boolean shuffleActive){
        shuffle = shuffleActive;

    }
    public void setShuffledList(){
        long seed = System.nanoTime();
        shuffleList = new ArrayList<>(playlist);
        Collections.shuffle(shuffleList, new Random(seed));
        Collections.shuffle(shuffleList, new Random(seed));
    }

    public void play(){

        if(!isPlaying() && !isLoading){
            if(mediaPlayer != null){
                mediaPlayer.start();
                if(listeners != null && listeners.size() != 0){
                    for(MusicPlaybackChangedListener listener : listeners){
                        listener.onMusicStartedPlaying();
                    }

                }
            }
        }
    }

    public void pause(){
        if(isPlaying() && !isLoading){
            if(mediaPlayer != null){
                mediaPlayer.pause();
                if(listeners != null && listeners.size() != 0){
                    for(MusicPlaybackChangedListener listener : listeners){
                        listener.onMusicPaused();
                    }

                }
            }
        }
    }

    public void stop(){
        dispose();
    }

    public void skipNext(){

        if(getPlaylist(false) == null || getPlaylist(false).size() == 0)return;

        int indexToPlay = 0;
        if(currentIndex < getPlaylist(false).size() -1){
            indexToPlay = currentIndex +=1;
        }
        startPlayingAtIndex(indexToPlay);
    }

    public void skipPrevious(){

       int indexToPlay = getPlaylist(false).size() -1;
       if(currentIndex > 0){
           indexToPlay = currentIndex -=1;
       }
       startPlayingAtIndex(indexToPlay);
    }

    public void seekTo(int msec){

        if(mediaPlayer != null && isPlaying())mediaPlayer.seekTo(msec);
    }

    public void dispose(){
        if(mediaPlayer != null){

            mediaPlayer.stop();
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        if (wifiLock != null && wifiLock.isHeld()){
            wifiLock.release();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        dispose();
        showErrorToast();

        return false;
    }

    private void showErrorToast(){
        Toast toast = Toast.makeText(ctx, "something when wrong tyring to play the track", Toast.LENGTH_LONG);
        toast.show();
    }

    /**
     * listen to playback changes
     * @param listener
     */
    public void addMusicPlaybackChangedListener(MusicPlaybackChangedListener listener){
        listeners.add(listener);
    }

    public void removeMusicPlaybackChangedListener(MusicPlaybackChangedListener listener){
        if(listeners.contains(listener))listeners.remove(listener);
    }

    public interface MusicPlaybackChangedListener{
        void onMusicPaused();
        void onMusicStartedPlaying();
        void onTrackChanged(TrackMediaInfo update);
    }

    /**
     * listen to audio focus changes from the system and respond accordingly
     * @param focusChange
     */
    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {

            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                if (isPlaying()){
                    mediaPlayer.setVolume(1.0f, 1.0f);

                }


                break;

            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                pause();
                dispose();
              /*  if(((SleeveApplication)ctx).isMusicPlayerServiceActive()){
                    PendingIntent pendingIntent;
                    Intent intent;

                    intent = new Intent(MusicPlayerService.ACTION_STOP);
                    pendingIntent = PendingIntent.getService(ctx,
                            MusicPlayerService.REQUEST_CODE_PLAYER, intent,
                            PendingIntent.FLAG_UPDATE_CURRENT);
                    try {
                        pendingIntent.send();
                    } catch (PendingIntent.CanceledException e) {
                        e.printStackTrace();
                    }
                }*/

                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume

                pause();
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (isPlaying()) mediaPlayer.setVolume(0.1f, 0.1f);
                break;
        }
    }
}

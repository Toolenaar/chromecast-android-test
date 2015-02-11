package com.jtms.chromecasttest;

import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.RemoteMediaPlayer;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.images.WebImage;

import java.net.URI;


public class MainActivity extends ActionBarActivity {


    private ChromeCastController castController;
    private RemoteMediaPlayer mRemoteMediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
       castController = new ChromeCastController(this,"827C9309");
        castController.setOnConnectionListener(new ChromeCastController.OnConnectedToDeviceListener() {
            @Override
            public void onConnectionSucces() {
                //start media playback
                //onnce there is succes setup the mediaplayer and start playing
                setupRemoteMediaPlayer();
            }
        });

    }
    public void setupRemoteMediaPlayer(){
        //todo create a seperate mediaplayer class and integrate with local media player
        mRemoteMediaPlayer = new RemoteMediaPlayer();
        mRemoteMediaPlayer.setOnStatusUpdatedListener(
                new RemoteMediaPlayer.OnStatusUpdatedListener() {
                    @Override
                    public void onStatusUpdated() {
                        MediaStatus mediaStatus = mRemoteMediaPlayer.getMediaStatus();
                       // boolean isPlaying = mediaStatus.getPlayerState() ==
                           //     MediaStatus.PLAYER_STATE_PLAYING;

                    }
                });

        mRemoteMediaPlayer.setOnMetadataUpdatedListener(
                new RemoteMediaPlayer.OnMetadataUpdatedListener() {
                    @Override
                    public void onMetadataUpdated() {
                        MediaInfo mediaInfo = mRemoteMediaPlayer.getMediaInfo();
                       // MediaMetadata metadata = mediaInfo.getMetadata();

                    }
                });

        TrackMediaInfo info = new TrackMediaInfo();
        info.artistName = "Alex cruz";
        info.title = "Deep Sexy Podcast #15 - November 2014";
        info.streamUri = "https://api.soundcloud.com/tracks/177437558/stream?client_id=5be8a5639583c700d021ac61bd06437d";
        info.artworkImage = new WebImage(Uri.parse("https://i1.sndcdn.com/artworks-000097503590-cjca4c-t500x500.jpg"),500,500);

        castController.launchChannelWithMedia(mRemoteMediaPlayer, ChromeCastController.createTrackMediaInfo(info,
                "audio/mp3", MediaMetadata.MEDIA_TYPE_MUSIC_TRACK));
    }

    /**
     * pauses the playback on tv
     */
    private void pausePlayback(){
        mRemoteMediaPlayer.pause(castController.getApiClient()).setResultCallback(
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


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        castController.setupActionbarMenuItem(menu);
        return true;
    }


    @Override
    protected void onResume() {
        super.onResume();
        castController.addCallback();
    }

    @Override
    protected void onPause() {
        if (isFinishing()) {
            castController.removeCallback();
        }
        super.onPause();
    }
    @Override
    protected void onStart() {
        super.onStart();
        castController.addCallback();
    }

    @Override
    protected void onStop() {
        castController.removeCallback();
        super.onStop();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
    }
}

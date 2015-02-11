package com.jtms.chromecasttest.view;

import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;

import com.google.android.gms.cast.RemoteMediaPlayer;

import com.jtms.chromecasttest.ChromeCastController;
import com.jtms.chromecasttest.R;
import com.jtms.chromecasttest.musicPlayer.ChromecastMusicPlayer;
import com.jtms.chromecasttest.musicPlayer.TrackMediaInfo;

import java.util.ArrayList;


public class MainActivity extends ActionBarActivity {


    private ChromeCastController castController;
    private RemoteMediaPlayer mRemoteMediaPlayer;
    private MainFragment fragment;
    private ChromecastMusicPlayer musicPlayer;

    public ChromecastMusicPlayer getMusicPlayer() {
        if(musicPlayer == null){
            musicPlayer = new ChromecastMusicPlayer(this,castController);
        }
        return musicPlayer;
    }
    private ArrayList<TrackMediaInfo> playlist;
    public ArrayList<TrackMediaInfo> getPlaylist(){
        if(playlist == null){
            playlist = new ArrayList<>();

            TrackMediaInfo info = new TrackMediaInfo("Deep Sexy Podcast #15 - November 2014","Alex cruz",
                    "https://i1.sndcdn.com/artworks-000097503590-cjca4c-t500x500.jpg",
                    "https://api.soundcloud.com/tracks/177437558/stream?client_id=5be8a5639583c700d021ac61bd06437d");

            TrackMediaInfo info2 = new TrackMediaInfo("DJ Mix Number 03","Boris Brejcha",
                    "https://i1.sndcdn.com/artworks-000100866077-r5ba1f-t500x500.jpg",
                    "https://api.soundcloud.com/tracks/182450133/stream?client_id=5be8a5639583c700d021ac61bd06437d");


            playlist.add(info);
            playlist.add(info2);
        }
        return playlist;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {

            fragment = new MainFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container,fragment)
                    .commit();
        }
        castController = new ChromeCastController(this,"827C9309");
        musicPlayer = new ChromecastMusicPlayer(this,castController);


        castController.setOnConnectionListener(new ChromeCastController.OnConnectedToDeviceListener() {
            @Override
            public void onConnectionSucces() {
                //start media playback
                //once there is success setup the mediaplayer and start playing

               // fragment.switchLoadingView(false);

                //pause playback on device and start playing on chromecast
                musicPlayer.pauseLocal();
                int index = musicPlayer.getCurrentTrack() != null ? musicPlayer.getPlaylist(false).indexOf(musicPlayer.getCurrentTrack()) : 0;
                musicPlayer.startPlaylist(getPlaylist(),index);
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
    public boolean dispatchKeyEvent(KeyEvent event) {

        boolean handled =   musicPlayer.handleSystemVolume(event);
        if(!handled)super.dispatchKeyEvent(event);

        return handled;
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
        }else if(id == R.id.media_route_menu_item){
            fragment.switchLoadingView(true);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class MainFragment extends Fragment {

        private Button start;
        private ProgressBar loading;

        public MainFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return setupview(rootView);
        }

        public void switchLoadingView(boolean isLoading){
            start.setVisibility(isLoading ? View.GONE : View.VISIBLE);
            loading.setVisibility(isLoading ? View.VISIBLE : View.GONE);

        }

        private View setupview(View root){



            final ChromecastMusicPlayer player = ((MainActivity)getActivity()).getMusicPlayer();
            final Button play =(Button)root.findViewById(R.id.button_play);

            final Button pause =(Button)root.findViewById(R.id.button_pause);
            final Button next =(Button)root.findViewById(R.id.button_next);
            final Button previous = (Button)root.findViewById(R.id.button_previous);


            View.OnClickListener listener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(v == play){
                        player.play();
                    }else if(v == pause){
                        player.pause();
                    }else if(v == next){
                        player.skipNext();
                    }else if(v == previous){
                        player.skipPrevious();
                    }

                }
            };

            play.setOnClickListener(listener);
            pause.setOnClickListener(listener);
            next.setOnClickListener(listener);
            previous.setOnClickListener(listener);



            RecyclerView recyclerView = (RecyclerView)root.findViewById(R.id.recyclerView_list);

            // use a linear layout manager
            LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
            recyclerView.setLayoutManager(layoutManager);


            PlaylistItemAdapter adapter = new PlaylistItemAdapter(((MainActivity)getActivity()).getPlaylist(),(MainActivity)getActivity());
            recyclerView.setAdapter(adapter);


            loading = (ProgressBar)root.findViewById(R.id.progressBar);

            return root;
        }
    }
}

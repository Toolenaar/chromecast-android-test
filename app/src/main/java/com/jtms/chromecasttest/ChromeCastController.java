package com.jtms.chromecasttest;

import android.content.Context;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.RemoteMediaPlayer;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.images.WebImage;
import com.jtms.chromecasttest.channels.BasicChannel;

import java.io.IOException;

/**
 * Created by jochem on 2/10/2015.
 */
public class ChromeCastController {

    private String sessionId;
    private String applicationStatus;
    private boolean wasLaunched;

    public GoogleApiClient getApiClient() {
        return mApiClient;
    }

    private static final String TAG = "ChromeCastController";
    private final MediaRouter mMediaRouter;
    private final MediaRouteSelector mMediaRouteSelector;
    //region properties
    Context ctx;
    String appId;
    private CastDevice mSelectedDevice;
    private CastRouterCallback callback;
    private GoogleApiClient mApiClient;
    private boolean mWaitingForReconnect;
    private ConnectionCallbacks mConnectionCallbacks;
    private Cast.Listener mCastClientListener;
    private boolean mApplicationStarted;
    private BasicChannel messageChannel;
    private OnConnectedToDeviceListener listener;
    private ConnectionFailedListener mConnectionFailedListener;
    //endregion

    /**
     * create the controller and initialize the media router
     * @param ctx
     * @param appId
     */
    public ChromeCastController(Context ctx, String appId){
        this.ctx = ctx;
        this.appId = appId;
        mMediaRouter = MediaRouter.getInstance(ctx.getApplicationContext());
        mMediaRouteSelector = new MediaRouteSelector.Builder()
                .addControlCategory(CastMediaControlIntent.categoryForCast(appId))
                .build();
    }

    /**
     * hooks up the actionbar item to the media provider
     * @param menu
     */
    public void setupActionbarMenuItem(Menu menu){
        MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
        MediaRouteActionProvider mediaRouteActionProvider =
                (MediaRouteActionProvider) MenuItemCompat.getActionProvider(mediaRouteMenuItem);
        mediaRouteActionProvider.setRouteSelector(mMediaRouteSelector);
    }

    /**
     * add a callback to listen to selected devices
     */
    public void addCallback(){
        callback = new CastRouterCallback();

        mMediaRouter.addCallback(mMediaRouteSelector,callback,
                MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
    }

    /**
     * remove the callback to listen to selected devices
     */
    public void removeCallback(){
      mMediaRouter.removeCallback(callback);
    }

    public void connectToApi(){

        mCastClientListener = new Cast.Listener() {
            @Override
            public void onApplicationStatusChanged() {
                if (mApiClient != null) {
                    Log.d(TAG, "onApplicationStatusChanged: "
                            + Cast.CastApi.getApplicationStatus(mApiClient));
                }
            }

            @Override
            public void onVolumeChanged() {
                if (mApiClient != null) {
                    Log.d(TAG, "onVolumeChanged: " + Cast.CastApi.getVolume(mApiClient));
                }
            }

            @Override
            public void onApplicationDisconnected(int errorCode) {
                teardown();
            }
        };

        Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions
                .builder(mSelectedDevice, mCastClientListener);

        mConnectionCallbacks = new ConnectionCallbacks();


        mConnectionFailedListener = new ConnectionFailedListener();

        mApiClient = new GoogleApiClient.Builder(ctx)
                .addApi(Cast.API, apiOptionsBuilder.build())
                .addConnectionCallbacks(mConnectionCallbacks)
                .addOnConnectionFailedListener(mConnectionFailedListener)
                .build();

        mApiClient.connect();

    }
    private void loadMedia(final RemoteMediaPlayer mediaPlayer, final MediaInfo mediaInfo){
        try {
            mediaPlayer.load(mApiClient, mediaInfo, true)
                    .setResultCallback(new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
                        @Override
                        public void onResult(RemoteMediaPlayer.MediaChannelResult result) {
                            if (result.getStatus().isSuccess()) {
                                Log.d(TAG, "Media loaded successfully");

                            }
                        }
                    });
        } catch (IllegalStateException e) {
            Log.e(TAG, "Problem occurred with media during loading", e);
        } catch (Exception e) {
            Log.e(TAG, "Problem opening media during loading", e);
        }
    }

    public static MediaInfo createTrackMediaInfo(TrackMediaInfo track, String contentType, int mediaMetadatatype){
        //video/mp4
        //
        MediaMetadata mediaMetadata = new MediaMetadata(mediaMetadatatype);
        mediaMetadata.putString(MediaMetadata.KEY_TITLE, track.title);
        mediaMetadata.putString(MediaMetadata.KEY_ARTIST,track.artistName);
        if(track.artworkImage != null)mediaMetadata.addImage(track.artworkImage);

        return new MediaInfo.Builder(
                track.streamUri)
                .setContentType(contentType)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setMetadata(mediaMetadata)
                .build();
    }

    private void launchMediaChannel(final RemoteMediaPlayer mediaPlayer, final MediaInfo mediaInfo){
        try {
            Cast.CastApi.setMessageReceivedCallbacks(mApiClient,
                    mediaPlayer.getNamespace(), mediaPlayer);
        } catch (IOException e) {
            Log.e(TAG, "Exception while creating media channel", e);
        }
        mediaPlayer.requestStatus(mApiClient)
                .setResultCallback(
                        new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
                            @Override
                            public void onResult(RemoteMediaPlayer.MediaChannelResult result) {
                                if (!result.getStatus().isSuccess()) {
                                    Log.e(TAG, "Failed to request status --     " +result.getStatus().getStatusMessage());

                                }else{
                                    loadMedia(mediaPlayer,mediaInfo);
                                }


                            }
                        });
    }

    public void launchChannelWithMedia(final RemoteMediaPlayer mediaPlayer, final MediaInfo mediaInfo){
        //when ready start the app
        if(getApiClient().isConnected()) {


            Cast.CastApi.launchApplication(mApiClient, appId, false)
                    .setResultCallback(
                            new ResultCallback<Cast.ApplicationConnectionResult>() {
                                @Override
                                public void onResult(Cast.ApplicationConnectionResult result) {
                                    Status status = result.getStatus();
                                    if (status.isSuccess()) {
                                        ApplicationMetadata applicationMetadata =
                                                result.getApplicationMetadata();
                                        sessionId = result.getSessionId();
                                        applicationStatus = result.getApplicationStatus();
                                        wasLaunched = result.getWasLaunched();

                                        mApplicationStarted = true;
                                        launchMediaChannel(mediaPlayer, mediaInfo);

                                  /*   messageChannel = new BasicChannel();
                                    try {
                                        Cast.CastApi.setMessageReceivedCallbacks(mApiClient,
                                                messageChannel.getNamespace(),
                                                messageChannel);
                                    } catch (IOException e) {
                                        Log.e(TAG, "Exception while creating channel", e);
                                    }*/
                                    }
                                }
                            });
        }
    }

    private void sendMessage(String message) {
        if (mApiClient != null && messageChannel != null) {
            try {
                Cast.CastApi.sendMessage(mApiClient, messageChannel.getNamespace(), message)
                        .setResultCallback(
                                new ResultCallback<Status>() {
                                    @Override
                                    public void onResult(Status result) {
                                        if (!result.isSuccess()) {
                                            Log.e(TAG, "Sending message failed");
                                        }
                                    }
                                });
            } catch (Exception e) {
                Log.e(TAG, "Exception while sending message", e);
            }
        }
    }

    //region callbacks
   public void setOnConnectionListener(OnConnectedToDeviceListener listener){
        this.listener = listener;
    }
    public interface OnConnectedToDeviceListener{
        public void onConnectionSucces();
    }

    private class CastRouterCallback extends MediaRouter.Callback {

        @Override
        public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo info) {
            mSelectedDevice = CastDevice.getFromBundle(info.getExtras());
            String routeId = info.getId();
            connectToApi();
        }

        @Override
        public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo info) {
            teardown();
            mSelectedDevice = null;
        }
    }

    private class ConnectionCallbacks implements
            GoogleApiClient.ConnectionCallbacks {
        @Override
        public void onConnected(Bundle connectionHint) {
            if (mWaitingForReconnect) {
                mWaitingForReconnect = false;
                //reconnectChannels();
            } else {
               if(listener != null){
                   listener.onConnectionSucces();
               }
            }
        }

        @Override
        public void onConnectionSuspended(int cause) {
            mWaitingForReconnect = true;
        }
    }

    private class ConnectionFailedListener implements
            GoogleApiClient.OnConnectionFailedListener {
        @Override
        public void onConnectionFailed(ConnectionResult result) {
            teardown();
        }
    }

    //endregion

    //region error handling
    private void teardown() {
        Log.d(TAG, "teardown");
        if (mApiClient != null) {
            if (mApplicationStarted) {
                if (mApiClient.isConnected() || mApiClient.isConnecting()) {
                    try {
                        Cast.CastApi.stopApplication(mApiClient, sessionId);
                        if (messageChannel != null) {
                            Cast.CastApi.removeMessageReceivedCallbacks(
                                    mApiClient,
                                    messageChannel.getNamespace());
                            messageChannel = null;
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Exception while removing channel", e);
                    }
                    mApiClient.disconnect();
                }
                mApplicationStarted = false;
            }
            mApiClient = null;
        }
        mSelectedDevice = null;
        mWaitingForReconnect = false;
        sessionId = null;
    }
    //endregion

}

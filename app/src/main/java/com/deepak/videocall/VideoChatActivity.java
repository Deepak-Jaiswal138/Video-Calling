package com.deepak.videocall;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

import android.Manifest;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;

public class VideoChatActivity extends AppCompatActivity implements Session.SessionListener, PublisherKit.PublisherListener {

    private static String API_KEY="46721692";
    private static String SESSION_ID="2_MX40NjcyMTY5Mn5-MTYwNTc4NDI2Njc4M35LQzc1emthYUllWWROZUxxR3ZOWDBENjd-fg";
    private static String TOKEN="T1==cGFydG5lcl9pZD00NjcyMTY5MiZzaWc9ZDE3MDc1YmVlZWNlZWJiYjk2YzEzNGI1YWMwZDJhOTRhYjc1YzA2ODpzZXNzaW9uX2lkPTJfTVg0ME5qY3lNVFk1TW41LU1UWXdOVGM0TkRJMk5qYzRNMzVMUXpjMWVtdGhZVWxsV1dST1pVeHhSM1pPV0RCRU5qZC1mZyZjcmVhdGVfdGltZT0xNjA1Nzg0MzA0Jm5vbmNlPTAuNzIyNzQzODY3OTUzNzIyNCZyb2xlPXB1Ymxpc2hlciZleHBpcmVfdGltZT0xNjA1Nzg3ODY5JmluaXRpYWxfbGF5b3V0X2NsYXNzX2xpc3Q9";
    private static final String LOG_TAG=VideoChatActivity.class.getSimpleName();
    private static final int RC_VIDEO_APP_PERM=124;

    private FrameLayout mPublisherViewContainer;
    private FrameLayout mSubscriberViewContainer;
    private com.opentok.android.Session mSession;
    private Publisher mPublisher;
    private Subscriber mSubscriber;

    private ImageView closeVideoChatBtn;
    private DatabaseReference usersRef;
    private String userId="";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_chat);
        userId= FirebaseAuth.getInstance().getCurrentUser().getUid();
        usersRef= FirebaseDatabase.getInstance().getReference().child("Users");

        closeVideoChatBtn=findViewById(R.id.close_video_chat_btn);
        closeVideoChatBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               usersRef.addValueEventListener(new ValueEventListener() {
                   @Override
                   public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                       if (dataSnapshot.child(userId).hasChild("Ringing")){
                           usersRef.child(userId).child("Ringing").removeValue();
                           if (mPublisher!=null){
                               mPublisher.destroy();
                           }
                           if (mSubscriber!=null){
                               mSubscriber.destroy();
                           }
                           startActivity(new Intent(VideoChatActivity.this,RegistrationActivity.class));
                           finish();
                       }
                       if (dataSnapshot.child(userId).hasChild("Calling")){
                           usersRef.child(userId).child("Calling").removeValue();
                           if (mPublisher!=null){
                               mPublisher.destroy();
                           }
                           if (mSubscriber!=null){
                               mSubscriber.destroy();
                           }
                           startActivity(new Intent(VideoChatActivity.this,RegistrationActivity.class));
                           finish();
                       }
                       else {
                           if (mPublisher!=null){
                               mPublisher.destroy();
                           }
                           if (mSubscriber!=null){
                               mSubscriber.destroy();
                           }
                           startActivity(new Intent(VideoChatActivity.this,RegistrationActivity.class));
                           finish();
                       }
                   }

                   @Override
                   public void onCancelled(@NonNull DatabaseError databaseError) {

                   }
               });
            }
        });
        requestPermissions();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode,permissions,grantResults,VideoChatActivity.this);
    }
    @AfterPermissionGranted(RC_VIDEO_APP_PERM)
    private void requestPermissions(){
        String[] perms={Manifest.permission.INTERNET,Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO};
        if (EasyPermissions.hasPermissions(this,perms)){
            mPublisherViewContainer=findViewById(R.id.publisher_container);
            mSubscriberViewContainer=findViewById(R.id.subscriber_container);

            //initialise and connect
            mSession=new Session.Builder(this,API_KEY,SESSION_ID).build();
            mSession.setSessionListener(VideoChatActivity.this);
            mSession.connect(TOKEN);

        }
        else {
            EasyPermissions.requestPermissions(this,"Hey this app needs Mic and Camera Permission,please allow",RC_VIDEO_APP_PERM,perms);
        }
    }

    @Override
    public void onStreamCreated(PublisherKit publisherKit, Stream stream) {

    }

    @Override
    public void onStreamDestroyed(PublisherKit publisherKit, Stream stream) {

    }

    @Override
    public void onError(PublisherKit publisherKit, OpentokError opentokError) {

    }
    //2. publishing a stream to the session
    @Override
    public void onConnected(Session session) {
        Log.i(LOG_TAG,"session connected");
        mPublisher=new Publisher.Builder(this).build();
        mPublisher.setPublisherListener(VideoChatActivity.this);

        mPublisherViewContainer.addView(mPublisher.getView());
        if (mPublisher.getView() instanceof GLSurfaceView){
            ((GLSurfaceView) mPublisher.getView()).setZOrderOnTop(true);
        }
        mSession.publish(mPublisher);

    }

    @Override
    public void onDisconnected(Session session) {
         Log.i(LOG_TAG,"stream disconnected");
    }
    //3 subscribing to the streams
    @Override
    public void onStreamReceived(Session session, Stream stream) {

        Log.i(LOG_TAG,"stream received");
        if (mSubscriber==null){
            mSubscriber=new Subscriber.Builder(this,stream).build();
            mSession.subscribe(mSubscriber);
            mSubscriberViewContainer.addView(mSubscriber.getView());
        }

    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {
         Log.i(LOG_TAG,"stream has droped");
         if (mSubscriber!=null){
             mSubscriber=null;
             mSubscriberViewContainer.removeAllViews();
         }
    }

    @Override
    public void onError(Session session, OpentokError opentokError) {
         Log.i(LOG_TAG,"stream error");
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}

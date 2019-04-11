package com.example.mobileapp;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginBehavior;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.squareup.picasso.Picasso;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.Twitter;
import com.twitter.sdk.android.core.TwitterApiClient;
import com.twitter.sdk.android.core.TwitterAuthToken;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterAuthClient;
import com.twitter.sdk.android.core.identity.TwitterLoginButton;
import com.twitter.sdk.android.core.models.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;

public class MainActivity extends AppCompatActivity {


    private static final String TAG = MainActivity.class.getSimpleName();
    private TwitterLoginButton twitterLoginButton;
     Button twitter_logout_button;
     Button profile_image_button;

    private ImageView userProfileImageView;
    private TextView userDetailsLabel;


    //twitter auth client required for custom login
    private TwitterAuthClient client;


    private LoginButton loginButton;
    private CircleImageView circleImageView;
    private TextView txtName , txtEmail;

    private CallbackManager callbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loginButton = findViewById(R.id.login_button);
        txtName = findViewById(R.id.profile_name);
        txtEmail = findViewById(R.id.profile_email);
        circleImageView = findViewById(R.id.profile_pic);


        LoginManager.getInstance().setLoginBehavior(LoginBehavior.WEB_VIEW_ONLY);



        callbackManager = CallbackManager.Factory.create();
        loginButton.setReadPermissions(Arrays.asList("email","public_profile"));

        //initialize twitter auth client
        client = new TwitterAuthClient();

        //find the id of views
        twitterLoginButton = findViewById(R.id.default_twitter_login_button);

        //custom_twitter_login_button=findViewById(R.id.custom_twitter_login_button);

        profile_image_button= findViewById(R.id.profile_image_button);
        twitter_logout_button=findViewById(R.id.twitter_logout_button);


        userProfileImageView = findViewById(R.id.user_profile_image_view);
        userDetailsLabel = findViewById(R.id.user_details_label);

        //NOTE : calling default twitter login in OnCreate/OnResume to initialize twitter callback
        defaultLoginTwitter();

        checkLoginStatus();

        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {

            }

            @Override
            public void onCancel() {

            }

            @Override
            public void onError(FacebookException error) {

            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        callbackManager.onActivityResult(requestCode,resultCode,data);

        // Pass the activity result to the twitterAuthClient.
        if (client != null)
            client.onActivityResult(requestCode, resultCode, data);

        // Pass the activity result to the login button.
        twitterLoginButton.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    AccessTokenTracker tokenTracker = new AccessTokenTracker() {
        @Override
        protected void onCurrentAccessTokenChanged(AccessToken oldAccessToken, AccessToken currentAccessToken) {
            if(currentAccessToken==null)
            {
                txtName.setText("");
                txtEmail.setText("");
                circleImageView.setImageResource(0);
                twitterLoginButton.setVisibility(View.VISIBLE);
                //custom_twitter_login_button.setVisibility(View.VISIBLE);
               // profile_image_button.setVisibility(View.VISIBLE);
                userProfileImageView.setVisibility(View.VISIBLE);
                userDetailsLabel.setVisibility(View.VISIBLE);
                Toast.makeText(MainActivity.this, "User Logged Out", Toast.LENGTH_LONG).show();
            }
            else
            {
                loadUserProfile(currentAccessToken);
            }
        }
    };

    private void loadUserProfile(AccessToken newAccessToken)
    {
        GraphRequest request = GraphRequest.newMeRequest(newAccessToken, new GraphRequest.GraphJSONObjectCallback() {
            @Override
            public void onCompleted(JSONObject object, GraphResponse response) {

                try {
                    String first_name = object.getString("first_name");

                    String last_name = object.getString("last_name");

                    String email = object.getString("email");

                    String id = object.getString("id");

                    String image_url = "https://graph.facebook.com/"+id+"/picture?type=normal";



                    txtEmail.setText(email);
                    txtName.setText(first_name +" "+last_name);
                    RequestOptions requestOptions = new RequestOptions();
                    requestOptions.dontAnimate();

                    Glide.with(MainActivity.this).load(image_url).into(circleImageView);
                    //custom_twitter_login_button.setVisibility(View.INVISIBLE);
                    profile_image_button.setVisibility(View.INVISIBLE);
                    twitterLoginButton.setVisibility(View.INVISIBLE);
                    userDetailsLabel.setVisibility(View.INVISIBLE);
                    userProfileImageView.setVisibility(View.INVISIBLE);



                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        });

        Bundle parameters = new Bundle();
        parameters.putString("fields","first_name,last_name,email,id");
        request.setParameters(parameters);
        request.executeAsync();

    }

    private void checkLoginStatus()
    {
        if(AccessToken.getCurrentAccessToken()!=null)
        {
            loadUserProfile(AccessToken.getCurrentAccessToken());
        }
    }

    /**
     * method to do Default Twitter Login
     */
    public void defaultLoginTwitter() {

        //check if user is already authenticated or not
        //if (getTwitterSession() == null) {

            //if user is not authenticated start authenticating
            twitterLoginButton.setCallback(new Callback<TwitterSession>() {
                @Override
                public void success(Result<TwitterSession> result) {
                    loginButton.setVisibility(View.INVISIBLE);
                    twitterLoginButton.setVisibility(View.INVISIBLE);
                    twitter_logout_button.setVisibility((View.VISIBLE));
                    profile_image_button.setVisibility(View.VISIBLE);
                    // Do something with result, which provides a TwitterSession for making API calls
                    TwitterSession twitterSession = result.data;

                    //call fetch email only when permission is granted
                    fetchTwitterEmail(twitterSession);

                }

                @Override
                public void failure(TwitterException exception) {
                    // Do something on failure
                    loginButton.setVisibility(View.VISIBLE);
                    Toast.makeText(MainActivity.this, "Failed to authenticate. Please try again.", Toast.LENGTH_SHORT).show();
                }
            });
        //} else {

            //if user is already authenticated direct call fetch twitter email api

            //Toast.makeText(this, "User already authenticated", Toast.LENGTH_SHORT).show();
            //fetchTwitterEmail(getTwitterSession());
        //}
    }



    /**
     * Before using this feature, ensure that “Request email addresses from users” is checked for your Twitter app.
     *
     * @param twitterSession user logged in twitter session
     */
    public void fetchTwitterEmail(final TwitterSession twitterSession) {
        client.requestEmail(twitterSession, new Callback<String>() {
            @Override
            public void success(Result<String> result) {
                //here it will give u only email and rest of other information u can get from TwitterSession
                userDetailsLabel.setText("User Id : " + twitterSession.getUserId() + "\nScreen Name : " + twitterSession.getUserName() + "\nEmail Id : " + result.data);
            }

            @Override
            public void failure(TwitterException exception) {
                Toast.makeText(MainActivity.this, "Failed to authenticate. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * call Verify Credentials API when Twitter Auth is successful else it will go in exception block
     * this metod will provide you User model which contain all user information
     *
     * @param view calling view
     */
    public void fetchTwitterImage(View view) {
        //check if user is already authenticated or not
        if (getTwitterSession() != null) {

            //fetch twitter image with other information if user is already authenticated

            //initialize twitter api client
            TwitterApiClient twitterApiClient = TwitterCore.getInstance().getApiClient();

            //Link for Help : https://developer.twitter.com/en/docs/accounts-and-users/manage-account-settings/api-reference/get-account-verify_credentials

            //pass includeEmail : true if you want to fetch Email as well
            Call<User> call = twitterApiClient.getAccountService().verifyCredentials(true, false, true);
            call.enqueue(new Callback<User>() {
                @Override
                public void success(Result<User> result) {
                    User user = result.data;
                    userDetailsLabel.setText("User Id : " + user.id + "\nUser Name : " + user.name + "\nEmail Id : " + user.email + "\nScreen Name : " + user.screenName);

                    String imageProfileUrl = user.profileImageUrl;
                    Log.e(TAG, "Data : " + imageProfileUrl);
                    //NOTE : User profile provided by twitter is very small in size i.e 48*48
                    //Link : https://developer.twitter.com/en/docs/accounts-and-users/user-profile-images-and-banners
                    //so if you want to get bigger size image then do the following:
                    imageProfileUrl = imageProfileUrl.replace("_normal", "");

                    ///load image using Picasso
                    Picasso.with(MainActivity.this)
                            .load(imageProfileUrl)
                            .placeholder(R.mipmap.ic_launcher_round)
                            .into(userProfileImageView);
                }

                @Override
                public void failure(TwitterException exception) {
                    Toast.makeText(MainActivity.this, "Failed to authenticate. Please try again.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            //if user is not authenticated first ask user to do authentication
            Toast.makeText(this, "First to Twitter auth to Verify Credentials.", Toast.LENGTH_SHORT).show();
        }

    }

    /**
     * get authenticates user session
     *
     * @return twitter session
     */
    private TwitterSession getTwitterSession() {
        TwitterSession session = TwitterCore.getInstance().getSessionManager().getActiveSession();

        //NOTE : if you want to get token and secret too use uncomment the below code
        /*TwitterAuthToken authToken = session.getAuthToken();
        String token = authToken.token;
        String secret = authToken.secret;*/

        return session;
    }
    public void twitterLogout(View view)
    {
        twitter_logout_button.setVisibility(View.INVISIBLE);
        profile_image_button.setVisibility(View.INVISIBLE);
        userDetailsLabel.setText("");
        userProfileImageView.setImageBitmap(null);
        loginButton.setVisibility(View.VISIBLE);
        twitterLoginButton.setVisibility(View.VISIBLE);
        Toast.makeText(this, "User Logged Out", Toast.LENGTH_SHORT).show();
        defaultLoginTwitter();


    }
}

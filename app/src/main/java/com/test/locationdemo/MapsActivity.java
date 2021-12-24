package com.test.locationdemo;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.GeomagneticField;
import android.location.Geocoder;
import android.location.Location;
import android.os.Handler;
import android.os.ResultReceiver;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends AppCompatActivity
        implements GoogleMap.OnMyLocationButtonClickListener,
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleMap.OnMarkerDragListener,
        GoogleApiClient.OnConnectionFailedListener,
        ActivityCompat.OnRequestPermissionsResultCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private boolean mPermissionDenied = false;

    private GoogleApiClient mGoogleApiClient;
    private GoogleMap mMap;
    private Location mLastLocation;
    private AddressResultReceiver mResultReceiver;
    /**
     * 用来判断用户在连接上Google Play services之前是否有请求地址的操作
     */
    private boolean mAddressRequested;
    /**
     * 地图上锚点
     */
    private Marker perth;
    private LatLng lastLatLng, perthLatLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        mResultReceiver = new AddressResultReceiver(new Handler());
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        Button btn=findViewById(R.id.btn);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (null!=mMap){
                    mMap.snapshot(new GoogleMap.SnapshotReadyCallback() {
                        @Override
                        public void onSnapshotReady(@Nullable Bitmap bitmap) {
                        }
                    });

                }
            }
        });

    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMarkerDragListener(this);
        enableMyLocation();
        mMap.addPolyline(new PolylineOptions().addAll(readLatLngs()).width(2)//设置线宽
                .geodesic(false) //是否是大地曲线
                .color(getResources().getColor(R.color.black)));

        BitmapDescriptor startDescriptor = BitmapDescriptorFactory.fromResource(R.mipmap.map_start);
        BitmapDescriptor ednDescriptor = BitmapDescriptorFactory.fromResource(R.mipmap.map_end);
        mMap.addMarker((new MarkerOptions()).position(readLatLngs().get(0)).icon(startDescriptor));
        mMap.addMarker((new MarkerOptions()).position(readLatLngs().get(readLatLngs().size() - 1)).icon(ednDescriptor));
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(readLatLngs().get(0).latitude, readLatLngs().get(0).longitude)).zoom(15).build();
        mMap.animateCamera(CameraUpdateFactory
                .newCameraPosition(cameraPosition));

    }

    private double[] coords = {37.85859603831572, 112.49881265978634,
            37.858621450453285, 112.49891994813964,
            37.85863658891602, 112.49923665178488,
            37.85894153382089, 112.49931175363218,
            37.8592041242558, 112.49933321130284,
            37.859576831653975, 112.49940831315014,
            37.859932596048694, 112.49947268616212,
            37.86029682924524, 112.4995370591741,
            37.86064411968655, 112.49960143218608,
            37.860915174503745, 112.49964434752741,
            37.86101681980319, 112.49981600889267,
            37.86096599717097, 112.50024516230584,
            37.86088976315696, 112.50071723106035,
            37.86077117675623, 112.501779385758,
            37.86065259016469, 112.50302393065623,
            37.86055094436294, 112.5040968141892,
            37.86045776893343, 112.50500876573533,
            37.86026294717527, 112.50621039529226,
            37.86007659544648, 112.50727254998988,
            37.85987330211409, 112.50863511207672,
            37.859771655237616, 112.5093861305498,
            37.85966153762991, 112.51046974291809,
            37.85959377286647, 112.51104910002589,
            37.85952600804074, 112.51152116878039,
            37.859373536955054, 112.51160699946304,
            37.85905165251616, 112.51158554179236};

    private List<LatLng> readLatLngs() {
        List<LatLng> points = new ArrayList<>();
        for (int i = 0; i < coords.length; i += 2) {
//            LatLng latLng = TransBaiduGaodePoint.baidu_to_gaode(new LatLng(coords[i], coords[i+1]));
            points.add(new LatLng(coords[i], coords[i+1]));
        }
        return points;
    }
    /**
     * 检查是否已经连接到 Google Play services
     */
    private void checkIsGooglePlayConn() {
        if (mGoogleApiClient.isConnected() && mLastLocation != null) {
            startIntentService(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()));
        }
        mAddressRequested = true;
    }

    /**
     * 如果取得了权限,显示地图定位层
     */
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else if (mMap != null) {
            mMap.setMyLocationEnabled(true);
        }
    }


    /**
     * '我的位置'按钮点击时的调用
     *
     * @return
     */
    @Override
    public boolean onMyLocationButtonClick() {
        if (lastLatLng != null)
            perth.setPosition(lastLatLng);
        checkIsGooglePlayConn();
        return false;
    }


    /**
     * 启动地址搜索Service
     */
    protected void startIntentService(LatLng latLng) {
        Intent intent = new Intent(this, FetchAddressIntentService.class);
        intent.putExtra(FetchAddressIntentService.RECEIVER, mResultReceiver);
        intent.putExtra(FetchAddressIntentService.LATLNG_DATA_EXTRA, latLng);
        startService(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            Toast.makeText(getApplicationContext(), "Permission to access the location is missing.", Toast.LENGTH_LONG).show();
            return;
        }
        if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            enableMyLocation();
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (mPermissionDenied) {
            showMissingPermissionError();
            mPermissionDenied = false;
        }
    }

    private void showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog
                .newInstance(true).show(getSupportFragmentManager(), "dialog");
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(), "Permission to access the location is missing.", Toast.LENGTH_LONG).show();
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            lastLatLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            displayPerth(true, lastLatLng);
            initCamera(lastLatLng);
            if (!Geocoder.isPresent()) {
                Toast.makeText(this, "No geocoder available", Toast.LENGTH_LONG).show();
                return;
            }
            if (mAddressRequested) {
                startIntentService(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()));
            }
        }
    }

    /**
     * 添加标记
     */
    private void displayPerth(boolean isDraggable, LatLng latLng) {
        if (perth == null) {
            perth = mMap.addMarker(new MarkerOptions().position(latLng).title("Your Position"));
            perth.setDraggable(isDraggable); //设置可移动
        }

    }


    /**
     * 将地图视角切换到定位的位置
     */
    private void initCamera(final LatLng sydney) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(800);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(sydney, 14));
                    }
                });
            }
        }).start();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onMarkerDragStart(Marker marker) {
    }

    @Override
    public void onMarkerDrag(Marker marker) {
    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        perthLatLng = marker.getPosition();
        startIntentService(perthLatLng);
    }


    class AddressResultReceiver extends ResultReceiver {
        private String mAddressOutput;

        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            mAddressOutput = resultData.getString(FetchAddressIntentService.RESULT_DATA_KEY);
            if (resultCode == FetchAddressIntentService.SUCCESS_RESULT) {
                new AlertDialog.Builder(MapsActivity.this)
                        .setTitle("Position")
                        .setMessage(mAddressOutput)
                        .create()
                        .show();
            }

        }
    }

}

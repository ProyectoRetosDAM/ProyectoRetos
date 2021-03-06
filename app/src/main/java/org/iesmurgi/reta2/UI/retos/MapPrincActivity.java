/*

Reta2  Copyright (C) 2018  Alberto Fernández Fernández, Santiago Álvarez Fernández, Joaquín Pérez Rodríguez

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program. If not, see http://www.gnu.org/licenses/.


Contact us:

fernandez.fernandez.angel@gmail.com
santiago.alvarez.dam@gmail.com
perezrodriguezjoaquin0@gmail.com

*/

package org.iesmurgi.reta2.UI.retos;

import android.Manifest;

import android.app.PendingIntent;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import android.support.v7.widget.Toolbar;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.iesmurgi.reta2.Chat.Chat;
import org.iesmurgi.reta2.Chat.ChatActivity;
import org.iesmurgi.reta2.Data.BasedeDatosApp;
import org.iesmurgi.reta2.Data.entidades.Retos;
import org.iesmurgi.reta2.R;
import org.iesmurgi.reta2.UI.admin.PruebaLoc;
import org.iesmurgi.reta2.papelera.GeofenceTransiciones;
import org.iesmurgi.reta2.UI.usuario.FinPartidaActivity;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Actividad que muestra el mapa de la partida al usuario
 * @author Alberto Fernández
 * @author Santiago Álvarez
 * @author Joaquín Pérez
 */
public class MapPrincActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {

    private static final int REQUEST_LOCATION_PERMISSION_CODE = 1;
    private static final String TAG = "GEOFENCE";

    int idUsuario;
    private boolean primeravez = true;
    public GoogleMap mapa;
    private ArrayList<MarkerOptions> marcadores = new ArrayList<>();
    FirebaseDatabase database;
    DatabaseReference myRef;
    private String usuario;
    static final int RETO_FINALIZADO = 1;
    private LocationListener mGpsListener;
    private LocationModel locationModel;
    private LocationManager gestorLoc;
    private GoogleApiClient myClient;
    float[] results = {100};
    private GeofencingRequest geofencingRequest;
    private boolean puedespinchar = false;
    private boolean isMonitoring = false;
    protected ArrayList<Geofence> mGeofenceList;
    private MarkerOptions markerOptions;
    private PendingIntent pendingIntent;
    private GeofencingClient mGeofencingClient;
    private CircleOptions co = new CircleOptions();
    private Marker destinoactual;
    private int idpartida;
    private final LatLng Murgi = new LatLng(36.7822801, -2.815255);
    private FirebaseAuth mAuth;

    @BindView(R.id.toolbar_mapaUsuario)
    Toolbar toolbar;


    public static final long GEOFENCE_EXPIRATION_IN_HOURS = 1;

    public static final long GEOFENCE_EXPIRATION_IN_MILLISECONDS =
            GEOFENCE_EXPIRATION_IN_HOURS * 60 * 60 * 1000;



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            bindLocationListener();
        } else {
            Toast.makeText(this, "This sample requires Location access", Toast.LENGTH_LONG).show();
        }
    }


    /**
     * Método que instancia el escuchador de la localización.
     */
    private void bindLocationListener() {
        BoundLocationManager.bindLocationListenerIn(this, mGpsListener, getApplicationContext());
    }

    /**
     * Clase que hereda de AsynTask para poder hacer una consulta al room en un hilo secundario y obtener los retos de la partida
     */
    class RecuperarRetos extends AsyncTask<Integer, Void, Integer> {
        @Override
        protected Integer doInBackground(Integer... p) {
            retos = BasedeDatosApp.getAppDatabase(getApplicationContext()).retosDao().getRetosPartida(p[0]);

            locationModel.setRetos(retos);
            locationModel.setCargados(true);

            return 0;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
            initMapa();
        }
    }


    private List<Retos> retos = new ArrayList<>();


    private String nombrepartida;

    int aux;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_princ);
        locationModel = ViewModelProviders.of(this).get(LocationModel.class);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);

        if (locationModel.getNumReto() == null) {
            locationModel.setNumReto(getIntent().getExtras().getInt("ultimoReto"));
        }


        if (locationModel.getIdpartida() == null) {
            idpartida = getIntent().getExtras().getInt("IDPARTIDA");
            Log.e("idpartidamodel", "" + idpartida);
            locationModel.setIdpartida(idpartida);
            Log.e("idpartidamodeldentro", "" + locationModel.getIdpartida());
        } else {
            idpartida = locationModel.getIdpartida();
        }


        if (locationModel.getIdusuario() == null) {
            idUsuario = getIntent().getExtras().getInt("idUsuario");
            Log.e("idususmodel", "" + idUsuario);
            locationModel.setIdusuario(idUsuario);
            Log.e("idusudentro", "" + locationModel.getIdusuario());
        } else {
            idUsuario = locationModel.getIdusuario();
        }


        if (locationModel.getNombrepartida() == null) {
            nombrepartida = getIntent().getExtras().getString("NOMBREPARTIDA");
            Log.e("nombreparidamodel", "" + nombrepartida);
            locationModel.setNombrepartida(nombrepartida);
        } else {
            nombrepartida = locationModel.getNombrepartida();
        }


        Log.e("nombrepartida mapa", nombrepartida);


        FloatingActionButton mChat = findViewById(R.id.btn_mapa_chat);
        mChat.setOnClickListener(v -> {
            Log.e("chat", "deberia abrir el chat" + nombrepartida);
            startActivity(new Intent(getApplicationContext(), ChatActivity.class)
                    .putExtra("SALA", nombrepartida)
            );
        });


        locationModel = ViewModelProviders.of(this).get(LocationModel.class);


        mGpsListener = new MyLocationListener(locationModel);
        mGeofenceList = new ArrayList<Geofence>();


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_LOCATION_PERMISSION_CODE);
        } else {
            bindLocationListener();
        }


        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.mapa);
        mapFragment.getMapAsync(this);

        mGeofencingClient = LocationServices.getGeofencingClient(this);

        buildGoogleApiClient();
        if (myClient == null) {

            myClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .addApi(Places.GEO_DATA_API)
                    .addApi(Places.PLACE_DETECTION_API)
                    .enableAutoManage(this, this)

                    .build();
        }

        crearmarcadores();


        database = FirebaseDatabase.getInstance();

        mAuth = FirebaseAuth.getInstance();
        usuario = mAuth.getCurrentUser().getDisplayName();
        if (usuario == null) {
            usuario = locationModel.getUsuario();
        } else {
            locationModel.setUsuario(usuario);
        }

        if (nombrepartida == null) {
            nombrepartida = locationModel.getPartida();

        } else {
            locationModel.setPartida(nombrepartida);
        }

        myRef = database.getReference("chat").child(nombrepartida).child(usuario);

        myRef.push().setValue(new Chat(""));

        myRef = database.getReference("Localizaciones").child(nombrepartida).child(usuario);


        locationModel.getmLocation().observe(this, location -> {
            if (location != null && mapa != null) {

                puedespinchar = false;
                Log.e("no puedes pinchar", "no puedes pinchar" + puedespinchar);
                Log.e("he entrado", "he entrado" + location.getLatitude() + " " + location.getLongitude());

                PruebaLoc lat = new PruebaLoc(String.valueOf(location.getLatitude()), String.valueOf(location.getLongitude()));

                myRef.push().setValue(lat);


                for (int i = 0; i < retos.size(); i++) {
                    Location.distanceBetween(location.getLatitude(), location.getLongitude(), retos.get(i).getLocalizacionLatitud(), retos.get(i).getLocalizacionLongitud(), results);
                    if (results[0] < 30) {
                        if (i == locationModel.getNumReto()) {
                            puedespinchar = true;

                            Log.e("puedes pinchar", "puedes pinchar" + puedespinchar);

                        }
                    }
                }

            }

        });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.mapa_opciones, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.op_saltar_reto:
                cargarDialogoSaltar();
                return true;


            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    /**
     * Método que permite saltar el reto actual
     */
    private void cargarDialogoSaltar() {

        String option = getResources().getString(R.string.choose_option);

        final AlertDialog.Builder alertOpciones = new AlertDialog.Builder(MapPrincActivity.this);
        alertOpciones.setTitle("" + option);
        alertOpciones.setMessage("Estas seguro que deseas saltar el reto?");
        alertOpciones.setPositiveButton("Si", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                cambiarReto();
                updateNumReto("Reto saltado");
                Log.e("Santi", "Estoy en si");
            }
        });

        alertOpciones.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                dialogInterface.dismiss();
            }
        });
        alertOpciones.show();

    }

    /**
     * Método que lanza una tarea para actualizar el reto y la puntuacion en la base de datos
     * @param mensaje mensaje que se manda en el toast
     */
    void updateNumReto(String mensaje) {

        Constraints myConstraints = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED)
                // Many other constraints are available, see the
                // Constraints.Builder reference
                .build();
        Data.Builder builder = new Data.Builder();
        builder.putString("idUsuario", String.valueOf(idUsuario));
        builder.putString("idPartida", String.valueOf(idpartida));
        builder.putString("ultimoReto",String.valueOf(locationModel.getNumReto()));
        OneTimeWorkRequest updatenumretos = new OneTimeWorkRequest.Builder(RetoUpdateNumRetoWorker.class).setInputData(builder.build()).setConstraints(myConstraints).build();
        WorkManager.getInstance().enqueue(updatenumretos);



       /* final String URL = "http://geogame.ml/api/update_numPartida.php";
        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {


                if (response.contains("success")) {
                    //progressDialog.dismiss();
                    Toast.makeText(getApplicationContext(), mensaje, Toast.LENGTH_LONG).show();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(), "Error al conectar con el servidor", Toast.LENGTH_LONG).show();
            }

        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("idUsuario", "" + idUsuario);
                params.put("idPartida", "" + idpartida);
                params.put("ultimoReto", "" + locationModel.getNumReto());
                return params;
            }
        };
        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        requestQueue.add(stringRequest);
        */
    }

    private void buildGoogleApiClient() {
    }

    //---------------------------------------------------geofences-----------------------------------------------------------------------------
    @Override
    protected void onStart() {
        myClient.connect();
        //startGeofences();
        super.onStart();


    }

    @Override
    protected void onStop() {
        myClient.disconnect();
        super.onStop();

    }

    private static final String NOTIFICATION_MSG = "NOTIFICATION MSG";
    // Create a Intent send by the notification

    public static Intent makeNotificationIntent(Context context, String msg) {
        Intent intent = new Intent(context, MapPrincActivity.class);
        intent.putExtra(NOTIFICATION_MSG, msg);
        return intent;
    }


    // Start Geofence creation process
    private void startGeofences() {
        Log.i(TAG, "startGeofence()");
        for (int i = 0; i < retos.size(); i++) {
            Geofence geofence = createGeofence(new LatLng(retos.get(i).getLocalizacionLatitud(), retos.get(i).getLocalizacionLongitud()), 150, retos.get(i).getNombre());
            GeofencingRequest geofenceRequest = createGeofenceRequest(geofence);
            addGeofence(geofenceRequest);
        }

    }

    private static final long GEO_DURATION = 60 * 60 * 1000;

    // Create a Geofence
    private Geofence createGeofence(LatLng latLng, float radius, String id) {
        Log.d(TAG, "createGeofence");
        return new Geofence.Builder()
                .setRequestId(id)
                .setCircularRegion(latLng.latitude, latLng.longitude, radius)
                .setExpirationDuration(GEO_DURATION)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER
                        | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();
    }


    // Create a Geofence Request
    private GeofencingRequest createGeofenceRequest(Geofence geofence) {
        Log.d(TAG, "createGeofenceRequest");
        return new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build();
    }

    private PendingIntent geoFencePendingIntent;
    private final int GEOFENCE_REQ_CODE = 0;

    private PendingIntent createGeofencePendingIntent() {
        Log.d(TAG, "createGeofencePendingIntent");
        if (geoFencePendingIntent != null)
            return geoFencePendingIntent;

        Intent intent = new Intent(this, GeofenceTransiciones.class);
        return PendingIntent.getService(
                this, GEOFENCE_REQ_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }


    // Add the created GeofenceRequest to the device's monitoring list
    private void addGeofence(GeofencingRequest request) {
        Log.d(TAG, "addGeofence");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.GeofencingApi.addGeofences(
                myClient,
                request,
                createGeofencePendingIntent()
        ).setResultCallback(this);
    }

    @Override
    public void onResult(@NonNull Status status) {

        Log.e(TAG, "onResult: " + status);
        if (status.isSuccess()) {
            Toast.makeText(this, "yeeah", Toast.LENGTH_SHORT).show();

        } else {
            // inform about fail
        }


    }

    // Clear Geofence
    private void clearGeofence() {
        Log.d(TAG, "clearGeofence()");
        LocationServices.GeofencingApi.removeGeofences(
                myClient,
                createGeofencePendingIntent()
        ).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
                if (status.isSuccess()) {
                    // remove drawing
                    Log.e("removidas", "removidasGeofences");
                }
            }
        });
    }


//-----------------------------------------------------------------------------------------------------------------------------------------------


    //-----------------------------------------------------------mapa----------------------------------------------------------------------------


    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        if (mapa == null) {
            mapa = googleMap;

        }


        if (!locationModel.isCargados()) {
            new RecuperarRetos().execute(idpartida);
        } else {
            retos = locationModel.getRetos();
            initMapa();
        }


    }

    /**
     * Método que inica el mapa con los marcadores de los retos
     */
    void initMapa() {

        Log.e("Numero reto", locationModel.getNumReto() + "" + retos.size());
        if (locationModel.getNumReto() == retos.size()) {
            Log.d("ENTRE", "SI");
            startActivity(new Intent(getApplicationContext(), FinPartidaActivity.class).putExtra("idPartida", idpartida).putExtra("idUsuario", idUsuario));
            finish();
        } else {

            if (marcadores == null || marcadores.isEmpty()) {
                Log.e("if marcadores", "if marcadores");
                crearmarcadores();
            }
            mapa.addMarker(marcadores.get(locationModel.getNumReto()));
            if (locationModel.getNumReto() != null && !marcadores.isEmpty()) {
                mapa.moveCamera(CameraUpdateFactory.newLatLng(marcadores.get(locationModel.getNumReto()).getPosition()));
                mapa.animateCamera(CameraUpdateFactory.newLatLngZoom(marcadores.get(locationModel.getNumReto()).getPosition(), 17));
                primeravez = false;
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mapa.setMyLocationEnabled(true);
        mapa.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        mapa.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if (puedespinchar) {
                    for (int i = 0; i < marcadores.size(); i++) {

                        if (marcadores.get(i).getTitle().equalsIgnoreCase(marker.getTitle())) {
                            int[] aux;
                            aux = new int[]{idpartida, retos.get(i).getIdReto()};
                            startActivityForResult(
                                    new Intent(getApplicationContext(), RetoActivity.class).putExtra("idUsuario", idUsuario).putExtra("PARTIDAYRETO", aux).putExtra("NOMBREPARTIDA", nombrepartida).putExtra("numeroRetoArray", i + 1), RETO_FINALIZADO);
                        }
                    }
                }
                return false;
            }
        });
    }

    /**
     * Método que crea los marcadores de los retos del mapa
     */
    private void crearmarcadores() {

        for (int i = 0; i < retos.size(); i++) {
            marcadores.add(new MarkerOptions().position(new LatLng(retos.get(i).getLocalizacionLatitud(), retos.get(i).getLocalizacionLongitud()))
                    .title(retos.get(i).getNombre())

                    .icon(BitmapDescriptorFactory
                            .fromResource(R.drawable.ic_launcher))
                    .anchor(0.5f, 0.5f));

        }


        Log.e("Marcodores y retos", "Marcadores" + marcadores.size() + "Retos" + retos.size());


    }

    /**
     * Método que cambia el reto actual al siguiente, si el reto actual era el ultimo lanza la actividad finpartida
     */
    private void cambiarReto() {
        locationModel.setNumReto(locationModel.getNumReto() + 1);
        if (locationModel.getNumReto() < retos.size()) {
            try {
                mapa.clear();
                onMapReady(mapa);

            } catch (NullPointerException ex) {

            }


        } else {

            // finalizar juego
            startActivity(new Intent(getApplicationContext(), FinPartidaActivity.class).putExtra("idPartida", idpartida).putExtra("idUsuario", idUsuario));
            Log.e("FINISH", "FINISH");
            finish();

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RETO_FINALIZADO) {

            if (resultCode == RESULT_OK) {
                cambiarReto();

            }


        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}

/**
 * Clase que se usa para el observador de la localizacion
 */
class MyLocationListener implements LocationListener {

    private LocationModel model;

    /**
     * Constructor
     * @param locationModel viewmodel de la localizacion
     */
    public MyLocationListener(LocationModel locationModel) {
        model = locationModel;
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("onLocationChanged", "Changed 1");
        if (location != null && model.getmLocation() != null) {
            model.setmLocation(location);
        }
        Log.d("onLocationChanged", "Changed 2");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}




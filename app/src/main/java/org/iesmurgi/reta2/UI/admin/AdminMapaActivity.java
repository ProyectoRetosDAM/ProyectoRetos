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

package org.iesmurgi.reta2.UI.admin;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.iesmurgi.reta2.Chat.ChatAdminActivity;
import org.iesmurgi.reta2.R;
import org.iesmurgi.reta2.Seguridad.Cifrar;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.OnClick;
/**
 * Actividad del mapa que se muestra al administrador con todos los retos y participantes.
 * @author Alberto Fernández
 * @author Santiago Álvarez
 * @author Joaquín Pérez
 */
public class AdminMapaActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final int REQUEST_LOCATION_PERMISSION_CODE = 1;
    private GoogleMap mMap;
    private String partida;
    private int idPartida;
   // private final LatLng Murgi = new LatLng(36.7822801, -2.815255);
    FirebaseDatabase database;
    DatabaseReference myRef, mRefAux;
    private ArrayList<MarkerOptions> marcadores = new ArrayList<>();
    private static ArrayList<MarkerOptions> marcadoresRetos = new ArrayList<>();

    private ArrayList<String> participantes = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_mapa);
        ButterKnife.bind(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        partida = getIntent().getExtras().getString("PARTIDA");

        idPartida = getIntent().getIntExtra("idPartida",0);



        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_LOCATION_PERMISSION_CODE);
        }

        escucharUsuarios();



    }

    /**
     * Método que lanza el chat del administrador
     */
    @OnClick(R.id.btn_mapa_admin_chat)
    void abrirChat() {
        startActivity(new Intent(getApplicationContext(), ChatAdminActivity.class).putExtra("SALA", partida));
    }

    /**
     * Método que añade los marcadores al mapa
     */
    void pintarMapa() {

        mMap.clear();
        for (int i = 0; i < marcadoresRetos.size(); i++) {
            mMap.addMarker(marcadoresRetos.get(i));
        }
        for (int i = 0; i < marcadores.size(); i++) {
            mMap.addMarker(marcadores.get(i));
        }


    }

    /**
     * Método que borra el marcador de un participante
     * @param participante nombre del equipo participante
     */
    void comprobarMarcador(String participante) {

        for (int i = 0; i < marcadores.size(); i++) {

            if (marcadores.get(i).getTitle().equalsIgnoreCase(participante)) {

                marcadores.remove(i);

            }

        }

    }

    /**
     * Método que actualiza la posicion del marcador de un participante en el mapa
     * @param participante nombre del equipo participante
     */
    void actualizarPosiciones(String participante) {


        mRefAux = database.getReference("Localizaciones").child(partida).child(participante);
        // Log.e("participan",participantes.get(i));

        mRefAux.orderByKey().limitToLast(1).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot != null) {
                    try {
                        Log.e("data", dataSnapshot.toString());
                        PruebaLoc loc = dataSnapshot.getValue(PruebaLoc.class);

                        comprobarMarcador(participante);


                        marcadores.add(new MarkerOptions().icon(BitmapDescriptorFactory
                                .fromResource(R.drawable.runningpeque)).anchor(0.5f, 0.5f).title(participante).position(new LatLng(Double.parseDouble(loc.getLatitud()), Double.parseDouble(loc.getLongitud()))));

                        pintarMapa();
                    } catch (Exception ex) {
                        Log.e("error", ex.getMessage());
                    }
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


    }

    /**
     * Método que escucha los cambios de las localizaciones de los equipos
     */
    void escucharUsuarios() {

        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("Localizaciones").child(partida);

        myRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot != null) {
                    try {

                        String participante = dataSnapshot.getKey();
                        Log.e("participante", participante);
                        // participantes.add(participante);
                        //marcadores.add(new MarkerOptions().position(Murgi).title(participante));
                        // Log.e("size",""+participantes.size());
                        actualizarPosiciones(participante);
                    } catch (Exception ex) {
                        Log.e("ERROR", ex.getMessage());
                    }

                }

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (mMap == null) {
            mMap = googleMap;
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        }



        cargarRetos();

    }


    /**
     * Método que carga los retos de la partida de la base de datos
     */
    private void cargarRetos() {
        marcadoresRetos.clear();
        final String URL2 = "http://geogame.ml/api/Lista_Retos_idPartida.php?idPartida="+idPartida;
        StringRequest request2 = new StringRequest(Request.Method.POST, URL2, new Response.Listener<String>() {
            @Override
            public void onResponse(String response2) {

                    try {
                        JSONArray response = new JSONArray(Cifrar.decrypt(response2));

                        for (int i = 0; i < response.length(); i++) {
                        JSONObject o = response.getJSONObject(i);

                        marcadoresRetos.add( new MarkerOptions().position(
                                new LatLng(
                                        o.getDouble("localizacionLatitud"),
                                        o.getDouble("localizacionLongitud"))
                        )
                                .title(o.getString("nombre"))
                                .icon(BitmapDescriptorFactory
                                        .fromResource(R.drawable.ic_launcher))
                                .anchor(0.5f, 0.5f));


                }//endgfor
                if(!marcadoresRetos.isEmpty()) {
                    try {
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(marcadoresRetos.get(0).getPosition()));
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marcadoresRetos.get(0).getPosition(), 17));
                        pintarMapa();
                    }catch (Exception ex){}
                }


                } catch (Exception e) {
                        Toast.makeText(getApplicationContext(), "Error al conectar con el servidor", Toast.LENGTH_LONG).show();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(), "Error al conectar con el servidor", Toast.LENGTH_LONG).show();
            }
        });
        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        requestQueue.add(request2);

    }


}

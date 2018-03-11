package geogame.proyectoretos.UI;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import geogame.proyectoretos.Data.BasedeDatosApp;
import geogame.proyectoretos.Data.DAOS.RespuestasDao;
import geogame.proyectoretos.Data.DAOS.RetosDao;
import geogame.proyectoretos.Data.entidades.Respuestas;
import geogame.proyectoretos.Data.entidades.Retos;
import geogame.proyectoretos.R;

public class RetoActivity extends AppCompatActivity {

    @BindView(R.id.rb_reto_opcion1)
    RadioButton rbRetoOpcion1;
    @BindView(R.id.rb_reto_opcion2)
    RadioButton rbRetoOpcion2;
    @BindView(R.id.rb_reto_opcion3)
    RadioButton rbRetoOpcion3;
    @BindView(R.id.rg_reto_rgrupo)
    RadioGroup rgRetoRgrupo;
    @BindView(R.id.txt_reto_descripcion)
    TextView txtRetoDescripcion;
    @BindView(R.id.btn_reto_responder)
    Button btnRetoResponder;
    @BindView(R.id.btn_reto_cancelar)
    Button btnRetoCancelar;
    @BindView(R.id.txt_reto_crono)
    TextView txtRetoCrono;
    @BindView(R.id.txt_reto_nombre)
    TextView txtRetoNombre;

    private List<Respuestas> misRespuestas = new ArrayList<>();
    private List<Respuestas> misResBorrar = new ArrayList<>();
    private Retos miReto;
    private String resElegida;


    private LoginModel cronoViewModel;
    private long nTiempo;

    private RetosDao retosDao;
    private RespuestasDao respuestasDao;
    private int idpartida;
    private int idreto;
    private List<Respuestas> respuestas = new ArrayList<>();
    private boolean salida = false;
    BasedeDatosApp db;

    int[] aux;


    class recRes extends AsyncTask<Integer, Void, Void> {
        @Override
        protected Void doInBackground(Integer... p) {


            respuestas = db.respuestasDao().getRespuestas(p[0]);
            Log.e("respuestas", "" + respuestas.size());

            return null;


        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            init();
        }


    }

    class RecReto extends AsyncTask<Integer, Void, Void> {
        @Override
        protected Void doInBackground(Integer... p) {


            miReto = db.retosDao().getReto_Partida(p[0], p[1]);
            Log.e("retos", "" + miReto.getNombre());

            return null;

        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            new recRes().execute(aux[1]);

        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reto);
        ButterKnife.bind(this);


        db = BasedeDatosApp.getAppDatabase(this);


        aux = getIntent().getExtras().getIntArray("PARTIDAYRETO");
        Log.e("idpartida", "" + aux[0]);
        Log.e("idreto", "" + aux[1]);

        //rellenado de los radioBtn aleatoriamente

        new RecReto().execute(aux[0], aux[1]);


    }


    void init() {





        misResBorrar.clear();
        misRespuestas.clear();
        rellenarArray();
        Log.i("COMPROBAR misResBorrar:", misResBorrar.size() + "");
        Log.i("COMPROBAR misRespuesta:", misRespuestas.size() + "");


        int selec = elegirRandom(0, misResBorrar.size() - 1);
        rbRetoOpcion1.setText(misResBorrar.get(selec).getDescripcion());
        misResBorrar.remove(selec);
        int selec2 = elegirRandom(0, misResBorrar.size() - 1);
        rbRetoOpcion2.setText(misResBorrar.get(selec2).getDescripcion());
        misResBorrar.remove(selec2);
        int selec3 = 0;
        rbRetoOpcion3.setText(misResBorrar.get(selec3).getDescripcion());
        misResBorrar.remove(selec3);


        txtRetoNombre.setText(miReto.getNombre());
        txtRetoDescripcion.setText(miReto.getDescripcion());


        //este bloque es el control de la cuenta atras
        cronoViewModel = ViewModelProviders.of(this).get(LoginModel.class);

        Log.v("mitiempo", "" + cronoViewModel.getMiTiempo());

        long timeRec = miReto.getMaxDuracion() * 1000;
        if (cronoViewModel.getMiTiempo() == null) {
            cronoViewModel.setMiTiempo(timeRec);
            new CountDownTimer(timeRec, 1000) {

                public void onTick(long tiempo) {

                    txtRetoCrono.setText("Tiempo: " + tiempo / 1000);
                    cronoViewModel.setMiTiempo(tiempo);
                }

                public void onFinish() {
                    txtRetoCrono.setText("No puntuas!");
                    cronoViewModel.setMiTiempo((long) 0);
                    if (!salida) {
                        salida = true;
                        setResult(MapPrincActivity.RESULT_OK, new Intent(getApplicationContext(), MapPrincActivity.class));
                    }
                }
            }.start();

        } else {

            final long ntiempo = cronoViewModel.getMiTiempo();
            new CountDownTimer(ntiempo, 1000) {

                public void onTick(long ntiempo) {
                    txtRetoCrono.setText("Tiempo: " + ntiempo / 1000);
                    cronoViewModel.setMiTiempo(ntiempo);
                }

                public void onFinish() {
                    txtRetoCrono.setText("No puntuas!");
                    cronoViewModel.setMiTiempo((long) 0);
                    setResult(MapPrincActivity.RESULT_OK, new Intent(getApplicationContext(), MapPrincActivity.class));
                    if (!salida) {
                        salida = true;
                        setResult(MapPrincActivity.RESULT_OK, new Intent(getApplicationContext(), MapPrincActivity.class));
                    }
                }
            }.start();

        }

        Log.v("mitiempo", String.valueOf(cronoViewModel.getMiTiempo()));


        //cuando cambia la seleccion setea la resElegida con el string del radiobuton
        rgRetoRgrupo.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int idBoton) {
                if (idBoton == R.id.rb_reto_opcion1) {

                    resElegida = rbRetoOpcion1.getText().toString();

                } else if (idBoton == R.id.rb_reto_opcion2) {

                    resElegida = rbRetoOpcion2.getText().toString();

                } else if (idBoton == R.id.rb_reto_opcion3) {

                    resElegida = rbRetoOpcion3.getText().toString();
                }
            }
        });

        //fin aqui
        btnRetoResponder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for (int i = 0; i < respuestas.size(); i++) {
                    if (respuestas.get(i).getDescripcion().equals(resElegida)) {

                        if (respuestas.get(i).getVerdadero() == 1) {

                            Toast.makeText(getApplicationContext(), "Has acertado!!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getApplicationContext(), "Has fallado!! no puntuas!!", Toast.LENGTH_SHORT).show();
                        }

                        setResult(MapPrincActivity.RESULT_OK, new Intent(getApplicationContext(), MapPrincActivity.class));
                    }
                }
            }
        });

        //fin aqui
        btnRetoCancelar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "No puntas este reto...", Toast.LENGTH_SHORT).show();
                setResult(MapPrincActivity.RESULT_OK, new Intent(getApplicationContext(), MapPrincActivity.class));
            }
        });


    }


    public void rellenarArray() {

        for (int i = 0; i < respuestas.size(); i++) {
            misRespuestas.add(respuestas.get(i));

            misResBorrar.add(respuestas.get(i));

        }

    }


    public int elegirRandom(int min, int max) {

        int rango = (max - min) + 1;
        return (int) (Math.random() * rango) + min;

    }
}

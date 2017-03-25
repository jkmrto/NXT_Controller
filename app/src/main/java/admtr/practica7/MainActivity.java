package admtr.practica7;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.InvalidClassException;

public class MainActivity extends AppCompatActivity {
    //Definimos variables de privilegios
    private final static int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    BluetoothAdapter mBluetoothAdapter;
    private static final String MAC_LEGO = "00:16:53:06:C3:99";
    MyBroadcastReceiver mybroadcastreceiver;
    IntentFilter filter;
    protected Button button_connect;
    protected TextView StatusText;
    protected TextView VelocitiesText;
    protected TextView PeriodicText;
    PowerController ControladorPotencia = null;
    BluetothConnectionController BTController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void onResume() {

        super.onResume();
        setContentView(R.layout.activity_main);

        verifyStoragePermissions(this);
        InicializarElementos();
        InicializarReferencias();
        mybroadcastreceiver = new MyBroadcastReceiver();
        InicializarIntents();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        StatusText.setText(StateLegend.Desconectado);
        BTController.ModifyStatus(BluetothConnectionController.EstadosPosibles.Desconectado);
    }


    public void onStop() {
        super.onStop();
        unregisterReceiver(mybroadcastreceiver);
        BTController.Disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void InicializarElementos() {
        ControladorPotencia = new PowerController();
        BTController = new BluetothConnectionController(this, MAC_LEGO, ControladorPotencia);
    }

    private void EnableBluetoth() {
        if (mBluetoothAdapter == null) {
        }else{
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    private void InicializarReferencias() {
        button_connect = (Button) findViewById(R.id.connect);
        StatusText = (TextView) findViewById(R.id.textView1);
        VelocitiesText = (TextView) findViewById(R.id.textView2);
        PeriodicText = (TextView) findViewById(R.id.textView3);
    }

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    public void InicializarIntents() {

        filter = new android.content.IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mybroadcastreceiver, filter);

        LocalBroadcastManager.getInstance(this).registerReceiver(mybroadcastreceiver,
                new IntentFilter(BluetothConnectionController.AccionesPosibles.ConexionRealizada));

        LocalBroadcastManager.getInstance(this).registerReceiver(mybroadcastreceiver,
                new IntentFilter(BluetothConnectionController.AccionesPosibles.DesconexionRealizada));

        LocalBroadcastManager.getInstance(this).registerReceiver(mybroadcastreceiver,
                new IntentFilter(BluetothConnectionController.AccionesPosibles.ProblemaConexion));

        LocalBroadcastManager.getInstance(this).registerReceiver(mybroadcastreceiver,
                new IntentFilter(BluetothConnectionController.AccionesPosibles.ProblemaDesconexion));

        LocalBroadcastManager.getInstance(this).registerReceiver(mybroadcastreceiver,
                new IntentFilter(BluetothConnectionController.AccionesPosibles.ComandosLanzados));

        LocalBroadcastManager.getInstance(this).registerReceiver(mybroadcastreceiver,
                new IntentFilter(BluetothConnectionController.AccionesPosibles.ComandosRecibidos));

        LocalBroadcastManager.getInstance(this).registerReceiver(mybroadcastreceiver,
                new IntentFilter(BluetothConnectionController.AccionesPosibles.Error));

        LocalBroadcastManager.getInstance(this).registerReceiver(mybroadcastreceiver,
                new IntentFilter(BluetothConnectionController.AccionesPosibles.PeriodicasRecibidas));

        LocalBroadcastManager.getInstance(this).registerReceiver(mybroadcastreceiver,
                new IntentFilter(BluetothConnectionController.AccionesPosibles.MedidasPedidas));

        LocalBroadcastManager.getInstance(this).registerReceiver(mybroadcastreceiver,
                new IntentFilter(BluetothConnectionController.AccionesPosibles.VelocidadModificada));

        LocalBroadcastManager.getInstance(this).registerReceiver(mybroadcastreceiver,
                new IntentFilter(BluetothConnectionController.AccionesPosibles.ComandoNoAceptado));
    }

    // Class for a BroadcastReceiver for managing Bluetooth ACTION_* events
    class MyBroadcastReceiver extends BroadcastReceiver {
        public void onReceive(android.content.Context context, android.content.Intent intent) {
            String action = intent.getAction();

            switch (action) {

                case BluetothConnectionController.AccionesPosibles.ConexionRealizada:
                    Log.d("BroadcastReceiver", "ConexionRealizada");
                    BTController.ModifyStatus(BluetothConnectionController.EstadosPosibles.Conectado);
                    button_connect.setText(ButtonLegend.Leyenda_Desconectar);
                    StatusText.setText(StateLegend.Conectado + BTController.mensaje);
                    break;

                case BluetothConnectionController.AccionesPosibles.DesconexionRealizada:
                    Log.d("BroadcastReceiver", "DesconexionRealizada");
                    BTController.ModifyStatus(BluetothConnectionController.EstadosPosibles.Desconectado);
                    button_connect.setText(ButtonLegend.Leyenda_Conectar);
                    StatusText.setText(StateLegend.Desconectado + BTController.mensaje);
                    break;

                case BluetothConnectionController.AccionesPosibles.ProblemaConexion:
                    Log.d("BroadcastReceiver", "ProblemaConexion");
                    BTController.ModifyStatus(BluetothConnectionController.EstadosPosibles.Desconectado);
                    button_connect.setText(ButtonLegend.Leyenda_Conectar);
                    StatusText.setText(StateLegend.Desconectado + " " + BTController.mensaje); //StateLegend.Desconectado
                    break;

                case BluetothConnectionController.AccionesPosibles.ProblemaDesconexion:
                    Log.d("BroadcastReceiver", "ProblemaDesconexion");
                    BTController.ModifyStatus(BluetothConnectionController.EstadosPosibles.Conectado);
                    button_connect.setText(ButtonLegend.Leyenda_Desconectar);
                    StatusText.setText(StateLegend.Desconectado + BTController.mensaje);
                    break;

                case BluetothConnectionController.AccionesPosibles.Error:
                    Log.e("BroadcastReceiver", "Error");
                    BTController.ModifyStatus(BluetothConnectionController.EstadosPosibles.Desconectado);
                    button_connect.setText(ButtonLegend.Leyenda_Conectar);
                    Log.e("Accion error", BTController.mensaje);
                    StatusText.setText(StateLegend.Desconectado + " Error: " + BTController.mensaje);
                    LimpiezaPorError();
                    break;

                case BluetothConnectionController.AccionesPosibles.ComandosLanzados:
                    Log.d("BroadcastReceiver", "ComandosLanzados");
                    BTController.ModifyStatus(BluetothConnectionController.EstadosPosibles.EsperandoRespuesta);
                    StatusText.setText(StateLegend.EsperandoRespuestaComandos);
                    break;
                case BluetothConnectionController.AccionesPosibles.ComandosRecibidos:
                    Log.d("BroadcastReceiver", "Comandos Recibidos");
                    BTController.ModifyStatus(BluetothConnectionController.EstadosPosibles.Conectado);
                    BTController.hebra_comandos = null;
                    StatusText.setText(StateLegend.Conectado);
                    break;
                case BluetothConnectionController.AccionesPosibles.MedidasPedidas:
                    Log.d("BroadcastReceiver", "MedidasPedidas");
                    BTController.ModifyStatus(BluetothConnectionController.EstadosPosibles.EsperandoRespuesta);
                    StatusText.setText(StateLegend.Conectado);
                    break;
                case BluetothConnectionController.AccionesPosibles.PeriodicasRecibidas:
                    Log.d("BroadcastReceiver", "PeriodiasRecibidas");
                    BTController.ModifyStatus(BluetothConnectionController.EstadosPosibles.Conectado);
                    PeriodicText.setText("Periodic: " + BTController.Periodic_mensaje_1 + ", " + BTController.Periodic_mensaje_2);
                    BTController.hebra_medidas = null;
                    break;
                case BluetothConnectionController.AccionesPosibles.VelocidadModificada:
                    VelocitiesText.setText("V: " + String.format("%.3f", BTController.ControladorPotencia.v) + ", "
                            + "W: " + String.format("%.3f", BTController.ControladorPotencia.w)) ;
                    break;
                case BluetothConnectionController.AccionesPosibles.ComandoNoAceptado:
                    StatusText.setText(StatusText.getText() + " Comando No aceptado, socket Bluetooth ocupado (Medidas en paralelo)");
                    break;
            }
        }
    }

    private class ButtonLegend {
        public static final String Leyenda_Desconectar = "DISCONNECT FROM LEGO";
        public static final String Leyenda_Conectar = "CONNECT TO LEGO";
    }

    private class StateLegend {
        public static final String Conectado = "Conectado";
        public static final String Desconectado = "Desconectado";
        public static final String EsperandoRespuestaComandos = "EsperandoRespuestaComandos";
    }

    public void Connect_Called(View view) {

        EnableBluetoth();

        if (mBluetoothAdapter.isEnabled()) {
            switch (BTController.getStatus()) {
                case BluetothConnectionController.EstadosPosibles.Desconectado:
                    BTController.Connect();
                    break;
                case BluetothConnectionController.EstadosPosibles.Conectado:
                    BTController.Disconnect();
                    break;
            }
        }
    }

    public void V_plus(View view) {
        switch (BTController.getStatus()) {
            case BluetothConnectionController.EstadosPosibles.Conectado:
                Log.d("V_plus", "Ha entrado en la función V_plus");
                Log.d("V_plus", "Aumentando la potencia");
                ControladorPotencia.incrementar_V();
                Log.d("V_plus", "Enviando los comandos");
                BTController.EnviarComando();
                break;
            default:
                LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(BluetothConnectionController.AccionesPosibles.ComandoNoAceptado));
                break;
        }
    }

    public void Stop_called(View view) {
        switch (BTController.getStatus()) {
            case BluetothConnectionController.EstadosPosibles.Conectado:
                Log.d("V_plus", "Ha entrado en la función Stop");
                Log.d("V_plus", "Parando");
                ControladorPotencia.parar();
                Log.d("V_plus", "Enviando los comandos");
                BTController.EnviarComando();
                break;
            default:
                LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(BluetothConnectionController.AccionesPosibles.ComandoNoAceptado));
                break;
        }
    }

    public void V_minus(View view) {
        switch (BTController.getStatus()) {
            case BluetothConnectionController.EstadosPosibles.Conectado:
                Log.d("V_plus", "Ha entrado en la función V_plus");
                Log.d("V_plus", "Aumentando la potencia");
                ControladorPotencia.decrementar_V();
                Log.d("V_plus", "Enviando los comandos");
                BTController.EnviarComando();
                break;
            default:
                LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(BluetothConnectionController.AccionesPosibles.ComandoNoAceptado));
                break;
        }
    }

    public void W_minus(View view) {

        switch (BTController.getStatus()) {
            case BluetothConnectionController.EstadosPosibles.Conectado:
                Log.d("V_plus", "Ha entrado en la función V_plus");
                Log.d("V_plus", "Aumentando la potencia");
                ControladorPotencia.decrementar_W();
                Log.d("V_plus", "Enviando los comandos");
                BTController.EnviarComando();
                break;
            default:
                LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(BluetothConnectionController.AccionesPosibles.ComandoNoAceptado));
                break;
        }
    }

    public void W_plus(View view) {

        switch (BTController.getStatus()) {
            case BluetothConnectionController.EstadosPosibles.Conectado:
                Log.d("V_plus", "Ha entrado en la función V_plus");
                Log.d("V_plus", "Aumentando la potencia");
                ControladorPotencia.incrementar_W();
                Log.d("V_plus", "Enviando los comandos");
                BTController.EnviarComando();
                break;
            default:
                LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(BluetothConnectionController.AccionesPosibles.ComandoNoAceptado));
                break;
        }
    }

    public void Recto(View view) {

        switch (BTController.getStatus()) {
            case BluetothConnectionController.EstadosPosibles.Conectado:
                ControladorPotencia.straight();
                BTController.EnviarComando();
                break;
            default:
                LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(BluetothConnectionController.AccionesPosibles.ComandoNoAceptado));
                break;
        }
    }

    public void Save_Called(View view) {
        BTController.save_Datos.Llamar_salvar();
    }

    private void LimpiezaPorError() {
        Log.d("LimpiezaError", "Desconectando y Matando hebra periodica");

        if (BTController.socket.isConnected()) {
            BTController.Disconnect();
        }
    }
}


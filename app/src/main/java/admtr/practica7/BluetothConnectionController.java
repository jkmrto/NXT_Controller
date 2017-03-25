package admtr.practica7;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by jkmrto on 14/01/17.
 */

public class BluetothConnectionController extends SharedSocket {
    private final ReentrantLock lock = new ReentrantLock();
    private String address = "";
    private Context ctx;
    protected PowerController ControladorPotencia;
    protected GrabarDatos save_Datos;
    private final static long timeout_respuesta_nano = 500000000;

    private int Status;
    public String mensaje = "";
    public String Periodic_mensaje_1 = "";
    public String Periodic_mensaje_2 = "";

    Thread hebra_medidas = null;
    Thread hebra_comandos = null;
    Thread hebra_periodica = null;

    public BluetothConnectionController(Context ctx, String address, PowerController ControladorPotencia) {
        super();
        this.address = address;
        this.ctx = ctx;
        this.ControladorPotencia = ControladorPotencia;
        save_Datos = new GrabarDatos();
    }

    public void Connect() {

        (new Thread(new Connect())).start();
    }


    public void Disconnect() {

        Thread desconectar_hebra = new Thread(new Desconectar());
        desconectar_hebra.start();
    }

    public void EnviarComando() {

        hebra_comandos = new Thread(new SendCommad());
        hebra_comandos.start();
    }

    public void ModifyStatus(int NewStatus) {
        Status = NewStatus;
    }

    public int getStatus() {
        return Status;
    }

    public static class EstadosPosibles {
        public static final int Desconectado = 1;
        public static final int Conectado = 2;
        public static final int EsperandoRespuesta = 3;
    }

    public static class AccionesPosibles {
        public static final String ConexionRealizada = "ConexionRealizada";
        public static final String ProblemaConexion = "ProblemaConexion";
        public static final String DesconexionRealizada = "DesconexionRealizada";
        public static final String ProblemaDesconexion = "ProblemaDesconexion";
        public static final String ComandosLanzados = "ComandosLanzados";
        public static final String ComandosRecibidos = "ComandosRecibidos";
        public static final String Error = "Error";
        public static final String MedidasPedidas = "MedidasPedidas";
        public static final String PeriodicasRecibidas = "PeriodicasRecibidas";
        public static final String VelocidadModificada = "VelocidadModificada";
        public static final String ComandoNoAceptado = "ComandoNoAceptado";

    }

    public class Connect implements Runnable {
        public void run() {
            long tiempo_inicial, tiempo_final;
            tiempo_inicial = System.nanoTime();
            create(BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address));
            String out = connect();

            //Log.d("Estado Socker", toString(socket.isConnected()));
            mensaje = out;
            Log.d("Conectando", mensaje);
            if ("".equals(out)) {
                LocalBroadcastManager.getInstance(ctx).sendBroadcast(new Intent(AccionesPosibles.ConexionRealizada));
                hebra_periodica = new Thread(new Periodic_heb());
                hebra_periodica.start();
                out = "";

                tiempo_final = System.nanoTime();
                save_Datos.tiempo_conexion[save_Datos.indice_tiempo_conexion] =
                        tiempo_final - tiempo_inicial;
                save_Datos.indice_tiempo_conexion++;

            } else {
                LocalBroadcastManager.getInstance(ctx).sendBroadcast(new Intent(AccionesPosibles.ProblemaConexion));
            }
        }
    }

    public class Desconectar implements Runnable {

        public void run() {

            String out = unconnect();

            if ("".equals(out)) {
                LocalBroadcastManager.getInstance(ctx).sendBroadcast(new Intent(AccionesPosibles.DesconexionRealizada));
            } else {
                LocalBroadcastManager.getInstance(ctx).sendBroadcast(new Intent(AccionesPosibles.ProblemaDesconexion));
            }
        }
    }

    public class SendCommad implements Runnable {

        public void run() {
            long tiempo_inicial, tiempo_final;
            tiempo_inicial = System.nanoTime();
            Command.SetOutputState c;
            String out;

            c = new Command.SetOutputState((byte) 0x00, ControladorPotencia.powerForWheel(false));

            lock.lock();

            try {
                out = sendCommand(c);

                if (out.equals("")) {
                    c = new Command.SetOutputState((byte) 0x02, ControladorPotencia.powerForWheel(true));
                    LocalBroadcastManager.getInstance(ctx).sendBroadcast(new Intent(AccionesPosibles.VelocidadModificada));

                    out = sendCommand(c);
                    if (out.equals("")) {
                        mensaje = "";
                        LocalBroadcastManager.getInstance(ctx).sendBroadcast(new Intent(AccionesPosibles.ComandosLanzados));

                        Response resp1 = receiveResponse(timeout_respuesta_nano);

                        if (resp1 instanceof Response.Error) {
                            Log.d("Respuesta", "Error en la respuesta");
                            LocalBroadcastManager.getInstance(ctx).sendBroadcast(new Intent(AccionesPosibles.Error));
                            mensaje = ((Response.Error) resp1).errortxt;

                        } else {
                            Log.d("Respuestas", "Repuesta 1 Recibida");
                            Response resp2 = receiveResponse(timeout_respuesta_nano);

                            if (resp2 instanceof Response.Error) {
                                Log.d("Respuesta", "Error en la respuesta");
                                LocalBroadcastManager.getInstance(ctx).sendBroadcast(new Intent(AccionesPosibles.Error));
                                mensaje = ((Response.Error) resp2).errortxt;
                            } else {
                                Log.d("Respuestas", "Repuesta 2 Recibida");
                                LocalBroadcastManager.getInstance(ctx).sendBroadcast(new Intent(AccionesPosibles.ComandosRecibidos));
                            }
                        }
                    } else {
                        LocalBroadcastManager.getInstance(ctx).sendBroadcast(new Intent(AccionesPosibles.Error));
                        mensaje = out;
                    }
                } else {
                    LocalBroadcastManager.getInstance(ctx).sendBroadcast(new Intent(AccionesPosibles.Error));
                    mensaje = out;
                }
            } finally {
                lock.unlock();
            }
            tiempo_final = System.nanoTime();
            save_Datos.tiempo_comandos[save_Datos.indice_tiempo_comandos] =
                    tiempo_final - tiempo_inicial;
            save_Datos.indice_tiempo_comandos++;
        }
    }

    public class Periodic_heb implements Runnable {
        public void run() {
            while (socket.isConnected()) {
                try {
                    Thread.sleep(500);
                    hebra_medidas = new Thread(new SendCommad_ReadData());
                    hebra_medidas.start();

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public class SendCommad_ReadData implements Runnable {

        public void run() {

            Command.GetOutputState c;
            String out;
            long tiempo_inicial, tiempo_final;
            Log.d("Log", "Entrando en hebra de solicitar medidas");

            if (socket.isConnected()) {
                tiempo_inicial = System.nanoTime();

                LocalBroadcastManager.getInstance(ctx).sendBroadcast(new Intent(AccionesPosibles.MedidasPedidas));
                c = new Command.GetOutputState((byte) 0x00);
                lock.lock();

                try {
                    out = sendCommand(c);
                    Log.d("Periodic", "Primer Comando Lectura Enviado");

                    if (out.equals("")) {
                        Response resp1 = receiveResponse(timeout_respuesta_nano);

                        if (resp1 instanceof Response.Error) {
                            Log.d("Periodic", "Error en la respuesta");
                            mensaje = ((Response.Error) resp1).errortxt;
                            LocalBroadcastManager.getInstance(ctx).sendBroadcast(new Intent(AccionesPosibles.Error));

                        } else {
                            c = new Command.GetOutputState((byte) 0x02);
                            out = sendCommand(c);

                            if (out.equals("")) {

                                Response resp2 = receiveResponse(timeout_respuesta_nano);
                                if (resp2 instanceof Response.Error) {
                                    Log.d("Periodic", "Error en la respuesta");
                                    mensaje = ((Response.Error) resp2).errortxt;
                                    LocalBroadcastManager.getInstance(ctx).sendBroadcast(new Intent(AccionesPosibles.Error));
                                } else {
                                    Log.d("Periodic", "Repuesta 2 Recibida");
                                    Periodic_mensaje_1 = Double.toString((double) ((Response.GetOutputState) resp1).power);
                                    Periodic_mensaje_2 = Double.toString((double) ((Response.GetOutputState) resp2).power);
                                    LocalBroadcastManager.getInstance(ctx).sendBroadcast(new Intent(AccionesPosibles.PeriodicasRecibidas));
                                }
                            } else {
                                mensaje = out;
                                LocalBroadcastManager.getInstance(ctx).sendBroadcast(new Intent(AccionesPosibles.Error));
                            }
                        }

                    } else {
                        mensaje = out;
                        LocalBroadcastManager.getInstance(ctx).sendBroadcast(new Intent(AccionesPosibles.Error));

                    }
                } finally {
                    lock.unlock();
                    tiempo_final = System.nanoTime();
                    save_Datos.tiempo_datos_periodico[save_Datos.indice_tiempo_datos_periodico] =
                            tiempo_final - tiempo_inicial;
                    save_Datos.indice_tiempo_datos_periodico++;
                }
            }
        }
    }
}

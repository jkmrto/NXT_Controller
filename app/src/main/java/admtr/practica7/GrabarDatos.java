package admtr.practica7;

import android.util.Log;
import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStreamWriter;

/**
 * Created by root on 23/01/17.
 */


public class GrabarDatos {

    static final int NUMEXPERIMENTS = 10000;

    public long[] tiempo_conexion = new long[NUMEXPERIMENTS];
    public long[] tiempo_datos_periodico = new long[NUMEXPERIMENTS];
    public long[] tiempo_comandos = new long[NUMEXPERIMENTS];

    public int indice_tiempo_conexion;
    public int indice_tiempo_datos_periodico;
    public int indice_tiempo_comandos;

    private String nombre_archivo_tiempo_conexion = "Datos_tiempo_conexion.txt";
    private String nombre_archivo_tiempo_datos_periodico = "Datos_tiempo_periodico.txt";
    private String nombre_archivo_tiempo_comandos = "Datos_tiempo_comandos.txt";

    public GrabarDatos(){
        indice_tiempo_comandos = 0;
        indice_tiempo_datos_periodico = 0;
        indice_tiempo_comandos = 0;
    }

    public void Llamar_salvar(){

        Thread hebra_guardar = new Thread(new SalvarDatos());
        hebra_guardar.start();

    }

    public class SalvarDatos implements Runnable {

        public void run() {

            // Accede al sistema de memoria externa
            String fileresult = "";
            String extstorstate = Environment.getExternalStorageState();
            boolean extstormounted = Environment.MEDIA_MOUNTED.equals(extstorstate); // true if mounted with r/w access

            if (extstormounted) fileresult = "mounted";
            else fileresult = "notmounted";
            if ((extstormounted) && (Environment.DIRECTORY_DOWNLOADS.length() > 0)) {// Si la memoria externa existe y el directorio Downloads es conocido por el sistema operativo
                imprirFichero(nombre_archivo_tiempo_conexion, tiempo_conexion, indice_tiempo_conexion);
                imprirFichero(nombre_archivo_tiempo_datos_periodico, tiempo_datos_periodico, indice_tiempo_datos_periodico);
                imprirFichero(nombre_archivo_tiempo_comandos, tiempo_comandos, indice_tiempo_comandos);
            }
            Log.i("testguardado", "testguardado ");
        }

    }

    static private void imprirFichero(String nombre_archivo, long[] array, int indice) {

        try {
            // Crea el fichero para grabar los datos en el directorio Downloads
            File dir = new java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "");
            if ((!dir.exists()) || (!dir.isDirectory())) dir.mkdirs();
            java.io.File fi = new java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), nombre_archivo);

            // Prepara un objeto de manejo de escrituras en fichero
            java.io.FileOutputStream fos = new java.io.FileOutputStream(fi);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

            // Aquí pon código para escribir los datos en el fichero; por ejemplo:
            for (int f = 0; f < indice; f++) {
                bw.write(String.valueOf((int) (array[f])));
                bw.newLine();
            }
            // Cierra el fichero al terminar de grabar datos
            bw.close();
            // fileresult += " ok";
            Log.e("Imprimir Fichero", "Creado  el fichero:" + nombre_archivo);
        } catch (Exception e) {
            Log.e("Imprimir Fichero", "Error generado el fichero:" + e.toString() + nombre_archivo);
            //fileresult = "error in file: " + e;
        }
    }
}

